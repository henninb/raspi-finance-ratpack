package finance.services

import finance.domain.PendingTransaction
import finance.repositories.PendingTransactionRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

@Log
@CompileStatic
class PendingTransactionService implements Service {

    private PendingTransactionRepository pendingTransactionRepository

    @Inject
    PendingTransactionService(PendingTransactionRepository pendingTransactionRepository) {
        this.pendingTransactionRepository = pendingTransactionRepository
    }

    List<PendingTransaction> pendingTransactions() {
        return pendingTransactionRepository.pendingTransactions()
    }

    PendingTransaction pendingTransaction(Long pendingTransactionId) {
        return pendingTransactionRepository.pendingTransaction(pendingTransactionId)
    }

    PendingTransaction pendingTransactionInsert(PendingTransaction pendingTransaction) {
        pendingTransaction.dateAdded = new Timestamp(System.currentTimeMillis())
        pendingTransactionRepository.pendingTransactionInsert(pendingTransaction)
        log.info("inserted pending transaction accountNameOwner=${pendingTransaction.accountNameOwner}")
        return pendingTransaction
    }

    PendingTransaction pendingTransactionUpdate(PendingTransaction pendingTransaction) {
        PendingTransaction existing = pendingTransactionRepository.pendingTransaction(pendingTransaction.pendingTransactionId)
        if (!existing) {
            throw new RuntimeException("pending transaction not found: ${pendingTransaction.pendingTransactionId}")
        }
        pendingTransactionRepository.pendingTransactionUpdate(pendingTransaction)
        return pendingTransactionRepository.pendingTransaction(pendingTransaction.pendingTransactionId)
    }

    boolean pendingTransactionDelete(Long pendingTransactionId) {
        PendingTransaction existing = pendingTransactionRepository.pendingTransaction(pendingTransactionId)
        if (!existing) {
            return false
        }
        return pendingTransactionRepository.pendingTransactionDelete(pendingTransactionId)
    }

    boolean pendingTransactionDeleteAll() {
        return pendingTransactionRepository.pendingTransactionDeleteAll()
    }
}
