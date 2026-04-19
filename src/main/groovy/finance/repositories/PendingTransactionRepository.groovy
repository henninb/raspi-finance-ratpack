package finance.repositories

import com.google.inject.Inject
import finance.domain.PendingTransaction
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_PENDING_TRANSACTION

@Log
class PendingTransactionRepository {
    private final DSLContext dslContext

    @Inject
    PendingTransactionRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    boolean pendingTransactionInsert(PendingTransaction pendingTransaction) {
        dslContext.newRecord(T_PENDING_TRANSACTION, pendingTransaction).store()
        return true
    }

    boolean pendingTransactionUpdate(PendingTransaction pendingTransaction) {
        dslContext.update(T_PENDING_TRANSACTION)
                .set(T_PENDING_TRANSACTION.DESCRIPTION, pendingTransaction.description)
                .set(T_PENDING_TRANSACTION.AMOUNT, pendingTransaction.amount)
                .set(T_PENDING_TRANSACTION.REVIEW_STATUS, pendingTransaction.reviewStatus)
                .where(T_PENDING_TRANSACTION.PENDING_TRANSACTION_ID.eq(pendingTransaction.pendingTransactionId))
                .execute()
        return true
    }

    List<PendingTransaction> pendingTransactions() {
        return dslContext.selectFrom(T_PENDING_TRANSACTION)
                .orderBy(T_PENDING_TRANSACTION.TRANSACTION_DATE.desc())
                .fetchInto(PendingTransaction)
    }

    PendingTransaction pendingTransaction(Long pendingTransactionId) {
        return dslContext.selectFrom(T_PENDING_TRANSACTION)
                .where(T_PENDING_TRANSACTION.PENDING_TRANSACTION_ID.equal(pendingTransactionId))
                .fetchOneInto(PendingTransaction)
    }

    boolean pendingTransactionDelete(Long pendingTransactionId) {
        dslContext.delete(T_PENDING_TRANSACTION)
                .where(T_PENDING_TRANSACTION.PENDING_TRANSACTION_ID.equal(pendingTransactionId))
                .execute()
        return true
    }

    boolean pendingTransactionDeleteAll() {
        dslContext.delete(T_PENDING_TRANSACTION).execute()
        return true
    }
}
