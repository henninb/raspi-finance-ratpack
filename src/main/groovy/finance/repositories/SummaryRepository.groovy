package finance.repositories

import com.google.inject.Inject
import finance.domain.Summary
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.jooq.impl.DSL
import org.jooq.*;
import javax.sql.DataSource

import static org.jooq.generated.Tables.T_TRANSACTION

@Log
class SummaryRepository {
    private final DSLContext dslContext

    @Inject
    SummaryRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    Summary summaryAll() {
//        select
//        (SELECT COALESCE(A.debits, 0.0) - COALESCE(B.credits, 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' AND active_status = true) B) as totals,
//        (SELECT COALESCE(A.debits, 0.0) - COALESCE(B.credits, 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND transaction_state = 'cleared' AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' and transaction_state = 'cleared' AND active_status = true) B) as totalsCleared,
//        (SELECT COALESCE(A.debits, 0.0) - COALESCE(B.credits, 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND transaction_state = 'outstanding' AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' and transaction_state = 'outstanding' AND active_status = true) B) as totalsOutstanding,
//        (SELECT COALESCE(A.debits, 0.0) - COALESCE(B.credits, 0.0) FROM ( SELECT SUM(amount) AS debits FROM t_transaction WHERE account_type = 'debit' AND transaction_state = 'future' AND active_status = true) A,( SELECT SUM(amount) AS credits FROM t_transaction WHERE account_type = 'credit' and transaction_state = 'future' AND active_status = true) B) as totalsFuture;

        Field TOTALS_DEBITS = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("debits"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_TYPE.eq("debit")).asField()

        Field TOTALS_CREDITS = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("credits"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_TYPE.eq("credit")).asField()

        Field TOTALS = dslContext.select(TOTALS_DEBITS.subtract(TOTALS_CREDITS).as("totals")).asField("totals")

        Field CLEARED_DEBITS = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("debits"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_TYPE.eq("debit") & T_TRANSACTION.TRANSACTION_STATE.eq("cleared")).asField()

        Field CLEARED_CREDITS = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("credits"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_TYPE.eq("credit") & T_TRANSACTION.TRANSACTION_STATE.eq("cleared")).asField()

        Field CLEARED = dslContext.select(CLEARED_DEBITS.subtract(CLEARED_CREDITS).as("totalsCleared")).asField("totalsCleared")

        Field OUTSTANDING_DEBITS = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("debits"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_TYPE.eq("debit") & T_TRANSACTION.TRANSACTION_STATE.eq("outstanding")).asField()

        Field OUTSTANDING_CREDITS = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("credits"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_TYPE.eq("credit") & T_TRANSACTION.TRANSACTION_STATE.eq("outstanding")).asField()

        Field OUTSTANDING = dslContext.select(OUTSTANDING_DEBITS.subtract(OUTSTANDING_CREDITS).as("totalsOutstanding")).asField("totalsOutstanding")


        Field FUTURE_DEBITS = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("debits"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_TYPE.eq("debit") & T_TRANSACTION.TRANSACTION_STATE.eq("future")).asField()

        Field FUTURE_CREDITS = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("credits"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_TYPE.eq("credit") & T_TRANSACTION.TRANSACTION_STATE.eq("future")).asField()

        Field FUTURE = dslContext.select(FUTURE_DEBITS.subtract(FUTURE_CREDITS).as("totalsFuture")).asField("totalsFuture")

        Summary summary = dslContext.select(TOTALS, CLEARED, OUTSTANDING,FUTURE)
                .fetchOneInto(Summary)

        return summary
    }

    Summary summary(String accountNameOwner) {

        //select
        //(select sum(amount) from t_transaction where active_status=true and account_name_owner='chase_kari') as totals,
        //(select sum(amount) from t_transaction where transaction_state='cleared' and active_status=true and account_name_owner='chase_kari') as totalsCleared,
        //(select sum(amount) from t_transaction where transaction_state='outstanding' and active_status=true and account_name_owner='chase_kari') as totalsOutstanding,
        //(select sum(amount) from t_transaction where transaction_state='future' and active_status=true and account_name_owner='chase_kari') as totalsFuture;

        Field TOTALS = dslContext.select( DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("totals"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_NAME_OWNER.eq(accountNameOwner))
                .asField("totals")

        Field TOTALS_CLEARED = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("totalsCleared"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_NAME_OWNER.eq(accountNameOwner) & T_TRANSACTION.TRANSACTION_STATE.eq("cleared"))
                .asField("totalsCleared")

        Field TOTALS_OUTSTANDING = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("totalsOutstanding"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_NAME_OWNER.eq(accountNameOwner) & T_TRANSACTION.TRANSACTION_STATE.eq("outstanding"))
                .asField("totalsOutstanding")

        Field TOTALS_FUTURE = dslContext.select(DSL.coalesce(DSL.sum(T_TRANSACTION.AMOUNT), 0.0).as("totalsFuture"))
                .from(T_TRANSACTION)
                .where(T_TRANSACTION.ACTIVE_STATUS.eq(true) & T_TRANSACTION.ACCOUNT_NAME_OWNER.eq(accountNameOwner) & T_TRANSACTION.TRANSACTION_STATE.eq("future"))
                .asField("totalsFuture")

        return dslContext.select(TOTALS, TOTALS_CLEARED, TOTALS_OUTSTANDING, TOTALS_FUTURE)
                .fetchOneInto(Summary)
    }

}