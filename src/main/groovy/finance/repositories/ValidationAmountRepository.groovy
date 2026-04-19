package finance.repositories

import com.google.inject.Inject
import finance.domain.ValidationAmount
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

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

    boolean validationAmountUpdate(ValidationAmount validationAmount) {
        dslContext.update(T_VALIDATION_AMOUNT)
                .set(T_VALIDATION_AMOUNT.AMOUNT, validationAmount.amount)
                .set(T_VALIDATION_AMOUNT.ACTIVE_STATUS, validationAmount.activeStatus)
                .where(T_VALIDATION_AMOUNT.VALIDATION_ID.eq(validationAmount.validationId))
                .execute()
        return true
    }

    List<ValidationAmount> validationAmounts() {
        return dslContext.selectFrom(T_VALIDATION_AMOUNT).where().fetchInto(ValidationAmount)
    }

    List<ValidationAmount> validationAmounts(Long accountId) {
        return dslContext.selectFrom(T_VALIDATION_AMOUNT)
                .where(T_VALIDATION_AMOUNT.ACCOUNT_ID.equal(accountId))
                .fetchInto(ValidationAmount)
    }

    List<ValidationAmount> validationAmountsByAccountAndState(String accountNameOwner, String transactionState) {
        return dslContext.select(T_VALIDATION_AMOUNT.fields())
                .from(T_VALIDATION_AMOUNT)
                .join(T_ACCOUNT).on(T_VALIDATION_AMOUNT.ACCOUNT_ID.eq(T_ACCOUNT.ACCOUNT_ID))
                .where(T_ACCOUNT.ACCOUNT_NAME_OWNER.eq(accountNameOwner)
                        .and(T_VALIDATION_AMOUNT.TRANSACTION_STATE.eq(transactionState)))
                .fetchInto(ValidationAmount)
    }

    ValidationAmount validationAmount(Long validationId) {
        return dslContext.selectFrom(T_VALIDATION_AMOUNT)
                .where(T_VALIDATION_AMOUNT.VALIDATION_ID.equal(validationId))
                .fetchOneInto(ValidationAmount)
    }

    boolean validationAmountDelete(Long validationId) {
        dslContext.delete(T_VALIDATION_AMOUNT)
                .where(T_VALIDATION_AMOUNT.VALIDATION_ID.equal(validationId))
                .execute()
        return true
    }
}
