package finance.repositories

import com.google.inject.Inject
import finance.domain.Transaction
import groovy.util.logging.Slf4j
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource
import java.time.LocalDate

import static org.jooq.generated.Tables.T_TRANSACTION

@Slf4j
class TransactionRepository {
    private final DSLContext dslContext

    @Inject
    TransactionRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    boolean transactionInsert(Transaction transaction) {
        dslContext.insertInto(T_TRANSACTION)
                .set(T_TRANSACTION.OWNER, transaction.owner ?: "")
                .set(T_TRANSACTION.GUID, (String) transaction.guid)
                .set(T_TRANSACTION.ACCOUNT_ID, (Long) transaction.accountId)
                .set(T_TRANSACTION.ACCOUNT_NAME_OWNER, (String) transaction.accountNameOwner)
                .set(T_TRANSACTION.ACCOUNT_TYPE, (String) transaction.accountType?.name()?.toLowerCase())
                .set(T_TRANSACTION.TRANSACTION_STATE, (String) transaction.transactionState?.name()?.toLowerCase())
                .set(T_TRANSACTION.TRANSACTION_DATE, (java.time.LocalDate) transaction.transactionDate?.toLocalDate())
                .set(T_TRANSACTION.DUE_DATE, (java.time.LocalDate) transaction.dueDate?.toLocalDate())
                .set(T_TRANSACTION.AMOUNT, (BigDecimal) transaction.amount)
                .set(T_TRANSACTION.CATEGORY, (String) transaction.category)
                .set(T_TRANSACTION.DESCRIPTION, (String) transaction.description)
                .set(T_TRANSACTION.NOTES, transaction.notes ?: "")
                .set(T_TRANSACTION.ACTIVE_STATUS, (Boolean) transaction.activeStatus)
                .set(T_TRANSACTION.TRANSACTION_TYPE, (String) transaction.transactionType?.name()?.toLowerCase())
                .set(T_TRANSACTION.REOCCURRING_TYPE, (String) transaction.reoccurringType?.name()?.toLowerCase())
                .execute()
        return true
    }

    boolean transactionUpdate(Transaction transaction) {
        dslContext.update(T_TRANSACTION)
                .set(T_TRANSACTION.DESCRIPTION, (String) transaction.description)
                .set(T_TRANSACTION.CATEGORY, (String) transaction.category)
                .set(T_TRANSACTION.AMOUNT, (BigDecimal) transaction.amount)
                .set(T_TRANSACTION.TRANSACTION_STATE, (String) transaction.transactionState?.name()?.toLowerCase())
                .set(T_TRANSACTION.NOTES, transaction.notes ?: "")
                .where(T_TRANSACTION.GUID.eq(transaction.guid))
                .execute()
        return true
    }

    boolean updateTransferLinkedTransaction(String guid, BigDecimal amount, java.sql.Date transactionDate) {
        dslContext.update(T_TRANSACTION)
                .set(T_TRANSACTION.AMOUNT, amount)
                .set(T_TRANSACTION.TRANSACTION_DATE, (java.time.LocalDate) transactionDate?.toLocalDate())
                .where(T_TRANSACTION.GUID.eq(guid))
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
                .where(T_TRANSACTION.ACCOUNT_NAME_OWNER.equal(accountNameOwner)
                        .and(T_TRANSACTION.ACTIVE_STATUS.eq(true)))
                .orderBy(T_TRANSACTION.TRANSACTION_DATE.desc())
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

    List<Transaction> transactionsByDateRange(LocalDate startDate, LocalDate endDate) {
        return dslContext.selectFrom(T_TRANSACTION)
                .where(T_TRANSACTION.TRANSACTION_DATE.between(startDate, endDate)
                        .and(T_TRANSACTION.ACTIVE_STATUS.eq(true)))
                .orderBy(T_TRANSACTION.TRANSACTION_DATE.desc())
                .fetchInto(Transaction)
    }

    boolean changeAccountNameOwner(String guid, String accountNameOwner, long accountId) {
        dslContext.update(T_TRANSACTION)
                .set(T_TRANSACTION.ACCOUNT_NAME_OWNER, accountNameOwner)
                .set(T_TRANSACTION.ACCOUNT_ID, accountId)
                .where(T_TRANSACTION.GUID.eq(guid))
                .execute()
        return true
    }

    BigDecimal sumSpendingInWindow(String accountNameOwner, LocalDate startDate, LocalDate endDate, String transactionState) {
        return dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), BigDecimal.ZERO))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACCOUNT_NAME_OWNER.eq(accountNameOwner)
                        .and(T_TRANSACTION.TRANSACTION_DATE.between(startDate, endDate))
                        .and(T_TRANSACTION.TRANSACTION_STATE.eq(transactionState))
                        .and(T_TRANSACTION.ACTIVE_STATUS.eq(true)))
                .fetchOneInto(BigDecimal) ?: BigDecimal.ZERO
    }

    BigDecimal sumPendingSpendingInWindow(String accountNameOwner, LocalDate startDate, LocalDate endDate, List<String> transactionStates) {
        return dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), BigDecimal.ZERO))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACCOUNT_NAME_OWNER.eq(accountNameOwner)
                        .and(T_TRANSACTION.TRANSACTION_DATE.between(startDate, endDate))
                        .and(T_TRANSACTION.TRANSACTION_STATE.in(transactionStates))
                        .and(T_TRANSACTION.ACTIVE_STATUS.eq(true)))
                .fetchOneInto(BigDecimal) ?: BigDecimal.ZERO
    }

    List<Transaction> transactionsFiltered(
            String accountNameOwner,
            int page,
            int size,
            String search,
            List<String> states,
            List<String> transactionTypes,
            List<String> reoccurringTypes,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        List<Condition> conditions = buildFilterConditions(accountNameOwner, search, states, transactionTypes, reoccurringTypes, startDate, endDate, minAmount, maxAmount)
        return dslContext.selectFrom(T_TRANSACTION)
                .where(DSL.and(conditions))
                .orderBy(T_TRANSACTION.TRANSACTION_DATE.desc())
                .limit(size)
                .offset(page * size)
                .fetchInto(Transaction)
    }

    int countTransactionsFiltered(
            String accountNameOwner,
            String search,
            List<String> states,
            List<String> transactionTypes,
            List<String> reoccurringTypes,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        List<Condition> conditions = buildFilterConditions(accountNameOwner, search, states, transactionTypes, reoccurringTypes, startDate, endDate, minAmount, maxAmount)
        return dslContext.fetchCount(
            dslContext.selectFrom(T_TRANSACTION).where(DSL.and(conditions))
        )
    }

    private List<Condition> buildFilterConditions(
            String accountNameOwner,
            String search,
            List<String> states,
            List<String> transactionTypes,
            List<String> reoccurringTypes,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        List<Condition> conditions = [
            T_TRANSACTION.ACCOUNT_NAME_OWNER.eq(accountNameOwner),
            T_TRANSACTION.ACTIVE_STATUS.eq(true)
        ]
        if (search) {
            String term = "%${search.toLowerCase()}%"
            conditions.add(
                DSL.lower(T_TRANSACTION.DESCRIPTION).like(term)
                    .or(DSL.lower(T_TRANSACTION.CATEGORY).like(term))
                    .or(DSL.lower(T_TRANSACTION.NOTES).like(term))
            )
        }
        if (states) {
            conditions.add(T_TRANSACTION.TRANSACTION_STATE.in(states))
        }
        if (transactionTypes) {
            conditions.add(T_TRANSACTION.TRANSACTION_TYPE.in(transactionTypes))
        }
        if (reoccurringTypes) {
            conditions.add(T_TRANSACTION.REOCCURRING_TYPE.in(reoccurringTypes))
        }
        if (startDate) {
            conditions.add(T_TRANSACTION.TRANSACTION_DATE.ge(startDate))
        }
        if (endDate) {
            conditions.add(T_TRANSACTION.TRANSACTION_DATE.le(endDate))
        }
        if (minAmount != null) {
            conditions.add(T_TRANSACTION.AMOUNT.ge(minAmount))
        }
        if (maxAmount != null) {
            conditions.add(T_TRANSACTION.AMOUNT.le(maxAmount))
        }
        return conditions
    }
}
