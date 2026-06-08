package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSetter

import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat

@JsonIgnoreProperties(ignoreUnknown = true)
class Transaction {
    Long transactionId
    String owner = ""
    String guid
    Long accountId
    AccountType accountType
    String accountNameOwner
    //Changed from java.sql.Date to String
    //String transactionDate
    Date transactionDate
    Date dueDate
    String description
    String category
    BigDecimal amount
    TransactionState transactionState
    TransactionType transactionType
    ReoccurringType reoccurringType
    String notes
    Boolean activeStatus
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())

    @JsonSetter("transactionDate")
    void jsonSetterTransactionDate(String dateStr) {
        this.transactionDate = dateStr ? java.sql.Date.valueOf(java.time.LocalDate.parse(dateStr)) : null
    }

    @JsonSetter("dueDate")
    void jsonSetterDueDate(String dateStr) {
        this.dueDate = dateStr ? java.sql.Date.valueOf(java.time.LocalDate.parse(dateStr)) : null
    }

    @JsonGetter("transactionDate")
    String jsonGetterTransactionDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.lenient = false
        return simpleDateFormat.format(this.transactionDate)
    }

    @JsonGetter("dueDate")
    String jsonGetterDueDate() {
        if (this.dueDate == null) return null
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
        simpleDateFormat.lenient = false
        return simpleDateFormat.format(this.dueDate)
    }
}

//
////    @JsonGetter("dueDate")
////    fun jsonGetterDueDate(): String {
////        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
////        simpleDateFormat.isLenient = false
////        return simpleDateFormat.format(this.dueDate)
////    }
//
//@JsonSetter("transactionDate")
//fun jsonSetterTransactionDate(stringDate: String) {
//    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")
//    simpleDateFormat.isLenient = false
////        simpleDateFormat.timeZone = TimeZone.getDefault()
////        simpleDateFormat.timeZone = TimeZone.getTimeZone("UTC")
//    this.transactionDate = Date(simpleDateFormat.parse(stringDate).time)
//}