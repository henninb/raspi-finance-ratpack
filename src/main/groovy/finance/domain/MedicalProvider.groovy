package finance.domain

import groovy.transform.ToString

import java.sql.Timestamp

@ToString
class MedicalProvider {
    Long providerId
    String providerName
    String providerType = "general"
    String specialty
    String npi
    String taxId
    String addressLine1
    String addressLine2
    String city
    String state
    String zipCode
    String country = "US"
    String phone
    String fax
    String email
    String website
    String networkStatus = "unknown"
    String billingName
    String notes
    Boolean activeStatus = true
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())
}
