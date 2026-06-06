package finance.repositories

import com.google.inject.Inject
import finance.domain.Account
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_ACCOUNT
import static org.jooq.generated.Tables.T_TRANSACTION

@Log
class AccountRepository {
    private final DSLContext dslContext

    @Inject
    AccountRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    boolean accountInsert(Account account) {
        dslContext.insertInto(T_ACCOUNT)
                .set(T_ACCOUNT.OWNER, account.owner ?: "")
                .set(T_ACCOUNT.ACCOUNT_NAME_OWNER, (String) account.accountNameOwner)
                .set(T_ACCOUNT.ACCOUNT_TYPE, (String) account.accountType?.name()?.toLowerCase())
                .set(T_ACCOUNT.ACTIVE_STATUS, (Boolean) account.activeStatus)
                .set(T_ACCOUNT.MONIKER, account.moniker ?: "0000")
                .execute()
        return true
    }

    boolean accountUpdate(Account account) {
        dslContext.update(T_ACCOUNT)
                .set(T_ACCOUNT.ACCOUNT_TYPE, account.accountType?.name()?.toLowerCase())
                .set(T_ACCOUNT.MONIKER, account.moniker ?: "0000")
                .set(T_ACCOUNT.ACTIVE_STATUS, account.activeStatus)
                .where(T_ACCOUNT.ACCOUNT_NAME_OWNER.eq(account.accountNameOwner))
                .execute()
        return true
    }

    boolean accountRename(String oldAccountNameOwner, String newAccountNameOwner) {
        dslContext.update(T_ACCOUNT)
                .set(T_ACCOUNT.ACCOUNT_NAME_OWNER, newAccountNameOwner)
                .where(T_ACCOUNT.ACCOUNT_NAME_OWNER.eq(oldAccountNameOwner))
                .execute()
        return true
    }

    boolean accountDeactivate(String accountNameOwner) {
        dslContext.update(T_ACCOUNT)
                .set(T_ACCOUNT.ACTIVE_STATUS, false)
                .where(T_ACCOUNT.ACCOUNT_NAME_OWNER.eq(accountNameOwner))
                .execute()
        return true
    }

    boolean accountActivate(String accountNameOwner) {
        dslContext.update(T_ACCOUNT)
                .set(T_ACCOUNT.ACTIVE_STATUS, true)
                .where(T_ACCOUNT.ACCOUNT_NAME_OWNER.eq(accountNameOwner))
                .execute()
        return true
    }

    List<Account> accounts() {
        return dslContext.selectFrom(T_ACCOUNT)
                .where(T_ACCOUNT.ACTIVE_STATUS.eq(true))
                .orderBy(T_ACCOUNT.ACCOUNT_NAME_OWNER)
                .fetchInto(Account)
    }

    List<Account> accountsRequiringPayment() {
        return dslContext.selectFrom(T_ACCOUNT)
                .where(T_ACCOUNT.ACTIVE_STATUS.eq(true)
                    .and(T_ACCOUNT.ACCOUNT_TYPE.eq("credit"))
                    .and(T_ACCOUNT.OUTSTANDING.gt(BigDecimal.ZERO)
                        .or(T_ACCOUNT.FUTURE.gt(BigDecimal.ZERO))
                        .or(T_ACCOUNT.CLEARED.gt(BigDecimal.ZERO))))
                .orderBy(T_ACCOUNT.ACCOUNT_NAME_OWNER)
                .fetchInto(Account)
    }

    Account account(String accountNameOwner) {
        return dslContext.selectFrom(T_ACCOUNT)
                .where(T_ACCOUNT.ACCOUNT_NAME_OWNER.equal(accountNameOwner))
                .fetchOneInto(Account)
    }

    boolean accountDelete(String accountNameOwner) {
        dslContext.delete(T_ACCOUNT)
                .where(T_ACCOUNT.ACCOUNT_NAME_OWNER.equal(accountNameOwner))
                .execute()
        return true
    }

    void updateValidationDates() {
        dslContext.execute("""
            UPDATE t_account a
            SET validation_date = sub.max_validation_date,
                date_updated = now()
            FROM (
                SELECT va.account_id, MAX(va.validation_date) AS max_validation_date
                FROM t_validation_amount va
                WHERE va.active_status = TRUE
                GROUP BY va.account_id
            ) sub
            WHERE a.account_id = sub.account_id
        """)
    }

    BigDecimal sumTransactionsByState(String transactionState) {
        BigDecimal debits = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), BigDecimal.ZERO))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACCOUNT_TYPE.eq("debit")
                        .and(T_TRANSACTION.TRANSACTION_STATE.eq(transactionState))
                        .and(T_TRANSACTION.ACTIVE_STATUS.eq(true)))
                .fetchOneInto(BigDecimal)
        BigDecimal credits = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), BigDecimal.ZERO))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACCOUNT_TYPE.eq("credit")
                        .and(T_TRANSACTION.TRANSACTION_STATE.eq(transactionState))
                        .and(T_TRANSACTION.ACTIVE_STATUS.eq(true)))
                .fetchOneInto(BigDecimal)
        return (debits ?: BigDecimal.ZERO) - (credits ?: BigDecimal.ZERO)
    }
}
