package finance.domain

import groovy.transform.ToString

import java.sql.Timestamp

@ToString
class Description {
    Long descriptionId
    String owner = ""
    String descriptionName
    Boolean activeStatus
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())
}
