package finance.repositories

import com.google.inject.Inject
import finance.domain.Account
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_ACCOUNT

@Log
class AccountRepository {
    private final DSLContext dslContext

    @Inject
    AccountRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    boolean accountInsert(Account account) {
        dslContext.newRecord(T_ACCOUNT, account).store()
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
                .where(T_ACCOUNT.ACTIVE_STATUS.eq(true).and(T_ACCOUNT.PAYMENT_REQUIRED.eq(true)))
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
}
