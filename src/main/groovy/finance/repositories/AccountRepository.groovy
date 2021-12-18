package finance.repositories

import com.google.inject.Inject
import finance.domain.Account
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import ratpack.exec.Blocking
import ratpack.exec.Operation
import javax.sql.DataSource

import static org.jooq.generated.Tables.T_ACCOUNT

@Log
@CompileStatic
class AccountRepository {
    private final DSLContext dslContext

    @Inject
    AccountRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    Operation insertAccount(Account account) {
        return Blocking.op({ -> dslContext.newRecord(T_ACCOUNT, account).store() })
    }

    List<Account> accounts() {
        return dslContext.selectFrom(T_ACCOUNT).where().orderBy(T_ACCOUNT.ACCOUNT_NAME_OWNER).fetchInto(Account)
    }

    Account findByAccountNameOwner(String accountName) {
        return dslContext.selectFrom(T_ACCOUNT).where(T_ACCOUNT.ACCOUNT_NAME_OWNER.equal(accountName)).fetchOneInto(Account)
    }

}






