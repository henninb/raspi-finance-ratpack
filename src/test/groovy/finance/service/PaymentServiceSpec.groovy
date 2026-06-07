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
}
