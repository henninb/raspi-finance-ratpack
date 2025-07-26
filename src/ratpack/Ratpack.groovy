import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import finance.domain.Account
import finance.domain.Category
import finance.domain.Description
import finance.domain.Parameter
import finance.domain.Payment
import finance.domain.Summary
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.handlers.CorsHandler
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.DescriptionService
import finance.services.ParameterService
import finance.services.PaymentService
import finance.services.SummaryService
import finance.services.TransactionService
import finance.services.ValidationAmountService

//import gql.ratpack.GraphQLHandler
//import ratpack.ssl.SSLContexts
//import io.netty.handler.ssl.SslContext
import ratpack.core.handling.Context
import ratpack.hikari.HikariModule
import ratpack.core.server.ServerConfigBuilder

import static ratpack.groovy.Groovy.ratpack

ratpack {
    serverConfig { ServerConfigBuilder config ->
       // port(5050)
        port(8080)
        json('db_config.json')
        //ssl SSLContexts.sslContext(new File('ssl/hornsup-raspi-finance.jks'), 'monday1')

        //https://github.com/merscwog/ratpack-ssl-test/tree/03b8d325708ae1a3fd20e3c35a5ead178b883703
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
        bind(ObjectMapper)
    }

    handlers {
        all(new CorsHandler())

        get ('account/totals') {
            Context context, SummaryService summaryService, ObjectMapper objectMapper ->
                context.request.getBody().then {
                    Summary summary = summaryService.summaryAll()
                    String json = objectMapper.writeValueAsString(summary)
                    render(json)
                }
        }

        get('transaction/account/totals/:accountNameOwner') {
            Context context, SummaryService summaryService, ObjectMapper objectMapper ->
                context.request.getBody().then {
                    String accountNameOwner = pathTokens["accountNameOwner"]
                    Summary summary = summaryService.summary(accountNameOwner)
                    String json = objectMapper.writeValueAsString(summary)
                    render(json)
                }
        }

        get('parm/select/:parameterName') {
            Context context, ParameterService parameterService, ObjectMapper objectMapper ->
                context.request.getBody().then {
                    String parameterName = pathTokens["parameterName"]
                    Parameter parameter = parameterService.parameter(parameterName)
                    String json = objectMapper.writeValueAsString(parameter)
                    render(json)
                }
        }

        get ('account/select/active') {
            Context context, AccountService accountService, ObjectMapper objectMapper ->
                context.request.getBody().then {
                    List<Account> accounts = accountService.accounts()
                    String json = objectMapper.writeValueAsString(accounts)
                    render(json)
                }
        }

        get ('payment/select') {
            Context context, PaymentService paymentService, ObjectMapper objectMapper ->
                context.request.getBody().then {
                    List<Payment> payments = paymentService.payments()
                    String json = objectMapper.writeValueAsString(payments)
                    render(json)
                }
        }


        get ('categories') {
            Context context, CategoryService categoryService, ObjectMapper objectMapper ->
                context.request.getBody().then {
                    List<Category> categories = categoryService.categories()
                    String json = objectMapper.writeValueAsString(categories)
                    render(json)
                }
        }

        get( 'transaction/account/select/:accountNameOwner') {

            Context context, TransactionService transactionService, ObjectMapper objectMapper ->
                context.request.getBody().then {
                    String accountNameOwner = pathTokens["accountNameOwner"]
                    List<Transaction> transactions = transactionService.transactions(accountNameOwner)
                    render(objectMapper.writeValueAsString(transactions))
                }
        }

        get('validation/amount/select/:accountNameOwner/cleared') {
            Context context, ValidationAmountService validationAmountService, ObjectMapper objectMapper ->
                context.request.getBody().then {
                    String accountNameOwner = pathTokens["accountNameOwner"]
                    ValidationAmount validationAmount = validationAmountService.validationAmount(accountNameOwner)
                    render(objectMapper.writeValueAsString(validationAmount))
                }
        }

        get ('transaction/select/all') {
            Context context, TransactionService transactionService, ObjectMapper objectMapper ->
                context.request.getBody().then {
                    List<Transaction> transactions = transactionService.transactionsAll()
                    render(objectMapper.writeValueAsString(transactions))
                }
        }

        get ('descriptions') {
            Context context, DescriptionService descriptionService, ObjectMapper objectMapper ->
                context.request.getBody().then {
                    List<Description> descriptions = descriptionService.descriptions()
                    render(objectMapper.writeValueAsString(descriptions))
                }
        }

        get('transaction/payment/required') {
            //TODO: need to code this
        }

        delete('transaction/delete/:guid') {
            Context context, TransactionService transactionService ->
                context.request.getBody().then {
                    String guid = pathTokens["guid"]
                    transactionService.deleteTransaction(guid)
                    println('transaction delete called')
                    render('{}')
                }
        }

        post('validation/amount/insert/:accountNameOwner') {
            Context context, ValidationAmountService validationAmountService, ObjectMapper objectMapper ->
                context.request.body.then {
                    String accountNameOwner = pathTokens["accountNameOwner"]
                    println(it.text)
                    ValidationAmount validationAmount = objectMapper.readValue(it.text, ValidationAmount)
                    ValidationAmount validationAmountResult = validationAmountService.validationAmountInsert(accountNameOwner, validationAmount)
                    println(objectMapper.writeValueAsString(validationAmountResult))
                    render(objectMapper.writeValueAsString(validationAmountResult))
                    //render('[]')
                }
        }

        //post('graphql', GraphQLHandler)
        post('graphql') {
            render('[]')
            //TODO: fix
        }

        post('description/insert') {
            Context context, DescriptionService descriptionService, ObjectMapper objectMapper ->
                context.request.body.then {
                    println(it.text)
                    Description description = objectMapper.readValue(it.text, Description)
                    Description descriptionResult = descriptionService.descriptionInsert(description)
                    render(objectMapper.writeValueAsString(descriptionResult))
                }

        }

        post('transaction/insert') {
            Context context, TransactionService transactionService, ObjectMapper objectMapper ->
                context.request.body.then {
                    Transaction transaction = objectMapper.readValue(it.text, Transaction)
                    Transaction transactionResult = transactionService.transactionInsert(transaction)
                    render(objectMapper.writeValueAsString(transactionResult))
                }
        }

        post('payment/insert') {
            Context context, PaymentService paymentService, ObjectMapper objectMapper ->
                context.request.body.then {
                    println("response body: " + it.text)
                    Payment payment = objectMapper.readValue(it.text, Payment)
                    Payment paymentResult = paymentService.paymentInsert(payment)
                    println payment
                    render(objectMapper.writeValueAsString(paymentResult))
                }
        }

        post('transaction/future/insert') {
            Context context, TransactionService transactionService, ObjectMapper objectMapper ->
                context.request.body.then {
                    Transaction transaction = objectMapper.readValue(it.text, Transaction)
                    println(it.text)
                    //TODO: fix
                    render('created future transaction')
                }

        }

        put('transaction/state/update/:guid/:transactionState') {
            Context context, TransactionService transactionService, ObjectMapper objectMapper ->
                context.request.body.then {
                    String guid = pathTokens["guid"]
                    String transactionState = pathTokens["transactionState"]

                    println(transactionState)
                    println(guid)
                    transactionService.transactionStateUpdate(guid, transactionState)
                    render('updated transaction record')
                }
        }
    }
}
