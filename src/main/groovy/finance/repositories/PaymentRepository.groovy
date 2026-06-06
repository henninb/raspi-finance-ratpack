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
        def record = dslContext.insertInto(T_PAYMENT)
                .set(T_PAYMENT.OWNER, payment.owner ?: "")
                .set(T_PAYMENT.SOURCE_ACCOUNT, (String) payment.sourceAccount)
                .set(T_PAYMENT.DESTINATION_ACCOUNT, (String) payment.destinationAccount)
                .set(T_PAYMENT.AMOUNT, (BigDecimal) payment.amount)
                .set(T_PAYMENT.TRANSACTION_DATE, (java.time.LocalDate) payment.transactionDate?.toLocalDate())
                .set(T_PAYMENT.GUID_SOURCE, (String) payment.guidSource)
                .set(T_PAYMENT.GUID_DESTINATION, (String) payment.guidDestination)
                .set(T_PAYMENT.ACTIVE_STATUS, (Boolean) payment.activeStatus)
                .set(T_PAYMENT.DATE_UPDATED, (java.sql.Timestamp) payment.dateUpdated)
                .set(T_PAYMENT.DATE_ADDED, (java.sql.Timestamp) payment.dateAdded)
                .returning(T_PAYMENT.PAYMENT_ID)
                .fetchOne()
        payment.paymentId = record?.get(T_PAYMENT.PAYMENT_ID) as Long
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
        return dslContext.selectFrom(T_PAYMENT)
                .orderBy(T_PAYMENT.TRANSACTION_DATE.desc())
                .fetchInto(Payment)
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

    boolean existsByTransactionGuid(String guid) {
        return dslContext.fetchCount(
            dslContext.selectFrom(T_PAYMENT)
                .where(T_PAYMENT.GUID_SOURCE.eq(guid)
                    .or(T_PAYMENT.GUID_DESTINATION.eq(guid)))
        ) > 0
    }
}
