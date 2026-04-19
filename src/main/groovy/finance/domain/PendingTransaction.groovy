package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import groovy.transform.ToString

import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat

@ToString
class PendingTransaction {
    Long pendingTransactionId
    String accountNameOwner
    Date transactionDate
    String description
    BigDecimal amount
    String reviewStatus = "pending"
    String owner
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())

    @JsonGetter("transactionDate")
    String jsonGetterTransactionDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
        sdf.lenient = false
        return sdf.format(this.transactionDate)
    }
}
