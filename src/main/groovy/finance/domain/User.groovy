package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import groovy.transform.ToString

import java.sql.Timestamp

@ToString(excludes = ['password'])
@JsonIgnoreProperties(ignoreUnknown = true)
class User {
    Long userId
    String firstName = ""
    String lastName = ""
    String username = ""
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String password = ""
    Boolean activeStatus = true
    Timestamp dateAdded
    Timestamp dateUpdated
}
