package finance.repositories

import com.google.inject.Inject
import finance.domain.Transfer
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_TRANSFER

@Log
class TransferRepository {
    private final DSLContext dslContext

    @Inject
    TransferRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    boolean transferInsert(Transfer transfer) {
        dslContext.newRecord(T_TRANSFER, transfer).store()
        return true
    }

    boolean transferUpdate(Transfer transfer) {
        dslContext.update(T_TRANSFER)
                .set(T_TRANSFER.SOURCE_ACCOUNT, transfer.sourceAccount)
                .set(T_TRANSFER.DESTINATION_ACCOUNT, transfer.destinationAccount)
                .set(T_TRANSFER.AMOUNT, transfer.amount)
                .set(T_TRANSFER.TRANSACTION_DATE, transfer.transactionDate?.toLocalDate())
                .where(T_TRANSFER.TRANSFER_ID.eq(transfer.transferId))
                .execute()
        return true
    }

    List<Transfer> transfers() {
        return dslContext.selectFrom(T_TRANSFER)
                .where(T_TRANSFER.ACTIVE_STATUS.eq(true))
                .orderBy(T_TRANSFER.TRANSACTION_DATE.desc())
                .fetchInto(Transfer)
    }

    Transfer transfer(Long transferId) {
        return dslContext.selectFrom(T_TRANSFER)
                .where(T_TRANSFER.TRANSFER_ID.equal(transferId))
                .fetchOneInto(Transfer)
    }

    boolean transferDelete(Long transferId) {
        dslContext.delete(T_TRANSFER)
                .where(T_TRANSFER.TRANSFER_ID.equal(transferId))
                .execute()
        return true
    }
}
