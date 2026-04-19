package finance.repositories

import com.google.inject.Inject
import finance.domain.Transaction
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_TRANSACTION

@Log
class TransactionRepository {
    private final DSLContext dslContext

    @Inject
    TransactionRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    boolean transactionInsert(Transaction transaction) {
        dslContext.newRecord(T_TRANSACTION, transaction).store()
        return true
    }

    boolean transactionUpdate(Transaction transaction) {
        dslContext.update(T_TRANSACTION)
                .set(T_TRANSACTION.DESCRIPTION, transaction.description)
                .set(T_TRANSACTION.CATEGORY, transaction.category)
                .set(T_TRANSACTION.AMOUNT, transaction.amount)
                .set(T_TRANSACTION.TRANSACTION_STATE, transaction.transactionState?.name()?.toLowerCase())
                .set(T_TRANSACTION.NOTES, transaction.notes ?: "")
                .where(T_TRANSACTION.GUID.eq(transaction.guid))
                .execute()
        return true
    }

    boolean transactionStateUpdate(String guid, String transactionState) {
        dslContext.update(T_TRANSACTION)
                .set(T_TRANSACTION.TRANSACTION_STATE, transactionState)
                .where(T_TRANSACTION.GUID.eq(guid))
                .execute()
        return true
    }

    List<Transaction> transactionsAll() {
        return dslContext.selectFrom(T_TRANSACTION).where().fetchInto(Transaction)
    }

    List<Transaction> transactions(String accountNameOwner) {
        return dslContext.selectFrom(T_TRANSACTION)
                .where(T_TRANSACTION.ACCOUNT_NAME_OWNER.equal(accountNameOwner))
                .orderBy(T_TRANSACTION.TRANSACTION_STATE.desc(), T_TRANSACTION.TRANSACTION_DATE.desc())
                .fetchInto(Transaction)
    }

    List<Transaction> transactionsByCategory(String categoryName) {
        return dslContext.selectFrom(T_TRANSACTION)
                .where(T_TRANSACTION.CATEGORY.equal(categoryName).and(T_TRANSACTION.ACTIVE_STATUS.eq(true)))
                .orderBy(T_TRANSACTION.TRANSACTION_DATE.desc())
                .fetchInto(Transaction)
    }

    List<Transaction> transactionsByDescription(String descriptionName) {
        return dslContext.selectFrom(T_TRANSACTION)
                .where(T_TRANSACTION.DESCRIPTION.equal(descriptionName).and(T_TRANSACTION.ACTIVE_STATUS.eq(true)))
                .orderBy(T_TRANSACTION.TRANSACTION_DATE.desc())
                .fetchInto(Transaction)
    }

    Transaction transaction(String guid) {
        return dslContext.selectFrom(T_TRANSACTION)
                .where(T_TRANSACTION.GUID.equal(guid))
                .fetchOneInto(Transaction)
    }

    boolean transactionDelete(String guid) {
        dslContext.delete(T_TRANSACTION)
                .where(T_TRANSACTION.GUID.equal(guid))
                .execute()
        return true
    }
}
