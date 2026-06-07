package finance.service

import finance.domain.Account
import finance.domain.TransactionState
import finance.domain.ValidationAmount
import finance.repositories.AccountRepository
import finance.repositories.ValidationAmountRepository
import finance.services.ValidationAmountService
import spock.lang.Specification

import java.sql.Timestamp

class ValidationAmountServiceSpec extends Specification {

    ValidationAmountRepository validationAmountRepository = Mock()
    AccountRepository accountRepository = Mock()
    ValidationAmountService service = new ValidationAmountService(validationAmountRepository, accountRepository)

    private ValidationAmount buildValidationAmount(Long id = 1L) {
        return new ValidationAmount(
                validationId: id,
                accountId: 1L,
                owner: 'henninb@gmail.com',
                transactionState: TransactionState.cleared,
                amount: 100.00,
                activeStatus: true,
                validationDate: new Timestamp(System.currentTimeMillis())
        )
    }

    def 'validationAmounts delegates to repository'() {
        given:
        validationAmountRepository.validationAmounts() >> [buildValidationAmount()]

        when:
        List<ValidationAmount> result = service.validationAmounts()

        then:
        result.size() == 1
    }

    def 'validationAmount returns latest by date for account'() {
        given:
        Account account = new Account(accountNameOwner: 'chase_brian', accountId: 1L)
        accountRepository.account('chase_brian') >> account
        ValidationAmount older = buildValidationAmount(1L)
        older.validationDate = new Timestamp(System.currentTimeMillis() - 10_000L)
        ValidationAmount newer = buildValidationAmount(2L)
        newer.validationDate = new Timestamp(System.currentTimeMillis())
        validationAmountRepository.validationAmounts(1L) >> [older, newer]

        when:
        ValidationAmount result = service.validationAmount('chase_brian')

        then:
        result.validationId == 2L
    }

    def 'validationAmount returns empty ValidationAmount when none found'() {
        given:
        Account account = new Account(accountNameOwner: 'chase_brian', accountId: 1L)
        accountRepository.account('chase_brian') >> account
        validationAmountRepository.validationAmounts(1L) >> []

        when:
        ValidationAmount result = service.validationAmount('chase_brian')

        then:
        result != null
        result.validationId == null
    }

    def 'validationAmountInsert sets accountId from account lookup and timestamps'() {
        given:
        Account account = new Account(accountNameOwner: 'chase_brian', accountId: 42L)
        accountRepository.account('chase_brian') >> account
        ValidationAmount va = new ValidationAmount(amount: 500.00, transactionState: TransactionState.cleared)

        when:
        ValidationAmount result = service.validationAmountInsert('chase_brian', va)

        then:
        1 * validationAmountRepository.validationAmountInsert(va)
        result.accountId == 42L
        result.dateAdded != null
        result.dateUpdated != null
    }

    def 'validationAmountUpdate throws when not found'() {
        given:
        ValidationAmount va = buildValidationAmount(99L)
        validationAmountRepository.validationAmount(99L) >> null

        when:
        service.validationAmountUpdate(va)

        then:
        thrown(RuntimeException)
    }

    def 'validationAmountUpdate delegates and returns refreshed record'() {
        given:
        ValidationAmount existing = buildValidationAmount(1L)
        ValidationAmount updated = buildValidationAmount(1L)
        updated.amount = 999.00
        validationAmountRepository.validationAmount(1L) >>> [existing, updated]

        when:
        ValidationAmount result = service.validationAmountUpdate(existing)

        then:
        1 * validationAmountRepository.validationAmountUpdate(existing)
        result.amount == 999.00
    }

    def 'validationAmountDelete returns false when not found'() {
        given:
        validationAmountRepository.validationAmount(99L) >> null

        expect:
        !service.validationAmountDelete(99L)
    }

    def 'validationAmountDelete returns true when found'() {
        given:
        validationAmountRepository.validationAmount(1L) >> buildValidationAmount()
        validationAmountRepository.validationAmountDelete(1L) >> true

        expect:
        service.validationAmountDelete(1L)
    }
}
