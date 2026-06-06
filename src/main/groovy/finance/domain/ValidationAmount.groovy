package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import java.sql.Timestamp

@JsonIgnoreProperties(ignoreUnknown = true)
class ValidationAmount {
    Long validationId
    Long accountId
    String owner = ""
    Timestamp validationDate
    Boolean activeStatus
    TransactionState transactionState
    BigDecimal amount
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())
}
