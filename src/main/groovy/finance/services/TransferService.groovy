package finance.services

import finance.domain.Account
import finance.domain.ReoccurringType
import finance.domain.Transaction
import finance.domain.TransactionState
import finance.domain.TransactionType
import finance.domain.Transfer
import finance.repositories.AccountRepository
import finance.repositories.TransferRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Date
import java.sql.Timestamp

@Slf4j
@CompileStatic
class TransferService implements Service {

    private TransferRepository transferRepository
    private AccountRepository accountRepository
    private TransactionService transactionService

    @Inject
    TransferService(TransferRepository transferRepository, AccountRepository accountRepository, TransactionService transactionService) {
        this.transferRepository = transferRepository
        this.accountRepository = accountRepository
        this.transactionService = transactionService
    }

    List<Transfer> transfers() {
        return transferRepository.transfers()
    }

    Transfer transfer(Long transferId) {
        return transferRepository.transfer(transferId)
    }

    Transfer transferInsert(Transfer transfer) {
        if (transfer.sourceAccount == transfer.destinationAccount) {
            throw new RuntimeException("Source and destination accounts must be different")
        }
        if (!transfer.amount || transfer.amount <= BigDecimal.ZERO) {
            throw new RuntimeException("Transfer amount must be positive")
        }

        transfer.dateUpdated = new Timestamp(System.currentTimeMillis())
        transfer.dateAdded = new Timestamp(System.currentTimeMillis())

        Account sourceAccount = accountRepository.account(transfer.sourceAccount)
        if (!sourceAccount) {
            throw new RuntimeException("Source account not found: ${transfer.sourceAccount}")
        }
        Account destinationAccount = accountRepository.account(transfer.destinationAccount)
        if (!destinationAccount) {
            throw new RuntimeException("Destination account not found: ${transfer.destinationAccount}")
        }

        if (transferRepository.existsByTransfer(transfer.sourceAccount, transfer.destinationAccount, transfer.amount, transfer.transactionDate)) {
            throw new RuntimeException("A transfer with the same source, destination, amount, and date already exists")
        }

        Timestamp now = new Timestamp(System.currentTimeMillis())

        Transaction sourceTransaction = new Transaction()
        sourceTransaction.guid = UUID.randomUUID().toString()
        sourceTransaction.transactionDate = transfer.transactionDate
        sourceTransaction.description = "transfer withdrawal"
        sourceTransaction.category = "transfer"
        sourceTransaction.notes = "transfer to ${transfer.destinationAccount}"
        sourceTransaction.amount = transfer.amount.negate()
        sourceTransaction.transactionState = TransactionState.outstanding
        sourceTransaction.reoccurringType = ReoccurringType.onetime
        sourceTransaction.transactionType = TransactionType.transfer
        sourceTransaction.accountNameOwner = transfer.sourceAccount
        sourceTransaction.owner = transfer.owner
        sourceTransaction.activeStatus = true
        sourceTransaction.dateUpdated = now
        sourceTransaction.dateAdded = now

        Transaction insertedSource = transactionService.transactionInsert(sourceTransaction)
        transfer.guidSource = insertedSource.guid

        Transaction destinationTransaction = new Transaction()
        destinationTransaction.guid = UUID.randomUUID().toString()
        destinationTransaction.transactionDate = transfer.transactionDate
        destinationTransaction.description = "transfer deposit"
        destinationTransaction.category = "transfer"
        destinationTransaction.notes = "transfer from ${transfer.sourceAccount}"
        destinationTransaction.amount = transfer.amount
        destinationTransaction.transactionState = TransactionState.outstanding
        destinationTransaction.reoccurringType = ReoccurringType.onetime
        destinationTransaction.transactionType = TransactionType.transfer
        destinationTransaction.accountNameOwner = transfer.destinationAccount
        destinationTransaction.owner = transfer.owner
        destinationTransaction.activeStatus = true
        destinationTransaction.dateUpdated = now
        destinationTransaction.dateAdded = now

        Transaction insertedDestination
        try {
            insertedDestination = transactionService.transactionInsert(destinationTransaction)
        } catch (Exception e) {
            transactionService.deleteTransactionCascade(insertedSource.guid)
            throw new RuntimeException("Transfer insert failed; source transaction rolled back: ${e.message}", e)
        }
        transfer.guidDestination = insertedDestination.guid

        try {
            transferRepository.transferInsert(transfer)
        } catch (Exception e) {
            transactionService.deleteTransactionCascade(insertedSource.guid)
            transactionService.deleteTransactionCascade(insertedDestination.guid)
            throw new RuntimeException("Transfer record insert failed; transactions rolled back: ${e.message}", e)
        }

        log.info("inserted transfer sourceAccount=${transfer.sourceAccount} destinationAccount=${transfer.destinationAccount}")
        return transfer
    }

    Transfer transferUpdate(Transfer transfer) {
        Transfer existing = transferRepository.transfer(transfer.transferId)
        if (!existing) {
            throw new RuntimeException("transfer not found: ${transfer.transferId}")
        }
        transferRepository.transferUpdate(transfer)
        if (existing.guidSource) {
            transactionService.updateTransferLinkedTransaction(existing.guidSource, transfer.amount.negate(), transfer.transactionDate)
        }
        if (existing.guidDestination) {
            transactionService.updateTransferLinkedTransaction(existing.guidDestination, transfer.amount, transfer.transactionDate)
        }
        return transferRepository.transfer(transfer.transferId)
    }

    boolean transferDelete(Long transferId) {
        Transfer existing = transferRepository.transfer(transferId)
        if (!existing) {
            return false
        }
        String guidSource = existing.guidSource
        String guidDestination = existing.guidDestination

        // Delete the transfer first to release FK references to t_transaction
        boolean deleted = transferRepository.transferDelete(transferId)

        // Cascade-delete linked transactions; each is isolated so one failure doesn't block the other
        if (guidSource) {
            try {
                transactionService.deleteTransactionCascade(guidSource)
                log.info("Deleted source transaction: ${guidSource}")
            } catch (Exception e) {
                log.warn("Failed to delete source transaction ${guidSource}: ${e.message}")
            }
        }
        if (guidDestination) {
            try {
                transactionService.deleteTransactionCascade(guidDestination)
                log.info("Deleted destination transaction: ${guidDestination}")
            } catch (Exception e) {
                log.warn("Failed to delete destination transaction ${guidDestination}: ${e.message}")
            }
        }
        accountRepository.updateTotalsForAllAccounts()
        return deleted
    }
}
