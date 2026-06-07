import org.slf4j.bridge.SLF4JBridgeHandler
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.zaxxer.hikari.HikariConfig
import finance.domain.Account
import finance.domain.AuthenticatedUser
import finance.domain.BonusProgress
import finance.domain.LoginRequest
import finance.domain.User
import io.netty.handler.ssl.SslContextBuilder
import org.yaml.snakeyaml.Yaml
import finance.domain.Category
import finance.domain.Description
import finance.domain.FamilyMember
import finance.domain.MedicalExpense
import finance.domain.MedicalProvider
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.PendingTransaction
import finance.domain.Summary
import finance.domain.Transaction
import finance.domain.Transfer
import finance.domain.ValidationAmount
import finance.handlers.CorsHandler
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.DescriptionService
import finance.services.FamilyMemberService
import finance.services.JwtTokenService
import finance.services.LoginAttemptService
import finance.services.MedicalExpenseService
import finance.services.MedicalProviderService
import finance.services.ParameterService
import finance.services.PaymentService
import finance.services.PendingTransactionService
import finance.services.SummaryService
import finance.services.TokenBlacklistService
import finance.services.TransactionService
import finance.services.TransferService
import finance.services.UserService
import finance.services.ValidationAmountService
import ratpack.core.error.ClientErrorHandler
import ratpack.core.error.ServerErrorHandler
import ratpack.core.handling.Context
import ratpack.exec.registry.Registry
import ratpack.hikari.HikariModule
import ratpack.core.server.ServerConfigBuilder

import static ratpack.groovy.Groovy.ratpack

SLF4JBridgeHandler.removeHandlersForRootLogger()
SLF4JBridgeHandler.install()

// Load config.yml before server initialisation so SSL paths are available
Map<String, Object> appCfg = new Yaml().load(
    Thread.currentThread().contextClassLoader.getResourceAsStream('config.yml')
) as Map<String, Object> ?: [:]
Map<String, Object> sslCfg = appCfg.ssl as Map<String, Object> ?: [:]
int serverPort = ((appCfg.server as Map)?.port as Integer) ?: 8443

ratpack {
    serverConfig { ServerConfigBuilder config ->
        port(serverPort)
        yaml('config.yml')
        json('db_config.json')
        ssl(SslContextBuilder.forServer(
            new File(sslCfg.certChain as String),
            new File(sslCfg.privateKey as String)
        ).build())
    }

    bindings {
        Properties hikariConfigProperties = serverConfig.get("/database", Properties)
        moduleConfig(HikariModule, new HikariConfig(hikariConfigProperties))

        bind(CategoryService)
        bind(DescriptionService)
        bind(AccountService)
        bind(PaymentService)
        bind(ParameterService)
        bind(TransactionService)
        bind(SummaryService)
        bind(ValidationAmountService)
        bind(TransferService)
        bind(PendingTransactionService)
        bind(FamilyMemberService)
        bind(MedicalProviderService)
        bind(MedicalExpenseService)
        bind(JwtTokenService)
        bind(LoginAttemptService)
        bind(TokenBlacklistService)
        bind(UserService)
        bindInstance(new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS))

        bindInstance(ClientErrorHandler, { Context ctx, int statusCode ->
            ctx.response.contentType('application/json')
            ctx.response.status(statusCode)
            ctx.render('{"error":"' + statusCode + '"}')
        } as ClientErrorHandler)

        bindInstance(ServerErrorHandler, { Context ctx, Throwable t ->
            ctx.response.contentType('application/json')
            ctx.response.status(500)
            ctx.render('{"error":"' + t.message?.replace('"', "'") + '"}')
        } as ServerErrorHandler)
    }

    handlers {
        all(new CorsHandler())

        // ===== JWT AUTHENTICATION MIDDLEWARE =====
        // Skips OPTIONS (CORS preflight) and public auth endpoints; validates all other requests.

        all { Context context, JwtTokenService jwtTokenService, TokenBlacklistService tokenBlacklistService ->
            String path = context.request.path
            String normalizedPath = path.startsWith('api/') ? path.substring(4) : path
            Set<String> publicPaths = ['login', 'register', 'logout', 'user/register', 'csrf'] as Set<String>
            if (normalizedPath in publicPaths || context.request.method.name == "OPTIONS") {
                context.next()
                return
            }

            String cookieHeader = context.request.headers.get("Cookie")
            String authHeader = context.request.headers.get("Authorization")
            String token = jwtTokenService.extractToken(cookieHeader, authHeader)

            if (!token) {
                context.response.status(401)
                render('{"error":"Authentication required"}')
                return
            }

            if (tokenBlacklistService.isBlacklisted(token)) {
                context.response.status(401)
                render('{"error":"Token has been revoked"}')
                return
            }

            try {
                def claims = jwtTokenService.parseClaims(token)
                String username = (String) claims.get(JwtTokenService.CLAIM_USERNAME)
                if (!username) {
                    context.response.status(401)
                    render('{"error":"Invalid token: missing username claim"}')
                    return
                }
                boolean keepLoggedIn = claims.get(JwtTokenService.CLAIM_KEEP_LOGGED_IN) as boolean ?: false
                context.next(Registry.single(new AuthenticatedUser(username, keepLoggedIn)))
            } catch (Exception e) {
                context.response.status(401)
                render('{"error":"Invalid or expired token"}')
            }
        }

        // ===== API ROUTES (all under /api/ prefix) =====

        prefix('api') {

        // ===== AUTH =====

        get('csrf') { Context context ->
            context.request.getBody().then {
                String token = UUID.randomUUID().toString().replace('-', '')
                render('{"csrfToken":"' + token + '"}')
            }
        }

        post('login') { Context context, JwtTokenService jwtTokenService, LoginAttemptService loginAttemptService, UserService userService, ObjectMapper objectMapper ->
            context.request.body.then {
                LoginRequest loginRequest = objectMapper.readValue(it.text, LoginRequest)
                String username = loginRequest.username?.trim() ?: ""

                if (loginAttemptService.isLocked(username)) {
                    long remaining = loginAttemptService.remainingLockSeconds(username)
                    context.response.status(429)
                    render(objectMapper.writeValueAsString([error: "Account temporarily locked. Try again in ${remaining} seconds."]))
                    return
                }

                if (!username || !loginRequest.password) {
                    context.response.status(400)
                    render('{"error":"Username and password are required"}')
                    return
                }

                Optional<User> userOpt = userService.signIn(username, loginRequest.password)
                if (userOpt.isEmpty()) {
                    loginAttemptService.recordFailure(username)
                    context.response.status(401)
                    render('{"error":"Invalid credentials"}')
                    return
                }

                loginAttemptService.recordSuccess(username)
                boolean keepLoggedIn = loginRequest.keepLoggedIn ?: false
                String token = jwtTokenService.buildToken(username, keepLoggedIn)
                context.response.headers.set("Set-Cookie", jwtTokenService.buildSetCookieHeader(token, keepLoggedIn))
                render(objectMapper.writeValueAsString([message: "Login successful"]))
            }
        }

        post('logout') { Context context, JwtTokenService jwtTokenService, TokenBlacklistService tokenBlacklistService ->
            context.request.getBody().then {
                String cookieHeader = context.request.headers.get("Cookie")
                String authHeader = context.request.headers.get("Authorization")
                String token = jwtTokenService.extractToken(cookieHeader, authHeader)

                if (token) {
                    try {
                        def claims = jwtTokenService.parseClaims(token)
                        long expiry = claims.expiration?.time ?: 0L
                        tokenBlacklistService.blacklistToken(token, expiry)
                    } catch (Exception ignored) {
                        // Token may already be expired; still clear the cookie
                    }
                }

                context.response.status(204)
                context.response.headers.set("Set-Cookie", jwtTokenService.buildClearCookieHeader())
                render('')
            }
        }

        post('register') { Context context, JwtTokenService jwtTokenService, UserService userService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    User newUser = objectMapper.readValue(it.text, User)
                    if (!newUser.username?.trim() || !newUser.password) {
                        context.response.status(400)
                        render('{"error":"Username and password are required"}')
                        return
                    }
                    userService.signUp(newUser)
                    String token = jwtTokenService.buildToken(newUser.username, false)
                    context.response.status(201)
                    context.response.headers.set("Set-Cookie", jwtTokenService.buildSetCookieHeader(token, false))
                    render(objectMapper.writeValueAsString([message: "Registration successful"]))
                } catch (IllegalArgumentException e) {
                    context.response.status(409)
                    render(objectMapper.writeValueAsString([error: e.message]))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render(objectMapper.writeValueAsString([error: e.message]))
                }
            }
        }

        post('user/register') { Context context, JwtTokenService jwtTokenService, UserService userService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    User newUser = objectMapper.readValue(it.text, User)
                    if (!newUser.username?.trim() || !newUser.password) {
                        context.response.status(400)
                        render('{"error":"Username and password are required"}')
                        return
                    }
                    userService.signUp(newUser)
                    String token = jwtTokenService.buildToken(newUser.username, false)
                    context.response.status(201)
                    context.response.headers.set("Set-Cookie", jwtTokenService.buildSetCookieHeader(token, false))
                    render(objectMapper.writeValueAsString([message: "Registration successful"]))
                } catch (IllegalArgumentException e) {
                    context.response.status(409)
                    render(objectMapper.writeValueAsString([error: e.message]))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render(objectMapper.writeValueAsString([error: e.message]))
                }
            }
        }

        post('refresh') { Context context, AuthenticatedUser principal, JwtTokenService jwtTokenService ->
            context.request.getBody().then {
                String token = jwtTokenService.buildToken(principal.username, principal.keepLoggedIn)
                context.response.headers.set("Set-Cookie", jwtTokenService.buildSetCookieHeader(token, principal.keepLoggedIn))
                render('{"message":"Token refreshed"}')
            }
        }

        get('me') { Context context, AuthenticatedUser principal, UserService userService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                User user = userService.findUserByUsername(principal.username)
                if (!user) {
                    context.response.status(404)
                    render('{"error":"User not found"}')
                    return
                }
                render(objectMapper.writeValueAsString(user))
            }
        }

        // ===== ACCOUNT =====

        get('account/totals') { Context context, SummaryService summaryService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Summary summary = summaryService.summaryAll()
                render(objectMapper.writeValueAsString(summary))
            }
        }

        // Fixed-segment account routes must precede account/:accountNameOwner
        get('account/select/active') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(accountService.accounts()))
            }
        }

        get('account/active') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(accountService.accounts()))
            }
        }

        get('account/payment/required') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(accountService.accountsRequiringPayment()))
            }
        }

        post('account') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    Account account = objectMapper.readValue(it.text, Account)
                    Account result = accountService.accountInsert(account)
                    context.response.status(201)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(409)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        put('account/rename') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String oldName = context.request.queryParams.get("old")
                String newName = context.request.queryParams.get("new")
                try {
                    Account result = accountService.accountRename(oldName, newName)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        put('account/deactivate/:accountNameOwner') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                try {
                    Account result = accountService.accountDeactivate(accountNameOwner)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        put('account/activate/:accountNameOwner') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                try {
                    Account result = accountService.accountActivate(accountNameOwner)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        get('account/validation/refresh') { Context context, AccountService accountService ->
            context.request.getBody().then {
                accountService.updateValidationDatesForAllAccounts()
                context.response.status(204)
                render('')
            }
        }

        path('account/:accountNameOwner') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            String accountNameOwner = context.pathTokens["accountNameOwner"]
            byMethod {
                get {
                    context.request.getBody().then {
                        Account account = accountService.account(accountNameOwner)
                        if (account) {
                            context.render(objectMapper.writeValueAsString(account))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"account not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            Account account = objectMapper.readValue(it.text, Account)
                            account.accountNameOwner = accountNameOwner
                            Account result = accountService.accountUpdate(account)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = accountService.accountDelete(accountNameOwner)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"account not found"}')
                        }
                    }
                }
            }
        }

        // ===== TRANSACTION =====

        get('transaction/account/totals/:accountNameOwner') { Context context, SummaryService summaryService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                Summary summary = summaryService.summary(accountNameOwner)
                render(objectMapper.writeValueAsString(summary))
            }
        }

        get('transaction/select/all') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(transactionService.transactionsAll()))
            }
        }

        get('transaction/active') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(transactionService.transactionsAll()))
            }
        }

        get('transaction/payment/required') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(accountService.accountsRequiringPayment()))
            }
        }

        get('transaction/account/select/:accountNameOwner') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                render(objectMapper.writeValueAsString(transactionService.transactions(accountNameOwner)))
            }
        }

        get('transaction/account/select/:accountNameOwner/paged') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                int page = (context.request.queryParams.get("page") ?: "0").toInteger()
                int size = (context.request.queryParams.get("size") ?: "50").toInteger()
                List<Transaction> all = transactionService.transactions(accountNameOwner)
                int total = all.size()
                int fromIndex = Math.min(page * size, total)
                int toIndex = Math.min(fromIndex + size, total)
                List<Transaction> content = all.subList(fromIndex, toIndex)
                int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0
                render(objectMapper.writeValueAsString([
                    content      : content,
                    totalElements: total,
                    totalPages   : totalPages,
                    pageNumber   : page,
                    pageSize     : size,
                    first        : page == 0,
                    last         : page >= totalPages - 1,
                    empty        : content.isEmpty()
                ]))
            }
        }

        get('transaction/category/:categoryName') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String categoryName = pathTokens["categoryName"]
                render(objectMapper.writeValueAsString(transactionService.transactionsByCategory(categoryName)))
            }
        }

        get('transaction/description/:descriptionName') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String descriptionName = pathTokens["descriptionName"]
                render(objectMapper.writeValueAsString(transactionService.transactionsByDescription(descriptionName)))
            }
        }

        get('transaction/date-range') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String startDateStr = context.request.queryParams.get("startDate")
                String endDateStr = context.request.queryParams.get("endDate")
                try {
                    java.time.LocalDate startDate = java.time.LocalDate.parse(startDateStr)
                    java.time.LocalDate endDate = java.time.LocalDate.parse(endDateStr)
                    List<Transaction> transactions = transactionService.transactionsByDateRange(startDate, endDate)
                    render(objectMapper.writeValueAsString(transactions))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        path('transaction/:guid') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            String guid = context.pathTokens["guid"]
            byMethod {
                get {
                    context.request.getBody().then {
                        Transaction transaction = transactionService.transaction(guid)
                        if (transaction) {
                            context.render(objectMapper.writeValueAsString(transaction))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"transaction not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            Transaction transaction = objectMapper.readValue(it.text, Transaction)
                            transaction.guid = guid
                            transactionService.transactionUpdate(transaction)
                            context.render(objectMapper.writeValueAsString(transaction))
                        } catch (RuntimeException e) {
                            context.response.status(400)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        try {
                            boolean deleted = transactionService.deleteTransaction(guid)
                            if (deleted) {
                                context.render('{}')
                            } else {
                                context.response.status(404)
                                context.render('{"error":"transaction not found"}')
                            }
                        } catch (RuntimeException e) {
                            context.response.status(409)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
            }
        }

        post('transaction/insert') { Context context, TransactionService transactionService, ObjectMapper objectMapper, AuthenticatedUser principal ->
            context.request.body.then {
                try {
                    Transaction transaction = objectMapper.readValue(it.text, Transaction)
                    transaction.owner = principal.username
                    Transaction result = transactionService.transactionInsert(transaction)
                    context.render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    System.err.println("transaction/insert error: ${e.class.simpleName}: ${e.message}")
                    e.printStackTrace()
                    context.response.status(400)
                    context.render('{"error":"' + e.message + '"}')
                }
            }
        }

        post('transaction') { Context context, TransactionService transactionService, ObjectMapper objectMapper, AuthenticatedUser principal ->
            context.request.body.then {
                try {
                    Transaction transaction = objectMapper.readValue(it.text, Transaction)
                    transaction.owner = principal.username
                    Transaction result = transactionService.transactionInsert(transaction)
                    context.response.status(201)
                    context.render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    System.err.println("transaction insert error: ${e.class.simpleName}: ${e.message}")
                    e.printStackTrace()
                    context.response.status(400)
                    context.render('{"error":"' + e.message + '"}')
                }
            }
        }

        post('transaction/future/insert') { Context context, TransactionService transactionService, ObjectMapper objectMapper, AuthenticatedUser principal ->
            context.request.body.then {
                try {
                    Transaction transaction = objectMapper.readValue(it.text, Transaction)
                    transaction.owner = principal.username
                    Transaction result = transactionService.transactionInsert(transaction)
                    context.render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    context.render('{"error":"' + e.message + '"}')
                }
            }
        }

        post('transaction/future') { Context context, TransactionService transactionService, ObjectMapper objectMapper, AuthenticatedUser principal ->
            context.request.body.then {
                try {
                    Transaction transaction = objectMapper.readValue(it.text, Transaction)
                    transaction.owner = principal.username
                    Transaction result = transactionService.transactionInsert(transaction)
                    context.response.status(201)
                    context.render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    context.render('{"error":"' + e.message + '"}')
                }
            }
        }

        put('transaction/state/update/:guid/:transactionState') { Context context, TransactionService transactionService ->
            context.request.body.then {
                String guid = context.pathTokens["guid"]
                String transactionState = context.pathTokens["transactionState"]
                transactionService.transactionStateUpdate(guid, transactionState)
                context.render('{}')
            }
        }

        delete('transaction/delete/:guid') { Context context, TransactionService transactionService ->
            context.request.getBody().then {
                String guid = context.pathTokens["guid"]
                transactionService.deleteTransaction(guid)
                context.render('{}')
            }
        }

        put('transaction/update/account') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    Map<String, Object> body = objectMapper.readValue(it.text, Map)
                    String guid = (String) body.get("guid")
                    String accountNameOwner = (String) body.get("accountNameOwner")
                    Transaction result = transactionService.changeAccountNameOwner(guid, accountNameOwner)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        get('transaction/account/bonus-progress/:accountNameOwner') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                try {
                    String startDateStr = context.request.queryParams.get("startDate")
                    BigDecimal targetAmount = new BigDecimal(context.request.queryParams.get("targetAmount"))
                    BigDecimal bonusAmount = new BigDecimal(context.request.queryParams.get("bonusAmount"))
                    long windowDays = Long.parseLong(context.request.queryParams.get("windowDays") ?: "90")
                    java.time.LocalDate startDate = java.time.LocalDate.parse(startDateStr)
                    BonusProgress bonusProgress = transactionService.calculateBonusProgress(accountNameOwner, startDate, targetAmount, bonusAmount, windowDays)
                    render(objectMapper.writeValueAsString(bonusProgress))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        // ===== CATEGORY =====

        get('categories') { Context context, CategoryService categoryService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(categoryService.categories()))
            }
        }

        get('category/active') { Context context, CategoryService categoryService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(categoryService.categories()))
            }
        }

        post('category') { Context context, CategoryService categoryService, ObjectMapper objectMapper ->
            context.request.body.then {
                Category category = objectMapper.readValue(it.text, Category)
                Category result = categoryService.categoryInsert(category)
                context.response.status(201)
                render(objectMapper.writeValueAsString(result))
            }
        }

        put('category/merge') { Context context, CategoryService categoryService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String oldName = context.request.queryParams.get("oldCategoryName")
                String newName = context.request.queryParams.get("newCategoryName")
                try {
                    Category result = categoryService.categoryMerge(oldName, newName)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        path('category/:categoryName') { Context context, CategoryService categoryService, ObjectMapper objectMapper ->
            String categoryName = context.pathTokens["categoryName"]
            byMethod {
                get {
                    context.request.getBody().then {
                        Category category = categoryService.category(categoryName)
                        if (category) {
                            context.render(objectMapper.writeValueAsString(category))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"category not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            Category category = objectMapper.readValue(it.text, Category)
                            category.categoryName = categoryName
                            Category result = categoryService.categoryUpdate(category)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = categoryService.categoryDelete(categoryName)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"category not found"}')
                        }
                    }
                }
            }
        }

        // ===== DESCRIPTION =====

        get('descriptions') { Context context, DescriptionService descriptionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(descriptionService.descriptions()))
            }
        }

        get('description/active') { Context context, DescriptionService descriptionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(descriptionService.descriptions()))
            }
        }

        post('description/insert') { Context context, DescriptionService descriptionService, ObjectMapper objectMapper ->
            context.request.body.then {
                Description description = objectMapper.readValue(it.text, Description)
                Description result = descriptionService.descriptionInsert(description)
                render(objectMapper.writeValueAsString(result))
            }
        }

        post('description') { Context context, DescriptionService descriptionService, ObjectMapper objectMapper ->
            context.request.body.then {
                Description description = objectMapper.readValue(it.text, Description)
                Description result = descriptionService.descriptionInsert(description)
                context.response.status(201)
                render(objectMapper.writeValueAsString(result))
            }
        }

        post('description/merge') { Context context, DescriptionService descriptionService, ObjectMapper objectMapper ->
            context.request.body.then {
                Map<String, Object> body = objectMapper.readValue(it.text, Map)
                List<String> sourceNames = (List<String>) body.get("sourceNames")
                String targetName = (String) body.get("targetName")
                descriptionService.descriptionsMerge(sourceNames, targetName)
                render('{}')
            }
        }

        path('description/:descriptionName') { Context context, DescriptionService descriptionService, ObjectMapper objectMapper ->
            String descriptionName = context.pathTokens["descriptionName"]
            byMethod {
                get {
                    context.request.getBody().then {
                        Description description = descriptionService.description(descriptionName)
                        if (description) {
                            context.render(objectMapper.writeValueAsString(description))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"description not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            Description description = objectMapper.readValue(it.text, Description)
                            description.descriptionName = descriptionName
                            Description result = descriptionService.descriptionUpdate(description)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = descriptionService.descriptionDelete(descriptionName)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"description not found"}')
                        }
                    }
                }
            }
        }

        // ===== PAYMENT =====

        get('payment/select') { Context context, PaymentService paymentService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(paymentService.payments()))
            }
        }

        get('payment/active') { Context context, PaymentService paymentService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(paymentService.payments()))
            }
        }

        path('payment/:paymentId') { Context context, PaymentService paymentService, ObjectMapper objectMapper ->
            Long paymentId = Long.parseLong(context.pathTokens["paymentId"])
            byMethod {
                get {
                    context.request.getBody().then {
                        Payment payment = paymentService.payment(paymentId)
                        if (payment) {
                            context.render(objectMapper.writeValueAsString(payment))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"payment not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            Payment payment = objectMapper.readValue(it.text, Payment)
                            payment.paymentId = paymentId
                            Payment result = paymentService.paymentUpdate(payment)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = paymentService.paymentDelete(paymentId)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"payment not found"}')
                        }
                    }
                }
            }
        }

        post('payment/insert') { Context context, PaymentService paymentService, ObjectMapper objectMapper, AuthenticatedUser principal ->
            context.request.body.then {
                try {
                    Payment payment = objectMapper.readValue(it.text, Payment)
                    payment.owner = principal.username
                    Payment result = paymentService.paymentInsert(payment)
                    context.render(objectMapper.writeValueAsString(result))
                } catch (Exception e) {
                    java.util.logging.Logger.getLogger("PaymentHandler").severe("payment insert error [${e.class.simpleName}]: ${e.message}")
                    context.response.status(400)
                    context.render('{"error":"' + e.message + '"}')
                }
            }
        }

        post('payment') { Context context, PaymentService paymentService, ObjectMapper objectMapper, AuthenticatedUser principal ->
            context.request.body.then {
                try {
                    Payment payment = objectMapper.readValue(it.text, Payment)
                    payment.owner = principal.username
                    Payment result = paymentService.paymentInsert(payment)
                    context.response.status(201)
                    context.render(objectMapper.writeValueAsString(result))
                } catch (Exception e) {
                    java.util.logging.Logger.getLogger("PaymentHandler").severe("payment insert error [${e.class.simpleName}]: ${e.message}")
                    context.response.status(400)
                    context.render('{"error":"' + e.message + '"}')
                }
            }
        }

        // ===== VALIDATION AMOUNT =====

        get('validation/amount/select/:accountNameOwner/cleared') { Context context, ValidationAmountService validationAmountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                ValidationAmount validationAmount = validationAmountService.validationAmount(accountNameOwner)
                render(objectMapper.writeValueAsString(validationAmount))
            }
        }

        get('validation/amount/active') { Context context, ValidationAmountService validationAmountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String accountNameOwner = context.request.queryParams.get("accountNameOwner")
                String transactionState = context.request.queryParams.get("transactionState") ?: "cleared"
                if (accountNameOwner) {
                    render(objectMapper.writeValueAsString(
                        validationAmountService.validationAmountsByAccountAndState(accountNameOwner, transactionState)
                    ))
                } else {
                    render(objectMapper.writeValueAsString(validationAmountService.validationAmounts()))
                }
            }
        }

        get('validation/amount/select/:accountNameOwner/:transactionState') { Context context, ValidationAmountService validationAmountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                String transactionState = pathTokens["transactionState"]
                render(objectMapper.writeValueAsString(
                        validationAmountService.validationAmountsByAccountAndState(accountNameOwner, transactionState)
                ))
            }
        }

        post('validation/amount/insert/:accountNameOwner') { Context context, ValidationAmountService validationAmountService, ObjectMapper objectMapper ->
            context.request.body.then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                ValidationAmount validationAmount = objectMapper.readValue(it.text, ValidationAmount)
                ValidationAmount result = validationAmountService.validationAmountInsert(accountNameOwner, validationAmount)
                render(objectMapper.writeValueAsString(result))
            }
        }

        post('validation/amount') { Context context, ValidationAmountService validationAmountService, ObjectMapper objectMapper ->
            context.request.body.then {
                ValidationAmount validationAmount = objectMapper.readValue(it.text, ValidationAmount)
                String accountNameOwner = context.request.queryParams.get("accountNameOwner") ?: ""
                ValidationAmount result = validationAmountService.validationAmountInsert(accountNameOwner, validationAmount)
                context.response.status(201)
                render(objectMapper.writeValueAsString(result))
            }
        }

        path('validation/amount/:validationId') { Context context, ValidationAmountService validationAmountService, ObjectMapper objectMapper ->
            Long validationId = Long.parseLong(context.pathTokens["validationId"])
            byMethod {
                get {
                    context.request.getBody().then {
                        ValidationAmount va = validationAmountService.validationAmountById(validationId)
                        if (va) {
                            context.render(objectMapper.writeValueAsString(va))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"validation amount not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            ValidationAmount va = objectMapper.readValue(it.text, ValidationAmount)
                            va.validationId = validationId
                            ValidationAmount result = validationAmountService.validationAmountUpdate(va)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = validationAmountService.validationAmountDelete(validationId)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"validation amount not found"}')
                        }
                    }
                }
            }
        }

        // ===== PARAMETER =====

        get('parameter/active') { Context context, ParameterService parameterService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(parameterService.parameters()))
            }
        }

        get('parm/select/:parameterName') { Context context, ParameterService parameterService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String parameterName = pathTokens["parameterName"]
                Parameter parameter = parameterService.parameter(parameterName)
                render(objectMapper.writeValueAsString(parameter))
            }
        }

        post('parameter') { Context context, ParameterService parameterService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    Parameter parameter = objectMapper.readValue(it.text, Parameter)
                    Parameter result = parameterService.parameterInsert(parameter)
                    context.response.status(201)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(409)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        path('parameter/:parameterName') { Context context, ParameterService parameterService, ObjectMapper objectMapper ->
            String parameterName = context.pathTokens["parameterName"]
            byMethod {
                get {
                    context.request.getBody().then {
                        Parameter parameter = parameterService.parameter(parameterName)
                        if (parameter) {
                            context.render(objectMapper.writeValueAsString(parameter))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"parameter not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            Parameter parameter = objectMapper.readValue(it.text, Parameter)
                            parameter.parameterName = parameterName
                            Parameter result = parameterService.parameterUpdate(parameter)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = parameterService.parameterDelete(parameterName)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"parameter not found"}')
                        }
                    }
                }
            }
        }

        // ===== TRANSFER =====

        get('transfer/active') { Context context, TransferService transferService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(transferService.transfers()))
            }
        }

        post('transfer') { Context context, TransferService transferService, ObjectMapper objectMapper, AuthenticatedUser principal ->
            context.request.body.then {
                try {
                    Transfer transfer = objectMapper.readValue(it.text, Transfer)
                    transfer.owner = principal.username
                    Transfer result = transferService.transferInsert(transfer)
                    context.response.status(201)
                    render(objectMapper.writeValueAsString(result))
                } catch (Exception e) {
                    log.error("transfer insert error [${e.class.simpleName}]: ${e.message}")
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        path('transfer/:transferId') { Context context, TransferService transferService, ObjectMapper objectMapper ->
            Long transferId = Long.parseLong(context.pathTokens["transferId"])
            byMethod {
                get {
                    context.request.getBody().then {
                        Transfer transfer = transferService.transfer(transferId)
                        if (transfer) {
                            context.render(objectMapper.writeValueAsString(transfer))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"transfer not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            Transfer transfer = objectMapper.readValue(it.text, Transfer)
                            transfer.transferId = transferId
                            Transfer result = transferService.transferUpdate(transfer)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = transferService.transferDelete(transferId)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"transfer not found"}')
                        }
                    }
                }
            }
        }

        // ===== PENDING TRANSACTION =====

        get('pending/transaction/active') { Context context, PendingTransactionService pendingTransactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(pendingTransactionService.pendingTransactions()))
            }
        }

        post('pending/transaction') { Context context, PendingTransactionService pendingTransactionService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    PendingTransaction pt = objectMapper.readValue(it.text, PendingTransaction)
                    PendingTransaction result = pendingTransactionService.pendingTransactionInsert(pt)
                    context.response.status(201)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('pending/transaction/delete/all') { Context context, PendingTransactionService pendingTransactionService ->
            context.request.getBody().then {
                pendingTransactionService.pendingTransactionDeleteAll()
                context.response.status(204)
                render('')
            }
        }

        path('pending/transaction/:pendingTransactionId') { Context context, PendingTransactionService pendingTransactionService, ObjectMapper objectMapper ->
            Long pendingTransactionId = Long.parseLong(context.pathTokens["pendingTransactionId"])
            byMethod {
                get {
                    context.request.getBody().then {
                        PendingTransaction pt = pendingTransactionService.pendingTransaction(pendingTransactionId)
                        if (pt) {
                            context.render(objectMapper.writeValueAsString(pt))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"pending transaction not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            PendingTransaction pt = objectMapper.readValue(it.text, PendingTransaction)
                            pt.pendingTransactionId = pendingTransactionId
                            PendingTransaction result = pendingTransactionService.pendingTransactionUpdate(pt)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = pendingTransactionService.pendingTransactionDelete(pendingTransactionId)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"pending transaction not found"}')
                        }
                    }
                }
            }
        }

        // ===== FAMILY MEMBER =====

        get('family-member/active') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(familyMemberService.familyMembers()))
            }
        }

        post('family-member') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    FamilyMember familyMember = objectMapper.readValue(it.text, FamilyMember)
                    FamilyMember result = familyMemberService.familyMemberInsert(familyMember)
                    context.response.status(201)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        get('family-member/owner/:owner/relationship/:relationship') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String owner = pathTokens["owner"]
                String relationship = pathTokens["relationship"]
                render(objectMapper.writeValueAsString(familyMemberService.familyMembersByOwnerAndRelationship(owner, relationship)))
            }
        }

        get('family-member/owner/:owner') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String owner = pathTokens["owner"]
                render(objectMapper.writeValueAsString(familyMemberService.familyMembersByOwner(owner)))
            }
        }

        put('family-member/:familyMemberId/activate') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long familyMemberId = Long.parseLong(pathTokens["familyMemberId"])
                try {
                    FamilyMember result = familyMemberService.familyMemberActivate(familyMemberId)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        put('family-member/:familyMemberId/deactivate') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long familyMemberId = Long.parseLong(pathTokens["familyMemberId"])
                try {
                    FamilyMember result = familyMemberService.familyMemberDeactivate(familyMemberId)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        path('family-member/:familyMemberId') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            Long familyMemberId = Long.parseLong(context.pathTokens["familyMemberId"])
            byMethod {
                get {
                    context.request.getBody().then {
                        FamilyMember familyMember = familyMemberService.familyMember(familyMemberId)
                        if (familyMember) {
                            context.render(objectMapper.writeValueAsString(familyMember))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"family member not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            FamilyMember familyMember = objectMapper.readValue(it.text, FamilyMember)
                            familyMember.familyMemberId = familyMemberId
                            FamilyMember result = familyMemberService.familyMemberUpdate(familyMember)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = familyMemberService.familyMemberDelete(familyMemberId)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"family member not found"}')
                        }
                    }
                }
            }
        }

        // ===== MEDICAL PROVIDER =====

        get('medical-provider/active') { Context context, MedicalProviderService medicalProviderService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalProviderService.medicalProviders()))
            }
        }

        post('medical-provider') { Context context, MedicalProviderService medicalProviderService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    MedicalProvider medicalProvider = objectMapper.readValue(it.text, MedicalProvider)
                    MedicalProvider result = medicalProviderService.medicalProviderInsert(medicalProvider)
                    context.response.status(201)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        path('medical-provider/:providerId') { Context context, MedicalProviderService medicalProviderService, ObjectMapper objectMapper ->
            Long providerId = Long.parseLong(context.pathTokens["providerId"])
            byMethod {
                get {
                    context.request.getBody().then {
                        MedicalProvider medicalProvider = medicalProviderService.medicalProvider(providerId)
                        if (medicalProvider) {
                            context.render(objectMapper.writeValueAsString(medicalProvider))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"medical provider not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            MedicalProvider medicalProvider = objectMapper.readValue(it.text, MedicalProvider)
                            medicalProvider.providerId = providerId
                            MedicalProvider result = medicalProviderService.medicalProviderUpdate(medicalProvider)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = medicalProviderService.medicalProviderDelete(providerId)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"medical provider not found"}')
                        }
                    }
                }
            }
        }

        // ===== MEDICAL EXPENSE =====

        get('medical-expense/active') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpenses()))
            }
        }

        // Fixed-segment route must precede the /:medicalExpenseId pattern
        get('medical-expense/owner/:owner') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String owner = pathTokens["owner"]
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpensesByOwner(owner)))
            }
        }

        post('medical-expense') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    MedicalExpense medicalExpense = objectMapper.readValue(it.text, MedicalExpense)
                    MedicalExpense result = medicalExpenseService.medicalExpenseInsert(medicalExpense)
                    context.response.status(201)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        path('medical-expense/:medicalExpenseId') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            Long medicalExpenseId = Long.parseLong(context.pathTokens["medicalExpenseId"])
            byMethod {
                get {
                    context.request.getBody().then {
                        MedicalExpense medicalExpense = medicalExpenseService.medicalExpense(medicalExpenseId)
                        if (medicalExpense) {
                            context.render(objectMapper.writeValueAsString(medicalExpense))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"medical expense not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            MedicalExpense medicalExpense = objectMapper.readValue(it.text, MedicalExpense)
                            medicalExpense.medicalExpenseId = medicalExpenseId
                            MedicalExpense result = medicalExpenseService.medicalExpenseUpdate(medicalExpense)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = medicalExpenseService.medicalExpenseDelete(medicalExpenseId)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"medical expense not found"}')
                        }
                    }
                }
            }
        }

        // ===== MEDICAL EXPENSES (plural paths mirroring endpoint project) =====

        get('medical-expenses/active') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpenses()))
            }
        }

        get('medical-expenses/all') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpenses()))
            }
        }

        get('medical-expenses/out-of-network') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.outOfNetworkExpenses()))
            }
        }

        get('medical-expenses/outstanding-balances') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.outstandingPatientBalances()))
            }
        }

        get('medical-expenses/open-claims') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.openClaims()))
            }
        }

        get('medical-expenses/date-range') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                try {
                    java.time.LocalDate startDate = java.time.LocalDate.parse(context.request.queryParams.get("startDate"))
                    java.time.LocalDate endDate = java.time.LocalDate.parse(context.request.queryParams.get("endDate"))
                    render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpensesByDateRange(startDate, endDate)))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        get('medical-expenses/unpaid') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.unpaidMedicalExpenses()))
            }
        }

        get('medical-expenses/partially-paid') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.partiallyPaidMedicalExpenses()))
            }
        }

        get('medical-expenses/fully-paid') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.fullyPaidMedicalExpenses()))
            }
        }

        get('medical-expenses/without-transaction') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpensesWithoutTransaction()))
            }
        }

        get('medical-expenses/overpaid') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.overpaidMedicalExpenses()))
            }
        }

        get('medical-expenses/claim-status-counts') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalExpenseService.claimStatusCounts()))
            }
        }

        get('medical-expenses/totals/unpaid-balance') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString([unpaidBalance: medicalExpenseService.totalUnpaidBalance()]))
            }
        }

        // 2-segment parameterised catch-all — must follow all fixed 2-segment routes above
        path('medical-expenses/:medicalExpenseId') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            Long medicalExpenseId = Long.parseLong(context.pathTokens["medicalExpenseId"])
            byMethod {
                get {
                    context.request.getBody().then {
                        MedicalExpense medicalExpense = medicalExpenseService.medicalExpense(medicalExpenseId)
                        if (medicalExpense) {
                            context.render(objectMapper.writeValueAsString(medicalExpense))
                        } else {
                            context.response.status(404)
                            context.render('{"error":"medical expense not found"}')
                        }
                    }
                }
                put {
                    context.request.body.then {
                        try {
                            MedicalExpense medicalExpense = objectMapper.readValue(it.text, MedicalExpense)
                            medicalExpense.medicalExpenseId = medicalExpenseId
                            MedicalExpense result = medicalExpenseService.medicalExpenseUpdate(medicalExpense)
                            context.render(objectMapper.writeValueAsString(result))
                        } catch (RuntimeException e) {
                            context.response.status(404)
                            context.render('{"error":"' + e.message + '"}')
                        }
                    }
                }
                delete {
                    context.request.getBody().then {
                        boolean deleted = medicalExpenseService.medicalExpenseDelete(medicalExpenseId)
                        if (deleted) {
                            context.render('{}')
                        } else {
                            context.response.status(404)
                            context.render('{"error":"medical expense not found"}')
                        }
                    }
                }
            }
        }

        get('medical-expenses/transaction/:transactionId') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long transactionId = Long.parseLong(pathTokens["transactionId"])
                MedicalExpense result = medicalExpenseService.medicalExpenseByTransactionId(transactionId)
                if (result) {
                    render(objectMapper.writeValueAsString(result))
                } else {
                    context.response.status(404)
                    render('{"error":"medical expense not found for transaction"}')
                }
            }
        }

        get('medical-expenses/account/:accountId') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long accountId = Long.parseLong(pathTokens["accountId"])
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpensesByAccountId(accountId)))
            }
        }

        get('medical-expenses/provider/:providerId') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long providerId = Long.parseLong(pathTokens["providerId"])
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpensesByProviderId(providerId)))
            }
        }

        get('medical-expenses/family-member/:familyMemberId') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long familyMemberId = Long.parseLong(pathTokens["familyMemberId"])
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpensesByFamilyMemberId(familyMemberId)))
            }
        }

        get('medical-expenses/claim-status/:claimStatus') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String claimStatus = pathTokens["claimStatus"]
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpensesByClaimStatus(claimStatus)))
            }
        }

        get('medical-expenses/procedure-code/:procedureCode') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String procedureCode = pathTokens["procedureCode"]
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpensesByProcedureCode(procedureCode)))
            }
        }

        get('medical-expenses/diagnosis-code/:diagnosisCode') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String diagnosisCode = pathTokens["diagnosisCode"]
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpensesByDiagnosisCode(diagnosisCode)))
            }
        }

        get('medical-expenses/account/:accountId/date-range') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long accountId = Long.parseLong(pathTokens["accountId"])
                try {
                    java.time.LocalDate startDate = java.time.LocalDate.parse(context.request.queryParams.get("startDate"))
                    java.time.LocalDate endDate = java.time.LocalDate.parse(context.request.queryParams.get("endDate"))
                    render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpensesByAccountIdAndDateRange(accountId, startDate, endDate)))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        get('medical-expenses/family-member/:familyMemberId/date-range') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long familyMemberId = Long.parseLong(pathTokens["familyMemberId"])
                try {
                    java.time.LocalDate startDate = java.time.LocalDate.parse(context.request.queryParams.get("startDate"))
                    java.time.LocalDate endDate = java.time.LocalDate.parse(context.request.queryParams.get("endDate"))
                    render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpensesByFamilyMemberIdAndDateRange(familyMemberId, startDate, endDate)))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        get('medical-expenses/totals/year/:year') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                int year = Integer.parseInt(pathTokens["year"])
                render(objectMapper.writeValueAsString(medicalExpenseService.medicalExpenseTotalsByYear(year)))
            }
        }

        get('medical-expenses/totals/year/:year/paid') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                int year = Integer.parseInt(pathTokens["year"])
                render(objectMapper.writeValueAsString([totalPaid: medicalExpenseService.totalPaidByYear(year)]))
            }
        }

        post('medical-expenses') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    MedicalExpense medicalExpense = objectMapper.readValue(it.text, MedicalExpense)
                    MedicalExpense result = medicalExpenseService.medicalExpenseInsert(medicalExpense)
                    context.response.status(201)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        put('medical-expenses/:medicalExpenseId/claim-status') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.body.then {
                Long medicalExpenseId = Long.parseLong(pathTokens["medicalExpenseId"])
                try {
                    Map<String, Object> body = objectMapper.readValue(it.text, Map)
                    String claimStatus = (String) body.get("claimStatus")
                    MedicalExpense result = medicalExpenseService.medicalExpenseUpdateClaimStatus(medicalExpenseId, claimStatus)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        post('medical-expenses/:medicalExpenseId/payments/:transactionId') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long medicalExpenseId = Long.parseLong(pathTokens["medicalExpenseId"])
                Long transactionId = Long.parseLong(pathTokens["transactionId"])
                try {
                    MedicalExpense result = medicalExpenseService.medicalExpenseLinkTransaction(medicalExpenseId, transactionId)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('medical-expenses/:medicalExpenseId/payments') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long medicalExpenseId = Long.parseLong(pathTokens["medicalExpenseId"])
                try {
                    MedicalExpense result = medicalExpenseService.medicalExpenseUnlinkTransaction(medicalExpenseId)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        // ===== UUID =====

        post('uuid/generate') { Context context, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString([
                    uuid     : UUID.randomUUID().toString(),
                    timestamp: System.currentTimeMillis(),
                    source   : "server"
                ]))
            }
        }

        post('uuid/generate/batch') { Context context, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String countParam = context.request.queryParams.get("count") ?: "1"
                int count = Integer.parseInt(countParam)
                if (count <= 0 || count > 100) {
                    context.response.status(400)
                    render('{"error":"count must be between 1 and 100"}')
                    return
                }
                List<String> uuids = (1..count).collect { UUID.randomUUID().toString() }
                render(objectMapper.writeValueAsString([
                    uuids    : uuids,
                    count    : uuids.size(),
                    timestamp: System.currentTimeMillis(),
                    source   : "server"
                ]))
            }
        }

        post('uuid/health') { Context context, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString([
                    status   : "healthy",
                    service  : "uuid-generation",
                    timestamp: System.currentTimeMillis()
                ]))
            }
        }

        // ===== GRAPHQL =====

        post('graphql') { Context context, TransferService transferService, AccountService accountService, PaymentService paymentService, ObjectMapper objectMapper, AuthenticatedUser principal ->
            context.request.body.then {
                try {
                    Map body = objectMapper.readValue(it.text, Map)
                    String query = (String) body.get("query") ?: ""
                    Map variables = (Map) body.get("variables") ?: [:]
                    Map data = [:]

                    if (query.trim().startsWith("mutation")) {
                        if (query.contains("createTransfer")) {
                            Map t = (Map) variables.get("transfer") ?: [:]
                            Transfer transfer = new Transfer()
                            transfer.sourceAccount = (String) t.get("sourceAccount")
                            transfer.destinationAccount = (String) t.get("destinationAccount")
                            transfer.amount = new BigDecimal(t.get("amount").toString())
                            transfer.activeStatus = t.get("activeStatus") as Boolean ?: true
                            transfer.owner = principal.username
                            transfer.transactionDate = java.sql.Date.valueOf((String) t.get("transactionDate"))
                            data.createTransfer = transferService.transferInsert(transfer)
                        } else if (query.contains("createPayment")) {
                            Map p = (Map) variables.get("payment") ?: [:]
                            Payment payment = new Payment()
                            payment.sourceAccount = (String) p.get("sourceAccount")
                            payment.destinationAccount = (String) p.get("destinationAccount")
                            payment.amount = new BigDecimal(p.get("amount").toString())
                            payment.activeStatus = p.get("activeStatus") as Boolean ?: true
                            payment.owner = principal.username
                            payment.transactionDate = java.sql.Date.valueOf((String) p.get("transactionDate"))
                            data.createPayment = paymentService.paymentInsert(payment)
                        } else if (query.contains("deletePayment")) {
                            Long id = Long.parseLong(variables.get("id").toString())
                            data.deletePayment = paymentService.paymentDelete(id)
                        } else if (query.contains("updatePayment")) {
                            Long id = Long.parseLong(variables.get("id").toString())
                            Map p = (Map) variables.get("payment") ?: [:]
                            Payment payment = new Payment()
                            payment.paymentId = id
                            payment.sourceAccount = (String) p.get("sourceAccount")
                            payment.destinationAccount = (String) p.get("destinationAccount")
                            payment.amount = new BigDecimal(p.get("amount").toString())
                            payment.activeStatus = p.get("activeStatus") as Boolean ?: true
                            payment.transactionDate = java.sql.Date.valueOf((String) p.get("transactionDate"))
                            data.updatePayment = paymentService.paymentUpdate(payment)
                        }
                    } else {
                        if (query.contains("transfers")) {
                            data.transfers = transferService.transfers()
                        }
                        if (query.contains("accounts")) {
                            data.accounts = accountService.accounts()
                        }
                        if (query.contains("payments")) {
                            data.payments = paymentService.payments()
                        }
                    }
                    render(objectMapper.writeValueAsString([data: data]))
                } catch (Exception e) {
                    java.util.logging.Logger.getLogger("GraphQL").severe("graphql error: ${e.message}")
                    render(objectMapper.writeValueAsString([errors: [[message: e.message]]]))
                }
            }
        }

        } // end prefix('api')

        // ===== ROOT-LEVEL ALIASES (for Next.js proxy compatibility) =====

        get('transaction/account/select/:accountNameOwner') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                render(objectMapper.writeValueAsString(transactionService.transactions(accountNameOwner)))
            }
        }

        get('account/select/active') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(accountService.accounts()))
            }
        }

        post('transaction/insert') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    Transaction transaction = objectMapper.readValue(it.text, Transaction)
                    Transaction result = transactionService.transactionInsert(transaction)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('transaction/delete/:guid') { Context context, TransactionService transactionService ->
            context.request.getBody().then {
                String guid = pathTokens["guid"]
                transactionService.deleteTransaction(guid)
                render('{}')
            }
        }

        post('graphql') { Context context, TransferService transferService, AccountService accountService, PaymentService paymentService, ObjectMapper objectMapper, AuthenticatedUser principal ->
            context.request.body.then {
                try {
                    Map body = objectMapper.readValue(it.text, Map)
                    String query = (String) body.get("query") ?: ""
                    Map variables = (Map) body.get("variables") ?: [:]
                    Map data = [:]

                    if (query.trim().startsWith("mutation")) {
                        if (query.contains("createTransfer")) {
                            Map t = (Map) variables.get("transfer") ?: [:]
                            Transfer transfer = new Transfer()
                            transfer.sourceAccount = (String) t.get("sourceAccount")
                            transfer.destinationAccount = (String) t.get("destinationAccount")
                            transfer.amount = new BigDecimal(t.get("amount").toString())
                            transfer.activeStatus = t.get("activeStatus") as Boolean ?: true
                            transfer.owner = principal.username
                            transfer.transactionDate = java.sql.Date.valueOf((String) t.get("transactionDate"))
                            data.createTransfer = transferService.transferInsert(transfer)
                        } else if (query.contains("createPayment")) {
                            Map p = (Map) variables.get("payment") ?: [:]
                            Payment payment = new Payment()
                            payment.sourceAccount = (String) p.get("sourceAccount")
                            payment.destinationAccount = (String) p.get("destinationAccount")
                            payment.amount = new BigDecimal(p.get("amount").toString())
                            payment.activeStatus = p.get("activeStatus") as Boolean ?: true
                            payment.owner = principal.username
                            payment.transactionDate = java.sql.Date.valueOf((String) p.get("transactionDate"))
                            data.createPayment = paymentService.paymentInsert(payment)
                        } else if (query.contains("deletePayment")) {
                            Long id = Long.parseLong(variables.get("id").toString())
                            data.deletePayment = paymentService.paymentDelete(id)
                        } else if (query.contains("updatePayment")) {
                            Long id = Long.parseLong(variables.get("id").toString())
                            Map p = (Map) variables.get("payment") ?: [:]
                            Payment payment = new Payment()
                            payment.paymentId = id
                            payment.sourceAccount = (String) p.get("sourceAccount")
                            payment.destinationAccount = (String) p.get("destinationAccount")
                            payment.amount = new BigDecimal(p.get("amount").toString())
                            payment.activeStatus = p.get("activeStatus") as Boolean ?: true
                            payment.transactionDate = java.sql.Date.valueOf((String) p.get("transactionDate"))
                            data.updatePayment = paymentService.paymentUpdate(payment)
                        }
                    } else {
                        if (query.contains("transfers")) {
                            data.transfers = transferService.transfers()
                        }
                        if (query.contains("accounts")) {
                            data.accounts = accountService.accounts()
                        }
                        if (query.contains("payments")) {
                            data.payments = paymentService.payments()
                        }
                    }
                    render(objectMapper.writeValueAsString([data: data]))
                } catch (Exception e) {
                    java.util.logging.Logger.getLogger("GraphQL").severe("graphql error: ${e.message}")
                    render(objectMapper.writeValueAsString([errors: [[message: e.message]]]))
                }
            }
        }

        // ===== STATIC FILES =====

        files {
            dir "public"
            indexFiles "index.html"
        }
    }
}
