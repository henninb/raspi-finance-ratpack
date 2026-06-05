import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import finance.domain.Account
import finance.domain.BonusProgress
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
import finance.services.MedicalExpenseService
import finance.services.MedicalProviderService
import finance.services.ParameterService
import finance.services.PaymentService
import finance.services.PendingTransactionService
import finance.services.SummaryService
import finance.services.TransactionService
import finance.services.TransferService
import finance.services.ValidationAmountService
import ratpack.core.handling.Context
import ratpack.hikari.HikariModule
import ratpack.core.server.ServerConfigBuilder

import static ratpack.groovy.Groovy.ratpack

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
        bind(ObjectMapper)
    }

    handlers {
        all(new CorsHandler())

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

        get('account/:accountNameOwner') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                Account account = accountService.account(accountNameOwner)
                if (account) {
                    render(objectMapper.writeValueAsString(account))
                } else {
                    context.response.status(404)
                    render('{"error":"account not found"}')
                }
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

        put('account/:accountNameOwner') { Context context, AccountService accountService, ObjectMapper objectMapper ->
            context.request.body.then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                try {
                    Account account = objectMapper.readValue(it.text, Account)
                    account.accountNameOwner = accountNameOwner
                    Account result = accountService.accountUpdate(account)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('account/:accountNameOwner') { Context context, AccountService accountService ->
            context.request.getBody().then {
                String accountNameOwner = pathTokens["accountNameOwner"]
                boolean deleted = accountService.accountDelete(accountNameOwner)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"account not found"}')
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

        get('transaction/:guid') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String guid = pathTokens["guid"]
                Transaction transaction = transactionService.transaction(guid)
                if (transaction) {
                    render(objectMapper.writeValueAsString(transaction))
                } else {
                    context.response.status(404)
                    render('{"error":"transaction not found"}')
                }
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

        post('transaction') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    Transaction transaction = objectMapper.readValue(it.text, Transaction)
                    Transaction result = transactionService.transactionInsert(transaction)
                    context.response.status(201)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        post('transaction/future/insert') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
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

        post('transaction/future') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    Transaction transaction = objectMapper.readValue(it.text, Transaction)
                    Transaction result = transactionService.transactionInsert(transaction)
                    context.response.status(201)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        put('transaction/state/update/:guid/:transactionState') { Context context, TransactionService transactionService ->
            context.request.body.then {
                String guid = pathTokens["guid"]
                String transactionState = pathTokens["transactionState"]
                transactionService.transactionStateUpdate(guid, transactionState)
                render('{}')
            }
        }

        put('transaction/:guid') { Context context, TransactionService transactionService, ObjectMapper objectMapper ->
            context.request.body.then {
                String guid = pathTokens["guid"]
                try {
                    Transaction transaction = objectMapper.readValue(it.text, Transaction)
                    transaction.guid = guid
                    transactionService.transactionUpdate(transaction)
                    render(objectMapper.writeValueAsString(transaction))
                } catch (RuntimeException e) {
                    context.response.status(404)
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

        delete('transaction/:guid') { Context context, TransactionService transactionService ->
            context.request.getBody().then {
                String guid = pathTokens["guid"]
                boolean deleted = transactionService.deleteTransaction(guid)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"transaction not found"}')
                }
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

        get('category/:categoryName') { Context context, CategoryService categoryService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String categoryName = pathTokens["categoryName"]
                Category category = categoryService.category(categoryName)
                if (category) {
                    render(objectMapper.writeValueAsString(category))
                } else {
                    context.response.status(404)
                    render('{"error":"category not found"}')
                }
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

        put('category/:categoryName') { Context context, CategoryService categoryService, ObjectMapper objectMapper ->
            context.request.body.then {
                String categoryName = pathTokens["categoryName"]
                try {
                    Category category = objectMapper.readValue(it.text, Category)
                    category.categoryName = categoryName
                    Category result = categoryService.categoryUpdate(category)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('category/:categoryName') { Context context, CategoryService categoryService ->
            context.request.getBody().then {
                String categoryName = pathTokens["categoryName"]
                boolean deleted = categoryService.categoryDelete(categoryName)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"category not found"}')
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

        get('description/:descriptionName') { Context context, DescriptionService descriptionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String descriptionName = pathTokens["descriptionName"]
                Description description = descriptionService.description(descriptionName)
                if (description) {
                    render(objectMapper.writeValueAsString(description))
                } else {
                    context.response.status(404)
                    render('{"error":"description not found"}')
                }
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

        put('description/:descriptionName') { Context context, DescriptionService descriptionService, ObjectMapper objectMapper ->
            context.request.body.then {
                String descriptionName = pathTokens["descriptionName"]
                try {
                    Description description = objectMapper.readValue(it.text, Description)
                    description.descriptionName = descriptionName
                    Description result = descriptionService.descriptionUpdate(description)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('description/:descriptionName') { Context context, DescriptionService descriptionService ->
            context.request.getBody().then {
                String descriptionName = pathTokens["descriptionName"]
                boolean deleted = descriptionService.descriptionDelete(descriptionName)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"description not found"}')
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

        get('payment/:paymentId') { Context context, PaymentService paymentService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long paymentId = Long.parseLong(pathTokens["paymentId"])
                Payment payment = paymentService.payment(paymentId)
                if (payment) {
                    render(objectMapper.writeValueAsString(payment))
                } else {
                    context.response.status(404)
                    render('{"error":"payment not found"}')
                }
            }
        }

        post('payment/insert') { Context context, PaymentService paymentService, ObjectMapper objectMapper ->
            context.request.body.then {
                Payment payment = objectMapper.readValue(it.text, Payment)
                Payment result = paymentService.paymentInsert(payment)
                render(objectMapper.writeValueAsString(result))
            }
        }

        post('payment') { Context context, PaymentService paymentService, ObjectMapper objectMapper ->
            context.request.body.then {
                Payment payment = objectMapper.readValue(it.text, Payment)
                Payment result = paymentService.paymentInsert(payment)
                context.response.status(201)
                render(objectMapper.writeValueAsString(result))
            }
        }

        put('payment/:paymentId') { Context context, PaymentService paymentService, ObjectMapper objectMapper ->
            context.request.body.then {
                Long paymentId = Long.parseLong(pathTokens["paymentId"])
                try {
                    Payment payment = objectMapper.readValue(it.text, Payment)
                    payment.paymentId = paymentId
                    Payment result = paymentService.paymentUpdate(payment)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('payment/:paymentId') { Context context, PaymentService paymentService ->
            context.request.getBody().then {
                Long paymentId = Long.parseLong(pathTokens["paymentId"])
                boolean deleted = paymentService.paymentDelete(paymentId)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"payment not found"}')
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
                render(objectMapper.writeValueAsString(validationAmountService.validationAmounts()))
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

        get('validation/amount/:validationId') { Context context, ValidationAmountService validationAmountService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long validationId = Long.parseLong(pathTokens["validationId"])
                ValidationAmount va = validationAmountService.validationAmountById(validationId)
                if (va) {
                    render(objectMapper.writeValueAsString(va))
                } else {
                    context.response.status(404)
                    render('{"error":"validation amount not found"}')
                }
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

        put('validation/amount/:validationId') { Context context, ValidationAmountService validationAmountService, ObjectMapper objectMapper ->
            context.request.body.then {
                Long validationId = Long.parseLong(pathTokens["validationId"])
                try {
                    ValidationAmount va = objectMapper.readValue(it.text, ValidationAmount)
                    va.validationId = validationId
                    ValidationAmount result = validationAmountService.validationAmountUpdate(va)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('validation/amount/:validationId') { Context context, ValidationAmountService validationAmountService ->
            context.request.getBody().then {
                Long validationId = Long.parseLong(pathTokens["validationId"])
                boolean deleted = validationAmountService.validationAmountDelete(validationId)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"validation amount not found"}')
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

        get('parameter/:parameterName') { Context context, ParameterService parameterService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String parameterName = pathTokens["parameterName"]
                Parameter parameter = parameterService.parameter(parameterName)
                if (parameter) {
                    render(objectMapper.writeValueAsString(parameter))
                } else {
                    context.response.status(404)
                    render('{"error":"parameter not found"}')
                }
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

        put('parameter/:parameterName') { Context context, ParameterService parameterService, ObjectMapper objectMapper ->
            context.request.body.then {
                String parameterName = pathTokens["parameterName"]
                try {
                    Parameter parameter = objectMapper.readValue(it.text, Parameter)
                    parameter.parameterName = parameterName
                    Parameter result = parameterService.parameterUpdate(parameter)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('parameter/:parameterName') { Context context, ParameterService parameterService ->
            context.request.getBody().then {
                String parameterName = pathTokens["parameterName"]
                boolean deleted = parameterService.parameterDelete(parameterName)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"parameter not found"}')
                }
            }
        }

        // ===== TRANSFER =====

        get('transfer/active') { Context context, TransferService transferService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(transferService.transfers()))
            }
        }

        get('transfer/:transferId') { Context context, TransferService transferService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long transferId = Long.parseLong(pathTokens["transferId"])
                Transfer transfer = transferService.transfer(transferId)
                if (transfer) {
                    render(objectMapper.writeValueAsString(transfer))
                } else {
                    context.response.status(404)
                    render('{"error":"transfer not found"}')
                }
            }
        }

        post('transfer') { Context context, TransferService transferService, ObjectMapper objectMapper ->
            context.request.body.then {
                try {
                    Transfer transfer = objectMapper.readValue(it.text, Transfer)
                    Transfer result = transferService.transferInsert(transfer)
                    context.response.status(201)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(400)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        put('transfer/:transferId') { Context context, TransferService transferService, ObjectMapper objectMapper ->
            context.request.body.then {
                Long transferId = Long.parseLong(pathTokens["transferId"])
                try {
                    Transfer transfer = objectMapper.readValue(it.text, Transfer)
                    transfer.transferId = transferId
                    Transfer result = transferService.transferUpdate(transfer)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('transfer/:transferId') { Context context, TransferService transferService ->
            context.request.getBody().then {
                Long transferId = Long.parseLong(pathTokens["transferId"])
                boolean deleted = transferService.transferDelete(transferId)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"transfer not found"}')
                }
            }
        }

        // ===== PENDING TRANSACTION =====

        get('pending/transaction/active') { Context context, PendingTransactionService pendingTransactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(pendingTransactionService.pendingTransactions()))
            }
        }

        get('pending/transaction/:pendingTransactionId') { Context context, PendingTransactionService pendingTransactionService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long pendingTransactionId = Long.parseLong(pathTokens["pendingTransactionId"])
                PendingTransaction pt = pendingTransactionService.pendingTransaction(pendingTransactionId)
                if (pt) {
                    render(objectMapper.writeValueAsString(pt))
                } else {
                    context.response.status(404)
                    render('{"error":"pending transaction not found"}')
                }
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

        put('pending/transaction/:pendingTransactionId') { Context context, PendingTransactionService pendingTransactionService, ObjectMapper objectMapper ->
            context.request.body.then {
                Long pendingTransactionId = Long.parseLong(pathTokens["pendingTransactionId"])
                try {
                    PendingTransaction pt = objectMapper.readValue(it.text, PendingTransaction)
                    pt.pendingTransactionId = pendingTransactionId
                    PendingTransaction result = pendingTransactionService.pendingTransactionUpdate(pt)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
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

        delete('pending/transaction/:pendingTransactionId') { Context context, PendingTransactionService pendingTransactionService ->
            context.request.getBody().then {
                Long pendingTransactionId = Long.parseLong(pathTokens["pendingTransactionId"])
                boolean deleted = pendingTransactionService.pendingTransactionDelete(pendingTransactionId)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"pending transaction not found"}')
                }
            }
        }

        // ===== FAMILY MEMBER =====

        get('family-member/active') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(familyMemberService.familyMembers()))
            }
        }

        get('family-member/:familyMemberId') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long familyMemberId = Long.parseLong(pathTokens["familyMemberId"])
                FamilyMember familyMember = familyMemberService.familyMember(familyMemberId)
                if (familyMember) {
                    render(objectMapper.writeValueAsString(familyMember))
                } else {
                    context.response.status(404)
                    render('{"error":"family member not found"}')
                }
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

        put('family-member/:familyMemberId') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            context.request.body.then {
                Long familyMemberId = Long.parseLong(pathTokens["familyMemberId"])
                try {
                    FamilyMember familyMember = objectMapper.readValue(it.text, FamilyMember)
                    familyMember.familyMemberId = familyMemberId
                    FamilyMember result = familyMemberService.familyMemberUpdate(familyMember)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('family-member/:familyMemberId') { Context context, FamilyMemberService familyMemberService ->
            context.request.getBody().then {
                Long familyMemberId = Long.parseLong(pathTokens["familyMemberId"])
                boolean deleted = familyMemberService.familyMemberDelete(familyMemberId)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"family member not found"}')
                }
            }
        }

        get('family-member/owner/:owner') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String owner = pathTokens["owner"]
                render(objectMapper.writeValueAsString(familyMemberService.familyMembersByOwner(owner)))
            }
        }

        get('family-member/owner/:owner/relationship/:relationship') { Context context, FamilyMemberService familyMemberService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                String owner = pathTokens["owner"]
                String relationship = pathTokens["relationship"]
                render(objectMapper.writeValueAsString(familyMemberService.familyMembersByOwnerAndRelationship(owner, relationship)))
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

        // ===== MEDICAL PROVIDER =====

        get('medical-provider/active') { Context context, MedicalProviderService medicalProviderService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                render(objectMapper.writeValueAsString(medicalProviderService.medicalProviders()))
            }
        }

        get('medical-provider/:providerId') { Context context, MedicalProviderService medicalProviderService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long providerId = Long.parseLong(pathTokens["providerId"])
                MedicalProvider medicalProvider = medicalProviderService.medicalProvider(providerId)
                if (medicalProvider) {
                    render(objectMapper.writeValueAsString(medicalProvider))
                } else {
                    context.response.status(404)
                    render('{"error":"medical provider not found"}')
                }
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

        put('medical-provider/:providerId') { Context context, MedicalProviderService medicalProviderService, ObjectMapper objectMapper ->
            context.request.body.then {
                Long providerId = Long.parseLong(pathTokens["providerId"])
                try {
                    MedicalProvider medicalProvider = objectMapper.readValue(it.text, MedicalProvider)
                    medicalProvider.providerId = providerId
                    MedicalProvider result = medicalProviderService.medicalProviderUpdate(medicalProvider)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('medical-provider/:providerId') { Context context, MedicalProviderService medicalProviderService ->
            context.request.getBody().then {
                Long providerId = Long.parseLong(pathTokens["providerId"])
                boolean deleted = medicalProviderService.medicalProviderDelete(providerId)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"medical provider not found"}')
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

        get('medical-expense/:medicalExpenseId') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long medicalExpenseId = Long.parseLong(pathTokens["medicalExpenseId"])
                MedicalExpense medicalExpense = medicalExpenseService.medicalExpense(medicalExpenseId)
                if (medicalExpense) {
                    render(objectMapper.writeValueAsString(medicalExpense))
                } else {
                    context.response.status(404)
                    render('{"error":"medical expense not found"}')
                }
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

        put('medical-expense/:medicalExpenseId') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.body.then {
                Long medicalExpenseId = Long.parseLong(pathTokens["medicalExpenseId"])
                try {
                    MedicalExpense medicalExpense = objectMapper.readValue(it.text, MedicalExpense)
                    medicalExpense.medicalExpenseId = medicalExpenseId
                    MedicalExpense result = medicalExpenseService.medicalExpenseUpdate(medicalExpense)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
                    render('{"error":"' + e.message + '"}')
                }
            }
        }

        delete('medical-expense/:medicalExpenseId') { Context context, MedicalExpenseService medicalExpenseService ->
            context.request.getBody().then {
                Long medicalExpenseId = Long.parseLong(pathTokens["medicalExpenseId"])
                boolean deleted = medicalExpenseService.medicalExpenseDelete(medicalExpenseId)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"medical expense not found"}')
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
        get('medical-expenses/:medicalExpenseId') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.getBody().then {
                Long medicalExpenseId = Long.parseLong(pathTokens["medicalExpenseId"])
                MedicalExpense medicalExpense = medicalExpenseService.medicalExpense(medicalExpenseId)
                if (medicalExpense) {
                    render(objectMapper.writeValueAsString(medicalExpense))
                } else {
                    context.response.status(404)
                    render('{"error":"medical expense not found"}')
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

        put('medical-expenses/:medicalExpenseId') { Context context, MedicalExpenseService medicalExpenseService, ObjectMapper objectMapper ->
            context.request.body.then {
                Long medicalExpenseId = Long.parseLong(pathTokens["medicalExpenseId"])
                try {
                    MedicalExpense medicalExpense = objectMapper.readValue(it.text, MedicalExpense)
                    medicalExpense.medicalExpenseId = medicalExpenseId
                    MedicalExpense result = medicalExpenseService.medicalExpenseUpdate(medicalExpense)
                    render(objectMapper.writeValueAsString(result))
                } catch (RuntimeException e) {
                    context.response.status(404)
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

        delete('medical-expenses/:medicalExpenseId') { Context context, MedicalExpenseService medicalExpenseService ->
            context.request.getBody().then {
                Long medicalExpenseId = Long.parseLong(pathTokens["medicalExpenseId"])
                boolean deleted = medicalExpenseService.medicalExpenseDelete(medicalExpenseId)
                if (deleted) {
                    render('{}')
                } else {
                    context.response.status(404)
                    render('{"error":"medical expense not found"}')
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

        // ===== GRAPHQL (stub) =====

        post('graphql') {
            render('[]')
        }

        // ===== STATIC FILES =====

        files {
            dir "public"
            indexFiles "index.html"
        }
    }
}
