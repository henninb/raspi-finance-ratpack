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
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Date
import java.sql.Timestamp

@Log
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

        Transaction insertedDestination = transactionService.transactionInsert(destinationTransaction)
        transfer.guidDestination = insertedDestination.guid

        transferRepository.transferInsert(transfer)
        log.info("inserted transfer sourceAccount=${transfer.sourceAccount} destinationAccount=${transfer.destinationAccount}")
        return transfer
    }

    Transfer transferUpdate(Transfer transfer) {
        Transfer existing = transferRepository.transfer(transfer.transferId)
        if (!existing) {
            throw new RuntimeException("transfer not found: ${transfer.transferId}")
        }
        transferRepository.transferUpdate(transfer)
        return transferRepository.transfer(transfer.transferId)
    }

    boolean transferDelete(Long transferId) {
        Transfer existing = transferRepository.transfer(transferId)
        if (!existing) {
            return false
        }
        return transferRepository.transferDelete(transferId)
    }
}
