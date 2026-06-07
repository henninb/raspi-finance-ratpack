package finance.service

import finance.domain.Account
import finance.domain.AccountType
import finance.domain.Transaction
import finance.domain.Transfer
import finance.repositories.AccountRepository
import finance.repositories.TransferRepository
import finance.services.TransactionService
import finance.services.TransferService
import spock.lang.Specification

import java.sql.Date

class TransferServiceSpec extends Specification {

    TransferRepository transferRepository = Mock()
    AccountRepository accountRepository = Mock()
    TransactionService transactionService = Mock()
    TransferService service = new TransferService(transferRepository, accountRepository, transactionService)

    private Transfer buildTransfer() {
        return new Transfer(
                sourceAccount: 'checking_brian',
                destinationAccount: 'savings_brian',
                amount: 200.00,
                transactionDate: new Date(System.currentTimeMillis()),
                owner: 'henninb@gmail.com',
                activeStatus: true
        )
    }

    private Account buildAccount(String name) {
        return new Account(accountNameOwner: name, accountId: 1L, accountType: AccountType.debit, activeStatus: true)
    }

    def 'transfers delegates to repository'() {
        given:
        transferRepository.transfers() >> [buildTransfer()]

        when:
        List<Transfer> result = service.transfers()

        then:
        result.size() == 1
    }

    def 'transferInsert throws when source account not found'() {
        given:
        accountRepository.account('checking_brian') >> null

        when:
        service.transferInsert(buildTransfer())

        then:
        RuntimeException ex = thrown()
        ex.message.contains('checking_brian')
    }

    def 'transferInsert throws when destination account not found'() {
        given:
        accountRepository.account('checking_brian') >> buildAccount('checking_brian')
        accountRepository.account('savings_brian') >> null

        when:
        service.transferInsert(buildTransfer())

        then:
        RuntimeException ex = thrown()
        ex.message.contains('savings_brian')
    }

    def 'transferInsert creates withdrawal and deposit transactions'() {
        given:
        Transfer transfer = buildTransfer()
        accountRepository.account('checking_brian') >> buildAccount('checking_brian')
        accountRepository.account('savings_brian') >> buildAccount('savings_brian')
        Transaction srcTx = new Transaction(guid: 'src-guid')
        Transaction dstTx = new Transaction(guid: 'dst-guid')
        transactionService.transactionInsert(_) >>> [srcTx, dstTx]
        transferRepository.transferInsert(_) >> true

        when:
        Transfer result = service.transferInsert(transfer)

        then:
        result.guidSource == 'src-guid'
        result.guidDestination == 'dst-guid'
    }

    def 'transferInsert sets source transaction amount as negative'() {
        given:
        Transfer transfer = buildTransfer()
        accountRepository.account('checking_brian') >> buildAccount('checking_brian')
        accountRepository.account('savings_brian') >> buildAccount('savings_brian')
        List<Transaction> captured = []
        transactionService.transactionInsert(_) >> { Transaction t -> captured << t; t }

        when:
        service.transferInsert(transfer)

        then:
        captured[0].amount == -200.00
        captured[1].amount == 200.00
    }

    def 'transferDelete returns false when transfer not found'() {
        given:
        transferRepository.transfer(99L) >> null

        expect:
        !service.transferDelete(99L)
    }

    def 'transferDelete returns true when transfer exists'() {
        given:
        transferRepository.transfer(1L) >> buildTransfer()
        transferRepository.transferDelete(1L) >> true

        expect:
        service.transferDelete(1L)
    }

    def 'transferUpdate throws when transfer not found'() {
        given:
        Transfer t = buildTransfer()
        t.transferId = 99L
        transferRepository.transfer(99L) >> null

        when:
        service.transferUpdate(t)

        then:
        thrown(RuntimeException)
    }
}
