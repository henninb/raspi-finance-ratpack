package finance.domain

import groovy.transform.ToString

import java.sql.Timestamp

@ToString
class Category {
    Long categoryId
    String owner = ""
    String categoryName
    Boolean activeStatus
    Timestamp dateUpdated = new Timestamp(System.currentTimeMillis())
    Timestamp dateAdded = new Timestamp(System.currentTimeMillis())
}
