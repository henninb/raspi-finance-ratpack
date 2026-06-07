package finance.services

import finance.domain.Account
import finance.repositories.AccountRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ratpack.core.service.Service

import javax.inject.Inject
import java.math.RoundingMode
import java.sql.Timestamp

@Slf4j
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

    List<Account> accountsRequiringPayment() {
        accountRepository.updateTotalsForAllAccounts()
        accountRepository.updateValidationDates()
        return accountRepository.accountsRequiringPayment()
    }

    Account account(String accountNameOwner) {
        return accountRepository.account(accountNameOwner)
    }

    Account accountInsert(Account account) {
        account.dateUpdated = new Timestamp(System.currentTimeMillis())
        account.dateAdded = new Timestamp(System.currentTimeMillis())
        if (accountRepository.account(account.accountNameOwner)) {
            throw new RuntimeException("account already exists: ${account.accountNameOwner}")
        }
        accountRepository.accountInsert(account)
        log.info("inserted account ${account.accountNameOwner}")
        return account
    }

    Account accountUpdate(Account account) {
        Account existing = accountRepository.account(account.accountNameOwner)
        if (!existing) {
            throw new RuntimeException("account not found: ${account.accountNameOwner}")
        }
        accountRepository.accountUpdate(account)
        return accountRepository.account(account.accountNameOwner)
    }

    boolean accountDelete(String accountNameOwner) {
        Account existing = accountRepository.account(accountNameOwner)
        if (!existing) {
            return false
        }
        return accountRepository.accountDelete(accountNameOwner)
    }

    Account accountRename(String oldAccountNameOwner, String newAccountNameOwner) {
        if (!accountRepository.account(oldAccountNameOwner)) {
            throw new RuntimeException("account not found: ${oldAccountNameOwner}")
        }
        accountRepository.accountRename(oldAccountNameOwner, newAccountNameOwner)
        return accountRepository.account(newAccountNameOwner)
    }

    Account accountDeactivate(String accountNameOwner) {
        Account existing = accountRepository.account(accountNameOwner)
        if (!existing) {
            throw new RuntimeException("account not found: ${accountNameOwner}")
        }
        accountRepository.accountDeactivate(accountNameOwner)
        return accountRepository.account(accountNameOwner)
    }

    Account accountActivate(String accountNameOwner) {
        Account existing = accountRepository.account(accountNameOwner)
        if (!existing) {
            throw new RuntimeException("account not found: ${accountNameOwner}")
        }
        accountRepository.accountActivate(accountNameOwner)
        return accountRepository.account(accountNameOwner)
    }

    boolean updateTotalsForAllAccounts() {
        return accountRepository.updateTotalsForAllAccounts()
    }

    void updateValidationDatesForAllAccounts() {
        accountRepository.updateValidationDates()
    }

    BigDecimal sumOfAllTransactionsByTransactionState(String transactionState) {
        return accountRepository.sumTransactionsByState(transactionState).setScale(2, RoundingMode.HALF_UP)
    }
}
