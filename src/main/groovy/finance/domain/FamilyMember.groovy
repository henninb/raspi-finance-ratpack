package finance.domain

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonSetter
import groovy.transform.ToString

import java.sql.Date
import java.sql.Timestamp
import java.text.SimpleDateFormat

@ToString
class FamilyMember {
    Long familyMemberId
    String owner
    String memberName
    String relationship
    Date dateOfBirth
    String insuranceMemberId
    String ssnLastFour
    String medicalRecordNumber
    Boolean activeStatus = true
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())

    @JsonSetter("dateOfBirth")
    void jsonSetterDateOfBirth(String dateStr) {
        this.dateOfBirth = dateStr ? java.sql.Date.valueOf(java.time.LocalDate.parse(dateStr)) : null
    }

    @JsonGetter("dateOfBirth")
    String jsonGetterDateOfBirth() {
        if (!dateOfBirth) return null
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
        sdf.lenient = false
        return sdf.format(dateOfBirth)
    }
}
