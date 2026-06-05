package finance.domain

class AuthenticatedUser {
    final String username
    final boolean keepLoggedIn

    AuthenticatedUser(String username, boolean keepLoggedIn) {
        this.username = username
        this.keepLoggedIn = keepLoggedIn
    }
}
