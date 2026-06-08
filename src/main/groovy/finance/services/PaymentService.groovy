package finance.services

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Payment
import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.repositories.AccountRepository
import finance.repositories.PaymentRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

@Slf4j
@CompileStatic
class PaymentService implements Service {

    private PaymentRepository paymentRepository
    private AccountRepository accountRepository
    private TransactionService transactionService

    @Inject
    PaymentService(PaymentRepository paymentRepository, AccountRepository accountRepository, TransactionService transactionService) {
        this.paymentRepository = paymentRepository
        this.accountRepository = accountRepository
        this.transactionService = transactionService
    }

    List<Payment> payments() {
        return paymentRepository.payments()
    }

    Payment payment(Long paymentId) {
        return paymentRepository.payment(paymentId)
    }

    Payment paymentInsert(Payment payment) {
        if (!payment.amount || payment.amount <= BigDecimal.ZERO) {
            throw new IllegalArgumentException("Payment amount must be positive: ${payment.amount}")
        }
        if (payment.sourceAccount == payment.destinationAccount) {
            throw new IllegalArgumentException("Source and destination accounts must be different: ${payment.sourceAccount}")
        }

        Timestamp now = new Timestamp(System.currentTimeMillis())
        payment.dateUpdated = now
        payment.dateAdded = now

        Account sourceAccount = accountRepository.account(payment.sourceAccount)
        if (!sourceAccount) {
            throw new RuntimeException("Source account not found: ${payment.sourceAccount}")
        }
        Account destinationAccount = accountRepository.account(payment.destinationAccount)
        if (!destinationAccount) {
            throw new RuntimeException("Destination account not found: ${payment.destinationAccount}")
        }

        BigDecimal sourceAmount = calculateSourceAmount(payment.amount, sourceAccount.accountType, destinationAccount.accountType)
        BigDecimal destinationAmount = calculateDestinationAmount(payment.amount, sourceAccount.accountType, destinationAccount.accountType)

        Transaction sourceTransaction = new Transaction()
        sourceTransaction.guid = UUID.randomUUID().toString()
        sourceTransaction.transactionDate = payment.transactionDate
        sourceTransaction.description = "payment"
        sourceTransaction.category = "bill_pay"
        sourceTransaction.notes = "to ${payment.destinationAccount}"
        sourceTransaction.amount = sourceAmount
        sourceTransaction.transactionState = TransactionState.outstanding
        sourceTransaction.reoccurringType = ReoccurringType.onetime
        sourceTransaction.transactionType = TransactionType.undefined
        sourceTransaction.accountNameOwner = payment.sourceAccount
        sourceTransaction.owner = payment.owner
        sourceTransaction.activeStatus = true
        sourceTransaction.dateUpdated = now
        sourceTransaction.dateAdded = now

        Transaction insertedSource = transactionService.transactionInsert(sourceTransaction)
        payment.guidSource = insertedSource.guid

        Transaction destinationTransaction = new Transaction()
        destinationTransaction.guid = UUID.randomUUID().toString()
        destinationTransaction.transactionDate = payment.transactionDate
        destinationTransaction.description = "payment"
        destinationTransaction.category = "bill_pay"
        destinationTransaction.notes = "from ${payment.sourceAccount}"
        destinationTransaction.amount = destinationAmount
        destinationTransaction.transactionState = TransactionState.outstanding
        destinationTransaction.reoccurringType = ReoccurringType.onetime
        destinationTransaction.transactionType = TransactionType.undefined
        destinationTransaction.accountNameOwner = payment.destinationAccount
        destinationTransaction.owner = payment.owner
        destinationTransaction.activeStatus = true
        destinationTransaction.dateUpdated = now
        destinationTransaction.dateAdded = now

        Transaction insertedDestination = transactionService.transactionInsert(destinationTransaction)
        payment.guidDestination = insertedDestination.guid

        paymentRepository.paymentInsert(payment)
        log.info("inserted payment sourceAccount=${payment.sourceAccount} destinationAccount=${payment.destinationAccount}")
        return payment
    }

    Payment paymentUpdate(Payment payment) {
        Payment existing = paymentRepository.payment(payment.paymentId)
        if (!existing) {
            throw new RuntimeException("payment not found: ${payment.paymentId}")
        }
        paymentRepository.paymentUpdate(payment)
        return paymentRepository.payment(payment.paymentId)
    }

    boolean paymentDelete(Long paymentId) {
        Payment existing = paymentRepository.payment(paymentId)
        if (!existing) {
            return false
        }
        String guidSource = existing.guidSource
        String guidDestination = existing.guidDestination

        // Delete the payment first to release FK references to t_transaction
        boolean deleted = paymentRepository.paymentDelete(paymentId)

        // Then cascade-delete the linked transactions
        if (guidSource) {
            transactionService.deleteTransactionCascade(guidSource)
            log.info("Deleted source transaction: ${guidSource}")
        }
        if (guidDestination) {
            transactionService.deleteTransactionCascade(guidDestination)
            log.info("Deleted destination transaction: ${guidDestination}")
        }
        return deleted
    }

    private BigDecimal calculateSourceAmount(BigDecimal amount, AccountType sourceType, AccountType destType) {
        if (sourceType == AccountType.credit && destType == AccountType.debit) {
            // cash advance
            return amount.abs()
        } else if (sourceType == AccountType.credit && destType == AccountType.credit) {
            // balance transfer
            return amount.abs()
        }
        // bill payment or transfer: deduct from source
        return amount.abs().negate()
    }

    private BigDecimal calculateDestinationAmount(BigDecimal amount, AccountType sourceType, AccountType destType) {
        if (sourceType == AccountType.debit && destType == AccountType.debit) {
            // transfer: add to destination
            return amount.abs()
        } else if (sourceType == AccountType.credit && destType == AccountType.debit) {
            // cash advance: add to destination
            return amount.abs()
        }
        // bill payment or balance transfer: payment reduces balance
        return amount.abs().negate()
    }
}
