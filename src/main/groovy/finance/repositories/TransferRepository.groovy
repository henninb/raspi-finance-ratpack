package finance.repositories

import com.google.inject.Inject
import finance.domain.Transfer
import groovy.util.logging.Slf4j
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_TRANSFER

@Slf4j
class TransferRepository {
    private final DSLContext dslContext

    @Inject
    TransferRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    boolean transferInsert(Transfer transfer) {
        def record = dslContext.insertInto(T_TRANSFER)
                .set(T_TRANSFER.OWNER, transfer.owner ?: "")
                .set(T_TRANSFER.SOURCE_ACCOUNT, (String) transfer.sourceAccount)
                .set(T_TRANSFER.DESTINATION_ACCOUNT, (String) transfer.destinationAccount)
                .set(T_TRANSFER.AMOUNT, (BigDecimal) transfer.amount)
                .set(T_TRANSFER.TRANSACTION_DATE, (java.time.LocalDate) transfer.transactionDate?.toLocalDate())
                .set(T_TRANSFER.GUID_SOURCE, (String) transfer.guidSource)
                .set(T_TRANSFER.GUID_DESTINATION, (String) transfer.guidDestination)
                .set(T_TRANSFER.ACTIVE_STATUS, (Boolean) transfer.activeStatus)
                .returning(T_TRANSFER.TRANSFER_ID)
                .fetchOne()
        transfer.transferId = record?.get(T_TRANSFER.TRANSFER_ID) as Long
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

    boolean existsByTransactionGuid(String guid) {
        return dslContext.fetchCount(
            dslContext.selectFrom(T_TRANSFER)
                .where(T_TRANSFER.GUID_SOURCE.eq(guid)
                    .or(T_TRANSFER.GUID_DESTINATION.eq(guid)))
        ) > 0
    }

    boolean existsByTransfer(String sourceAccount, String destinationAccount, BigDecimal amount, java.sql.Date transactionDate) {
        return dslContext.fetchCount(
            dslContext.selectFrom(T_TRANSFER)
                .where(T_TRANSFER.SOURCE_ACCOUNT.eq(sourceAccount)
                    .and(T_TRANSFER.DESTINATION_ACCOUNT.eq(destinationAccount))
                    .and(T_TRANSFER.AMOUNT.eq(amount))
                    .and(T_TRANSFER.TRANSACTION_DATE.eq(transactionDate?.toLocalDate()))
                    .and(T_TRANSFER.ACTIVE_STATUS.eq(true)))
        ) > 0
    }
}
