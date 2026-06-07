package finance.service

import finance.domain.Account
import finance.domain.AccountType
import finance.repositories.AccountRepository
import finance.services.AccountService
import spock.lang.Specification

class AccountServiceSpec extends Specification {

    AccountRepository accountRepository = Mock()
    AccountService service = new AccountService(accountRepository)

    def 'accounts delegates to repository'() {
        given:
        Account a = new Account(accountNameOwner: 'chase_brian', accountType: AccountType.credit)
        accountRepository.accounts() >> [a]

        when:
        List<Account> result = service.accounts()

        then:
        result == [a]
    }

    def 'accountInsert sets timestamps and inserts new account'() {
        given:
        Account account = new Account(accountNameOwner: 'chase_brian', accountType: AccountType.credit)
        accountRepository.account('chase_brian') >> null

        when:
        Account result = service.accountInsert(account)

        then:
        1 * accountRepository.accountInsert(account)
        result == account
        result.dateAdded != null
        result.dateUpdated != null
    }

    def 'accountInsert throws when account already exists'() {
        given:
        Account account = new Account(accountNameOwner: 'chase_brian')
        accountRepository.account('chase_brian') >> new Account(accountNameOwner: 'chase_brian')

        when:
        service.accountInsert(account)

        then:
        thrown(RuntimeException)
        0 * accountRepository.accountInsert(_)
    }

    def 'accountUpdate throws when account does not exist'() {
        given:
        Account account = new Account(accountNameOwner: 'missing_brian')
        accountRepository.account('missing_brian') >> null

        when:
        service.accountUpdate(account)

        then:
        thrown(RuntimeException)
    }

    def 'accountUpdate delegates update and returns refreshed account'() {
        given:
        Account existing = new Account(accountNameOwner: 'chase_brian')
        Account updated = new Account(accountNameOwner: 'chase_brian', moniker: '1234')
        accountRepository.account('chase_brian') >>> [existing, updated]

        when:
        Account result = service.accountUpdate(new Account(accountNameOwner: 'chase_brian', moniker: '1234'))

        then:
        1 * accountRepository.accountUpdate(_)
        result.moniker == '1234'
    }

    def 'accountDelete returns false when account not found'() {
        given:
        accountRepository.account('missing_brian') >> null

        expect:
        !service.accountDelete('missing_brian')
    }

    def 'accountDelete returns true when account exists'() {
        given:
        accountRepository.account('chase_brian') >> new Account(accountNameOwner: 'chase_brian')
        accountRepository.accountDelete('chase_brian') >> true

        expect:
        service.accountDelete('chase_brian')
    }

    def 'accountRename throws when source account not found'() {
        given:
        accountRepository.account('old_brian') >> null

        when:
        service.accountRename('old_brian', 'new_brian')

        then:
        thrown(RuntimeException)
    }

    def 'accountRename renames and returns updated account'() {
        given:
        accountRepository.account('old_brian') >> new Account(accountNameOwner: 'old_brian')
        accountRepository.account('new_brian') >> new Account(accountNameOwner: 'new_brian')

        when:
        Account result = service.accountRename('old_brian', 'new_brian')

        then:
        1 * accountRepository.accountRename('old_brian', 'new_brian')
        result.accountNameOwner == 'new_brian'
    }

    def 'accountDeactivate throws when account not found'() {
        given:
        accountRepository.account('missing_brian') >> null

        when:
        service.accountDeactivate('missing_brian')

        then:
        thrown(RuntimeException)
    }

    def 'accountDeactivate deactivates and returns updated account'() {
        given:
        Account account = new Account(accountNameOwner: 'chase_brian', activeStatus: false)
        accountRepository.account('chase_brian') >>> [new Account(accountNameOwner: 'chase_brian'), account]

        when:
        Account result = service.accountDeactivate('chase_brian')

        then:
        1 * accountRepository.accountDeactivate('chase_brian')
        result.activeStatus == false
    }

    def 'accountActivate throws when account not found'() {
        given:
        accountRepository.account('missing_brian') >> null

        when:
        service.accountActivate('missing_brian')

        then:
        thrown(RuntimeException)
    }

    def 'accountActivate activates and returns updated account'() {
        given:
        Account account = new Account(accountNameOwner: 'chase_brian', activeStatus: true)
        accountRepository.account('chase_brian') >>> [new Account(accountNameOwner: 'chase_brian'), account]

        when:
        Account result = service.accountActivate('chase_brian')

        then:
        1 * accountRepository.accountActivate('chase_brian')
        result.activeStatus == true
    }

    def 'sumOfAllTransactionsByTransactionState scales result to 2 decimal places'() {
        given:
        accountRepository.sumTransactionsByState('cleared') >> new BigDecimal('100.555')

        when:
        BigDecimal result = service.sumOfAllTransactionsByTransactionState('cleared')

        then:
        result == new BigDecimal('100.56')
        result.scale() == 2
    }
}
