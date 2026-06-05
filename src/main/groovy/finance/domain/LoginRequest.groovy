package finance.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import groovy.transform.ToString

@ToString(excludes = ['password'])
@JsonIgnoreProperties(ignoreUnknown = true)
class LoginRequest {
    String username = ""
    String password = ""
    Boolean keepLoggedIn = false
}
