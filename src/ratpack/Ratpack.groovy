import com.fasterxml.jackson.databind.ObjectMapper
import com.zaxxer.hikari.HikariConfig
import finance.domain.Account
import finance.domain.Category
import finance.domain.Description
import finance.domain.Transaction
import finance.handlers.CorsHandler
import finance.services.AccountService
import finance.services.CategoryService
import finance.services.DescriptionService
import finance.services.PaymentService
import finance.services.TransactionService
import ratpack.ssl.SSLContexts

//import postgres.PostgresConfig
//import postgres.PostgresModule

import ratpack.handling.Context
import ratpack.hikari.HikariModule
import ratpack.server.ServerConfigBuilder

import static ratpack.groovy.Groovy.ratpack

ratpack {
    serverConfig { ServerConfigBuilder config ->
       // port(5050)
        port(8443)
        json('db_config.json')
        ssl SSLContexts.sslContext(new File('ssl/hornsup-raspi-finance.jks'), 'monday1')

        //https://github.com/merscwog/ratpack-ssl-test/tree/03b8d325708ae1a3fd20e3c35a5ead178b883703
    }

    bindings {
        Properties hikariConfigProperties = serverConfig.get("/database", Properties)
        moduleConfig(HikariModule, new HikariConfig(hikariConfigProperties))

        bind(CategoryService)
        bind(DescriptionService)
        bind(AccountService)
        bind(PaymentService)
        bind(TransactionService)
    }

    handlers {
        all(new CorsHandler())
        get ('account/totals') {
                render('{totals:0, totalsCleared:0, totalsOutstanding:0, totalsFuture:0}')
        }

        get ('account/select/active') {
        //get ('accounts') {
            Context context, AccountService accountService ->
                context.request.getBody().then { typed ->
                    List<Account> accounts = accountService.accounts()
                    ObjectMapper objectMapper = new ObjectMapper()
                    String json = objectMapper.writeValueAsString(accounts)
                    render(json)
                }
        }

        get ('categories') {
            Context context, CategoryService categoryService ->
                context.request.getBody().then { typed ->
                    List<Category> categories = categoryService.categories()
                    ObjectMapper objectMapper = new ObjectMapper()
                    String json = objectMapper.writeValueAsString(categories)
                    render(json)
                }
        }

        get( 'transaction/account/select/:accountNameOwner') {

            Context context, TransactionService transactionService ->
                context.request.getBody().then { typed ->
                    String accountNameOwner = pathTokens["accountNameOwner"]
                    List<Transaction> transactions = transactionService.transactions(accountNameOwner)
                    ObjectMapper objectMapper = new ObjectMapper()
                    String json = objectMapper.writeValueAsString(transactions)
                    render(json)
                }
        }

        get('validation/amount/select/:accountNameOwner/cleared') {
            String accountNameOwner = pathTokens["accountNameOwner"]
            //validation/amount/select/amazon-store_brian/cleared
            render('[]')
        }


        get('transaction/account/totals/:accountNameOwner') {
            //	/transaction/account/totals/amazon-store_brian
            String accountNameOwner = pathTokens["accountNameOwner"]
            render('{totals:0, totalsCleared:0, totalsOutstanding:0, totalsFuture:0}')
        }

        //get ('transactions') {
        get ('transaction/select/all') {
            Context context, TransactionService transactionService ->
                context.request.getBody().then { typed ->
                    List<Transaction> transactions = transactionService.transactionsAll()
                    ObjectMapper objectMapper = new ObjectMapper()
                    String json = objectMapper.writeValueAsString(transactions)
                    render(json)
                }
        }

        get ('descriptions') {
            Context context, DescriptionService descriptionService ->
                context.request.getBody().then { typed ->
                    List<Description> descriptions = descriptionService.descriptions()
                    ObjectMapper objectMapper = new ObjectMapper()
                    String json = objectMapper.writeValueAsString(descriptions)
                    render(json)
                }
        }
    }
}
