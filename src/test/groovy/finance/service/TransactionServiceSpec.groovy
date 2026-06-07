package finance.service

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Category
import finance.domain.Description
import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.repositories.AccountRepository
import finance.repositories.CategoryRepository
import finance.repositories.DescriptionRepository
import finance.repositories.PaymentRepository
import finance.repositories.TransactionRepository
import finance.repositories.TransferRepository
import finance.services.TransactionService
import spock.lang.Specification

import java.sql.Date
import java.time.LocalDate

class TransactionServiceSpec extends Specification {

    TransactionRepository transactionRepository = Mock()
    AccountRepository accountRepository = Mock()
    CategoryRepository categoryRepository = Mock()
    DescriptionRepository descriptionRepository = Mock()
    PaymentRepository paymentRepository = Mock()
    TransferRepository transferRepository = Mock()

    TransactionService service = new TransactionService(
            transactionRepository, accountRepository, categoryRepository,
            descriptionRepository, paymentRepository, transferRepository)

    private Transaction buildTransaction(String accountNameOwner = 'chase_brian', String description = 'walmart') {
        return new Transaction(
                guid: UUID.randomUUID().toString(),
                accountNameOwner: accountNameOwner,
                owner: 'henninb@gmail.com',
                description: description,
                category: 'groceries',
                amount: 10.00,
                transactionDate: new Date(System.currentTimeMillis()),
                transactionState: TransactionState.outstanding,
                transactionType: TransactionType.expense,
                reoccurringType: ReoccurringType.onetime,
                activeStatus: true
        )
    }

    private Account buildAccount(String name = 'chase_brian') {
        return new Account(accountNameOwner: name, accountId: 1L, accountType: AccountType.credit, activeStatus: true)
    }

    def 'transactionInsert creates new category and description, lowercases description'() {
        given:
        Transaction t = buildTransaction('chase_brian', 'WALMART')
        categoryRepository.category('henninb@gmail.com', 'groceries') >> null
        descriptionRepository.description('henninb@gmail.com', 'walmart') >> null
        accountRepository.account('chase_brian') >> buildAccount()

        when:
        Transaction result = service.transactionInsert(t)

        then:
        1 * categoryRepository.categoryInsert({ Category c -> c.categoryName == 'groceries' })
        1 * descriptionRepository.descriptionInsert({ Description d -> d.descriptionName == 'walmart' })
        1 * transactionRepository.transactionInsert(t)
        result.description == 'walmart'
    }

    def 'transactionInsert skips category insert when category already exists'() {
        given:
        Transaction t = buildTransaction()
        categoryRepository.category('henninb@gmail.com', 'groceries') >> new Category(categoryName: 'groceries')
        descriptionRepository.description('henninb@gmail.com', 'walmart') >> new Description(descriptionName: 'walmart')
        accountRepository.account('chase_brian') >> buildAccount()

        when:
        service.transactionInsert(t)

        then:
        0 * categoryRepository.categoryInsert(_)
        0 * descriptionRepository.descriptionInsert(_)
        1 * transactionRepository.transactionInsert(t)
    }

    def 'transactionInsert throws when account not found'() {
        given:
        Transaction t = buildTransaction()
        categoryRepository.category(_, _) >> null
        descriptionRepository.description(_, _) >> null
        accountRepository.account('chase_brian') >> null

        when:
        service.transactionInsert(t)

        then:
        thrown(RuntimeException)
        0 * transactionRepository.transactionInsert(_)
    }

    def 'transactionInsert skips description handling when description is null'() {
        given:
        Transaction t = buildTransaction()
        t.description = null
        categoryRepository.category(_, _) >> null
        accountRepository.account('chase_brian') >> buildAccount()

        when:
        service.transactionInsert(t)

        then:
        0 * descriptionRepository._
        1 * transactionRepository.transactionInsert(t)
    }

    def 'transactionInsert sets accountType and accountId from account'() {
        given:
        Transaction t = buildTransaction()
        categoryRepository.category(_, _) >> new Category(categoryName: 'groceries')
        descriptionRepository.description(_, _) >> new Description(descriptionName: 'walmart')
        Account account = buildAccount()
        accountRepository.account('chase_brian') >> account

        when:
        Transaction result = service.transactionInsert(t)

        then:
        result.accountType == AccountType.credit
        result.accountId == 1L
    }

    def 'transactionUpdate throws when transaction not found'() {
        given:
        transactionRepository.transaction(_) >> null

        when:
        service.transactionUpdate(buildTransaction())

        then:
        thrown(RuntimeException)
    }

    def 'transactionUpdate lowercases description and creates new description if absent'() {
        given:
        Transaction existing = buildTransaction()
        existing.owner = 'henninb@gmail.com'
        transactionRepository.transaction(_) >> existing

        Transaction update = buildTransaction()
        update.description = 'TARGET'
        categoryRepository.category('henninb@gmail.com', 'groceries') >> new Category(categoryName: 'groceries')
        descriptionRepository.description('henninb@gmail.com', 'target') >> null

        when:
        service.transactionUpdate(update)

        then:
        1 * descriptionRepository.descriptionInsert({ Description d -> d.descriptionName == 'target' })
        update.description == 'target'
    }

    def 'deleteTransaction returns false when transaction not found'() {
        given:
        transactionRepository.transaction('missing-guid') >> null

        expect:
        !service.deleteTransaction('missing-guid')
    }

    def 'deleteTransaction throws when referenced by a payment'() {
        given:
        String guid = UUID.randomUUID().toString()
        transactionRepository.transaction(guid) >> buildTransaction()
        paymentRepository.existsByTransactionGuid(guid) >> true

        when:
        service.deleteTransaction(guid)

        then:
        thrown(RuntimeException)
    }

    def 'deleteTransaction throws when referenced by a transfer'() {
        given:
        String guid = UUID.randomUUID().toString()
        transactionRepository.transaction(guid) >> buildTransaction()
        paymentRepository.existsByTransactionGuid(guid) >> false
        transferRepository.existsByTransactionGuid(guid) >> true

        when:
        service.deleteTransaction(guid)

        then:
        thrown(RuntimeException)
    }

    def 'deleteTransaction deletes when no references exist'() {
        given:
        String guid = UUID.randomUUID().toString()
        transactionRepository.transaction(guid) >> buildTransaction()
        paymentRepository.existsByTransactionGuid(guid) >> false
        transferRepository.existsByTransactionGuid(guid) >> false
        transactionRepository.transactionDelete(guid) >> true

        expect:
        service.deleteTransaction(guid)
    }

    def 'transactionsByDateRange throws when startDate is after endDate'() {
        given:
        LocalDate start = LocalDate.of(2026, 6, 10)
        LocalDate end = LocalDate.of(2026, 6, 1)

        when:
        service.transactionsByDateRange(start, end)

        then:
        thrown(RuntimeException)
    }

    def 'transactionsByDateRange delegates when range is valid'() {
        given:
        LocalDate start = LocalDate.of(2026, 1, 1)
        LocalDate end = LocalDate.of(2026, 6, 1)
        transactionRepository.transactionsByDateRange(start, end) >> [buildTransaction()]

        when:
        List<Transaction> result = service.transactionsByDateRange(start, end)

        then:
        result.size() == 1
    }

    def 'changeAccountNameOwner throws when new account not found'() {
        given:
        accountRepository.account('missing_brian') >> null

        when:
        service.changeAccountNameOwner('some-guid', 'missing_brian')

        then:
        thrown(RuntimeException)
    }

    def 'changeAccountNameOwner throws when transaction not found'() {
        given:
        accountRepository.account('chase_brian') >> buildAccount()
        transactionRepository.transaction('missing-guid') >> null

        when:
        service.changeAccountNameOwner('missing-guid', 'chase_brian')

        then:
        thrown(RuntimeException)
    }

    def 'changeAccountNameOwner delegates and returns updated transaction'() {
        given:
        String guid = UUID.randomUUID().toString()
        Account account = buildAccount()
        Transaction updated = buildTransaction('chase_brian')
        accountRepository.account('chase_brian') >> account
        transactionRepository.transaction(guid) >> buildTransaction()
        transactionRepository.transaction(guid) >> updated

        when:
        service.changeAccountNameOwner(guid, 'chase_brian')

        then:
        1 * transactionRepository.changeAccountNameOwner(guid, 'chase_brian', 1L)
    }

    def 'calculateBonusProgress computes remaining and percent correctly'() {
        given:
        LocalDate start = LocalDate.of(2026, 1, 1)
        transactionRepository.sumSpendingInWindow('chase_brian', start, start.plusDays(89), 'cleared') >> 600.00
        transactionRepository.sumPendingSpendingInWindow('chase_brian', start, start.plusDays(89), ['outstanding', 'future']) >> 100.00

        when:
        def progress = service.calculateBonusProgress('chase_brian', start, 1000.00, 200.00, 90)

        then:
        progress.spent == 600.00
        progress.remaining == 400.00
        progress.bonusEarned == false
        progress.percentComplete == 60.0d
    }

    def 'calculateBonusProgress marks bonusEarned when spent meets target'() {
        given:
        LocalDate start = LocalDate.of(2026, 1, 1)
        transactionRepository.sumSpendingInWindow('chase_brian', start, start.plusDays(89), 'cleared') >> 1000.00
        transactionRepository.sumPendingSpendingInWindow(_, _, _, _) >> 0.00

        when:
        def progress = service.calculateBonusProgress('chase_brian', start, 1000.00, 200.00, 90)

        then:
        progress.bonusEarned == true
        progress.remaining == 0.00
        progress.percentComplete == 100.0d
    }
}
