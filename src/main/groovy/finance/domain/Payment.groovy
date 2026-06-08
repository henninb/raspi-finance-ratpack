package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonSetter
import groovy.transform.ToString

import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat

@ToString
class Payment {
    Long paymentId
    String owner = ""
    String sourceAccount
    String destinationAccount
    BigDecimal amount
    Date transactionDate
    String guidSource
    String guidDestination
    Boolean activeStatus
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())

    @JsonSetter("transactionDate")
    void jsonSetterTransactionDate(String dateStr) {
        this.transactionDate = dateStr ? java.sql.Date.valueOf(java.time.LocalDate.parse(dateStr)) : null
    }

    @JsonGetter("transactionDate")
    String jsonGetterTransactionDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.lenient = false
        return simpleDateFormat.format(this.transactionDate)
    }
}
