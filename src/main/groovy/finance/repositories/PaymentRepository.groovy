package finance.repositories

import com.google.inject.Inject
import finance.domain.Description
import finance.domain.Payment
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_DESCRIPTION
import static org.jooq.generated.Tables.T_PAYMENT

@Log
class PaymentRepository {
    private final DSLContext dslContext

    @Inject
    PaymentRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    boolean paymentInsert(Payment payment) {
        dslContext.newRecord(T_PAYMENT, payment).store()
        return true
    }

    List<Payment> payments() {
        return dslContext.selectFrom(T_PAYMENT).where().fetchInto(Payment)
    }
}