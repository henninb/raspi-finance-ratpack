package finance.services

import finance.domain.Transfer
import finance.repositories.TransferRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

@Log
@CompileStatic
class TransferService implements Service {

    private TransferRepository transferRepository

    @Inject
    TransferService(TransferRepository transferRepository) {
        this.transferRepository = transferRepository
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
        transfer.guidSource = UUID.randomUUID().toString()
        transfer.guidDestination = UUID.randomUUID().toString()
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
