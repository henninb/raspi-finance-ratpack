package finance.service

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Payment
import finance.domain.Transaction
import finance.repositories.AccountRepository
import finance.repositories.PaymentRepository
import finance.services.PaymentService
import finance.services.TransactionService
import spock.lang.Specification

import java.sql.Date

class PaymentServiceSpec extends Specification {

    PaymentRepository paymentRepository = Mock()
    AccountRepository accountRepository = Mock()
    TransactionService transactionService = Mock()
    PaymentService service = new PaymentService(paymentRepository, accountRepository, transactionService)

    private Payment buildPayment() {
        return new Payment(
                sourceAccount: 'checking_brian',
                destinationAccount: 'chase_brian',
                amount: 500.00,
                transactionDate: new Date(System.currentTimeMillis()),
                owner: 'henninb@gmail.com',
                activeStatus: true
        )
    }

    def 'payments delegates to repository'() {
        given:
        paymentRepository.payments() >> [buildPayment()]

        when:
        List<Payment> result = service.payments()

        then:
        result.size() == 1
    }

    def 'paymentInsert throws when source account not found'() {
        given:
        Payment payment = buildPayment()
        accountRepository.account('checking_brian') >> null

        when:
        service.paymentInsert(payment)

        then:
        RuntimeException ex = thrown()
        ex.message.contains('checking_brian')
    }

    def 'paymentInsert throws when destination account not found'() {
        given:
        Payment payment = buildPayment()
        accountRepository.account('checking_brian') >> new Account(accountNameOwner: 'checking_brian', accountType: AccountType.debit)
        accountRepository.account('chase_brian') >> null

        when:
        service.paymentInsert(payment)

        then:
        RuntimeException ex = thrown()
        ex.message.contains('chase_brian')
    }

    def 'paymentInsert creates two transactions and inserts payment'() {
        given:
        Payment payment = buildPayment()
        Account source = new Account(accountNameOwner: 'checking_brian', accountType: AccountType.debit)
        Account dest = new Account(accountNameOwner: 'chase_brian', accountType: AccountType.credit)
        accountRepository.account('checking_brian') >> source
        accountRepository.account('chase_brian') >> dest
        Transaction sourceTx = new Transaction(guid: 'src-guid')
        Transaction destTx = new Transaction(guid: 'dst-guid')
        transactionService.transactionInsert(_) >>> [sourceTx, destTx]
        paymentRepository.paymentInsert(_) >> true

        when:
        Payment result = service.paymentInsert(payment)

        then:
        result.guidSource == 'src-guid'
        result.guidDestination == 'dst-guid'
    }

    def 'paymentDelete returns false when payment not found'() {
        given:
        paymentRepository.payment(99L) >> null

        expect:
        !service.paymentDelete(99L)
    }

    def 'paymentDelete returns true when payment exists'() {
        given:
        paymentRepository.payment(1L) >> buildPayment()
        paymentRepository.paymentDelete(1L) >> true

        expect:
        service.paymentDelete(1L)
    }

    def 'paymentUpdate throws when payment not found'() {
        given:
        Payment payment = buildPayment()
        payment.paymentId = 99L
        paymentRepository.payment(99L) >> null

        when:
        service.paymentUpdate(payment)

        then:
        thrown(RuntimeException)
    }

    def 'source amount is negated for debit-to-credit bill payment'() {
        given:
        Payment payment = buildPayment()
        Account source = new Account(accountNameOwner: 'checking_brian', accountType: AccountType.debit)
        Account dest = new Account(accountNameOwner: 'chase_brian', accountType: AccountType.credit)
        accountRepository.account('checking_brian') >> source
        accountRepository.account('chase_brian') >> dest
        List<Transaction> capturedTransactions = []
        transactionService.transactionInsert(_) >> { Transaction t -> capturedTransactions << t; t }
        paymentRepository.paymentInsert(_) >> true

        when:
        service.paymentInsert(payment)

        then:
        capturedTransactions[0].amount < 0  // source deducted
        capturedTransactions[1].amount < 0  // credit balance reduced
    }

    def 'both amounts are positive for debit-to-debit transfer'() {
        given:
        Payment payment = buildPayment()
        payment.destinationAccount = 'savings_brian'
        Account source = new Account(accountNameOwner: 'checking_brian', accountType: AccountType.debit)
        Account dest = new Account(accountNameOwner: 'savings_brian', accountType: AccountType.debit)
        accountRepository.account('checking_brian') >> source
        accountRepository.account('savings_brian') >> dest
        List<Transaction> capturedTransactions = []
        transactionService.transactionInsert(_) >> { Transaction t -> capturedTransactions << t; t }
        paymentRepository.paymentInsert(_) >> true

        when:
        service.paymentInsert(payment)

        then:
        capturedTransactions[0].amount < 0  // source deducted
        capturedTransactions[1].amount > 0  // destination credited
    }

    def 'paymentInsert throws for credit-to-credit balance transfer - source positive, destination negative'() {
        given:
        Payment payment = buildPayment()
        payment.sourceAccount = 'amex_brian'
        payment.destinationAccount = 'visa_brian'
        Account source = new Account(accountNameOwner: 'amex_brian', accountType: AccountType.credit)
        Account dest = new Account(accountNameOwner: 'visa_brian', accountType: AccountType.credit)
        accountRepository.account('amex_brian') >> source
        accountRepository.account('visa_brian') >> dest
        List<Transaction> capturedTransactions = []
        transactionService.transactionInsert(_) >> { Transaction t -> capturedTransactions << t; t }
        paymentRepository.paymentInsert(_) >> true

        when:
        service.paymentInsert(payment)

        then:
        capturedTransactions[0].amount > 0  // balance transfer: source credit card charged positively
        capturedTransactions[1].amount < 0  // destination credit card balance reduced
    }

    def 'paymentInsert credit-to-debit cash advance - both amounts positive'() {
        given:
        Payment payment = buildPayment()
        payment.sourceAccount = 'amex_brian'
        payment.destinationAccount = 'checking_brian'
        Account source = new Account(accountNameOwner: 'amex_brian', accountType: AccountType.credit)
        Account dest = new Account(accountNameOwner: 'checking_brian', accountType: AccountType.debit)
        accountRepository.account('amex_brian') >> source
        accountRepository.account('checking_brian') >> dest
        List<Transaction> capturedTransactions = []
        transactionService.transactionInsert(_) >> { Transaction t -> capturedTransactions << t; t }
        paymentRepository.paymentInsert(_) >> true

        when:
        service.paymentInsert(payment)

        then:
        capturedTransactions[0].amount > 0  // cash advance: credit card charged positively
        capturedTransactions[1].amount > 0  // cash deposited to debit account
    }

    def 'paymentInsert sets correct description, category, and notes on transactions'() {
        given:
        Payment payment = buildPayment()
        Account source = new Account(accountNameOwner: 'checking_brian', accountType: AccountType.debit)
        Account dest = new Account(accountNameOwner: 'chase_brian', accountType: AccountType.credit)
        accountRepository.account('checking_brian') >> source
        accountRepository.account('chase_brian') >> dest
        List<Transaction> captured = []
        transactionService.transactionInsert(_) >> { Transaction t -> captured << t; t }
        paymentRepository.paymentInsert(_) >> true

        when:
        service.paymentInsert(payment)

        then:
        captured[0].description == 'payment'
        captured[0].category == 'bill_pay'
        captured[0].notes == 'to chase_brian'
        captured[1].description == 'payment'
        captured[1].category == 'bill_pay'
        captured[1].notes == 'from checking_brian'
    }

    def 'paymentInsert assigns correct account to each transaction'() {
        given:
        Payment payment = buildPayment()
        Account source = new Account(accountNameOwner: 'checking_brian', accountType: AccountType.debit)
        Account dest = new Account(accountNameOwner: 'chase_brian', accountType: AccountType.credit)
        accountRepository.account('checking_brian') >> source
        accountRepository.account('chase_brian') >> dest
        List<Transaction> captured = []
        transactionService.transactionInsert(_) >> { Transaction t -> captured << t; t }
        paymentRepository.paymentInsert(_) >> true

        when:
        service.paymentInsert(payment)

        then:
        captured[0].accountNameOwner == 'checking_brian'
        captured[1].accountNameOwner == 'chase_brian'
    }

    def 'paymentInsert throws when amount is negative'() {
        given:
        Payment payment = buildPayment()
        payment.amount = -500.00

        when:
        service.paymentInsert(payment)

        then:
        IllegalArgumentException ex = thrown()
        ex.message.contains('positive')
    }

    def 'paymentInsert throws when amount is zero'() {
        given:
        Payment payment = buildPayment()
        payment.amount = BigDecimal.ZERO

        when:
        service.paymentInsert(payment)

        then:
        IllegalArgumentException ex = thrown()
        ex.message.contains('positive')
    }

    def 'paymentInsert throws when amount is null'() {
        given:
        Payment payment = buildPayment()
        payment.amount = null

        when:
        service.paymentInsert(payment)

        then:
        IllegalArgumentException ex = thrown()
        ex.message.contains('positive')
    }

    def 'paymentInsert throws when source and destination accounts are the same'() {
        given:
        Payment payment = buildPayment()
        payment.destinationAccount = payment.sourceAccount

        when:
        service.paymentInsert(payment)

        then:
        IllegalArgumentException ex = thrown()
        ex.message.contains('different')
    }

    def 'paymentDelete cascades to linked transactions when GUIDs are set'() {
        given:
        Payment payment = buildPayment()
        payment.guidSource = 'src-guid'
        payment.guidDestination = 'dst-guid'
        paymentRepository.payment(1L) >> payment
        paymentRepository.paymentDelete(1L) >> true

        when:
        boolean result = service.paymentDelete(1L)

        then:
        result
        1 * transactionService.deleteTransactionCascade('src-guid')
        1 * transactionService.deleteTransactionCascade('dst-guid')
    }

    def 'paymentDelete still succeeds when linked transactions are already gone (stale GUIDs)'() {
        given:
        Payment payment = buildPayment()
        payment.guidSource = 'stale-src'
        payment.guidDestination = 'stale-dst'
        paymentRepository.payment(1L) >> payment
        paymentRepository.paymentDelete(1L) >> true
        transactionService.deleteTransactionCascade(_) >> false  // not found

        when:
        boolean result = service.paymentDelete(1L)

        then:
        result
    }

    def 'paymentDelete does not call cascade delete when GUIDs are null'() {
        given:
        paymentRepository.payment(1L) >> buildPayment()  // no GUIDs set
        paymentRepository.paymentDelete(1L) >> true

        when:
        service.paymentDelete(1L)

        then:
        0 * transactionService.deleteTransactionCascade(_)
    }

    def 'paymentInsert propagates exception when second transaction insert fails'() {
        given:
        Payment payment = buildPayment()
        Account source = new Account(accountNameOwner: 'checking_brian', accountType: AccountType.debit)
        Account dest = new Account(accountNameOwner: 'chase_brian', accountType: AccountType.credit)
        accountRepository.account('checking_brian') >> source
        accountRepository.account('chase_brian') >> dest
        transactionService.transactionInsert(_) >> new Transaction(guid: 'src-guid') >> { throw new RuntimeException('DB error on destination transaction') }

        when:
        service.paymentInsert(payment)

        then:
        RuntimeException ex = thrown()
        ex.message.contains('DB error')
    }
}
