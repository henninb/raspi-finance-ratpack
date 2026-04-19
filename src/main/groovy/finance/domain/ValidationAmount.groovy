package finance.domain

import java.sql.Timestamp

class ValidationAmount {
    Long validationId
    Long accountId
    Timestamp validationDate
    Boolean activeStatus
    TransactionState transactionState
    BigDecimal amount
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())
}
