package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonSetter
import groovy.transform.ToString

import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat

@ToString
class MedicalExpense {
    Long medicalExpenseId
    String owner
    Long transactionId
    Long providerId
    Long familyMemberId
    Date serviceDate
    String serviceDescription
    String procedureCode
    String diagnosisCode
    BigDecimal billedAmount = BigDecimal.ZERO
    BigDecimal insuranceDiscount = BigDecimal.ZERO
    BigDecimal insurancePaid = BigDecimal.ZERO
    BigDecimal patientResponsibility = BigDecimal.ZERO
    Date paidDate
    Boolean isOutOfNetwork = false
    String claimNumber
    String claimStatus = "submitted"
    Boolean activeStatus = true
    BigDecimal paidAmount = BigDecimal.ZERO
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())

    @JsonSetter("serviceDate")
    void jsonSetterServiceDate(String dateStr) {
        this.serviceDate = dateStr ? java.sql.Date.valueOf(java.time.LocalDate.parse(dateStr)) : null
    }

    @JsonSetter("paidDate")
    void jsonSetterPaidDate(String dateStr) {
        this.paidDate = dateStr ? java.sql.Date.valueOf(java.time.LocalDate.parse(dateStr)) : null
    }

    @JsonGetter("serviceDate")
    String jsonGetterServiceDate() {
        if (!serviceDate) return null
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
        sdf.lenient = false
        return sdf.format(serviceDate)
    }

    @JsonGetter("paidDate")
    String jsonGetterPaidDate() {
        if (!paidDate) return null
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
        sdf.lenient = false
        return sdf.format(paidDate)
    }
}
