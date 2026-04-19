package finance.repositories

import com.google.inject.Inject
import finance.domain.Payment
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

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

    boolean paymentUpdate(Payment payment) {
        dslContext.update(T_PAYMENT)
                .set(T_PAYMENT.AMOUNT, payment.amount)
                .set(T_PAYMENT.TRANSACTION_DATE, payment.transactionDate?.toLocalDate())
                .where(T_PAYMENT.PAYMENT_ID.eq(payment.paymentId))
                .execute()
        return true
    }

    List<Payment> payments() {
        return dslContext.selectFrom(T_PAYMENT).where().fetchInto(Payment)
    }

    Payment payment(Long paymentId) {
        return dslContext.selectFrom(T_PAYMENT)
                .where(T_PAYMENT.PAYMENT_ID.equal(paymentId))
                .fetchOneInto(Payment)
    }

    boolean paymentDelete(Long paymentId) {
        dslContext.delete(T_PAYMENT)
                .where(T_PAYMENT.PAYMENT_ID.equal(paymentId))
                .execute()
        return true
    }
}
