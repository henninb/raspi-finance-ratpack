package finance.service

import finance.domain.PendingTransaction
import finance.repositories.PendingTransactionRepository
import finance.services.PendingTransactionService
import spock.lang.Specification

class PendingTransactionServiceSpec extends Specification {

    PendingTransactionRepository pendingTransactionRepository = Mock()
    PendingTransactionService service = new PendingTransactionService(pendingTransactionRepository)

    private PendingTransaction buildPendingTransaction(Long id = 1L) {
        return new PendingTransaction(
                pendingTransactionId: id,
                accountNameOwner: 'chase_brian',
                description: 'starbucks',
                amount: 5.50
        )
    }

    def 'pendingTransactions delegates to repository'() {
        given:
        pendingTransactionRepository.pendingTransactions() >> [buildPendingTransaction()]

        when:
        List<PendingTransaction> result = service.pendingTransactions()

        then:
        result.size() == 1
    }

    def 'pendingTransactionInsert sets dateAdded and delegates'() {
        given:
        PendingTransaction pt = buildPendingTransaction()
        pt.dateAdded = null

        when:
        PendingTransaction result = service.pendingTransactionInsert(pt)

        then:
        1 * pendingTransactionRepository.pendingTransactionInsert(pt)
        result.dateAdded != null
    }

    def 'pendingTransactionUpdate throws when not found'() {
        given:
        pendingTransactionRepository.pendingTransaction(99L) >> null

        when:
        service.pendingTransactionUpdate(buildPendingTransaction(99L))

        then:
        thrown(RuntimeException)
    }

    def 'pendingTransactionUpdate delegates and returns refreshed record'() {
        given:
        PendingTransaction existing = buildPendingTransaction()
        PendingTransaction updated = buildPendingTransaction()
        updated.description = 'coffee shop'
        pendingTransactionRepository.pendingTransaction(1L) >>> [existing, updated]

        when:
        PendingTransaction result = service.pendingTransactionUpdate(buildPendingTransaction())

        then:
        1 * pendingTransactionRepository.pendingTransactionUpdate(_)
        result.description == 'coffee shop'
    }

    def 'pendingTransactionDelete returns false when not found'() {
        given:
        pendingTransactionRepository.pendingTransaction(99L) >> null

        expect:
        !service.pendingTransactionDelete(99L)
    }

    def 'pendingTransactionDelete returns true when found'() {
        given:
        pendingTransactionRepository.pendingTransaction(1L) >> buildPendingTransaction()
        pendingTransactionRepository.pendingTransactionDelete(1L) >> true

        expect:
        service.pendingTransactionDelete(1L)
    }

    def 'pendingTransactionDeleteAll delegates to repository'() {
        given:
        pendingTransactionRepository.pendingTransactionDeleteAll() >> true

        expect:
        service.pendingTransactionDeleteAll()
    }
}
