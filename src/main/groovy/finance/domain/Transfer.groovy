package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonSetter
import groovy.transform.ToString

import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat

@ToString
class Transfer {
    Long transferId
    String sourceAccount
    String destinationAccount
    Date transactionDate
    BigDecimal amount
    String guidSource
    String guidDestination
    String owner
    Boolean activeStatus = true
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())

    @JsonSetter("transactionDate")
    void jsonSetterTransactionDate(String dateStr) {
        this.transactionDate = dateStr ? java.sql.Date.valueOf(java.time.LocalDate.parse(dateStr)) : null
    }

    @JsonGetter("transactionDate")
    String jsonGetterTransactionDate() {
        if (!this.transactionDate) {
            return null
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
        sdf.lenient = false
        return sdf.format(this.transactionDate)
    }
}
