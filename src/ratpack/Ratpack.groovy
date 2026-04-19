import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import finance.domain.Account
import io.netty.handler.ssl.SslContextBuilder
import org.yaml.snakeyaml.Yaml
import finance.domain.Category
import finance.domain.Description
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
