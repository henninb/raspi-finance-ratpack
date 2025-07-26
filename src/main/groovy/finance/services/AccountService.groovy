package finance.services

import finance.domain.Account
import finance.repositories.AccountRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject

@Log
@CompileStatic
class AccountService implements Service {

    private AccountRepository accountRepository

    @Inject
    AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository
    }

    List<Account> accounts() {
        return accountRepository.accounts()
    }
}
