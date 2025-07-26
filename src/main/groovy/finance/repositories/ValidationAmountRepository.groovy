package finance.repositories

import com.google.inject.Inject
import finance.domain.Account
import finance.domain.ValidationAmount
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_ACCOUNT
import static org.jooq.generated.Tables.T_ACCOUNT
import static org.jooq.generated.Tables.T_VALIDATION_AMOUNT

@Log
class ValidationAmountRepository {
    private final DSLContext dslContext

    @Inject
    ValidationAmountRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    boolean validationAmountInsert(ValidationAmount validationAmount) {
        dslContext.newRecord(T_VALIDATION_AMOUNT, validationAmount).store()
        return true
    }

    List<ValidationAmount> validationAmounts() {
        return dslContext.selectFrom(T_VALIDATION_AMOUNT).where().fetchInto(ValidationAmount)
    }

    List<ValidationAmount> validationAmounts(Long accountId) {
        return dslContext.selectFrom(T_VALIDATION_AMOUNT).where(T_VALIDATION_AMOUNT.ACCOUNT_ID.equal(accountId)).fetchInto(ValidationAmount)
    }
}