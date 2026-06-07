package finance.services

import finance.domain.User
import finance.repositories.UserRepository
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.mindrot.jbcrypt.BCrypt
import ratpack.core.service.Service

import javax.inject.Inject
import java.sql.Timestamp

@Slf4j
@CompileStatic
class UserService implements Service {

    // Computed once at class load with strength 4 (fast, ~16 rounds) to prevent timing attacks
    private static final String TIMING_DUMMY_HASH = BCrypt.hashpw("timing-attack-prevention", BCrypt.gensalt(4))

    private UserRepository userRepository

    @Inject
    UserService(UserRepository userRepository) {
        this.userRepository = userRepository
    }

    Optional<User> signIn(String username, String rawPassword) {
        User dbUser = userRepository.findByUsername(username)
        String hashToCheck = (dbUser?.password) ?: TIMING_DUMMY_HASH
        boolean passwordMatches = BCrypt.checkpw(rawPassword, hashToCheck)
        return (dbUser != null && passwordMatches) ? Optional.of(dbUser) : Optional.<User>empty()
    }

    User signUp(User user) {
        if (userRepository.existsByUsername(user.username)) {
            throw new IllegalArgumentException("Username already exists")
        }
        validateRawPassword(user.password)
        user.password = BCrypt.hashpw(user.password, BCrypt.gensalt(12))
        user.dateAdded = new Timestamp(System.currentTimeMillis())
        user.dateUpdated = new Timestamp(System.currentTimeMillis())
        userRepository.saveUser(user)
        log.info("User registered: ${user.username}")
        return userRepository.findByUsername(user.username)
    }

    User findUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
        if (user) {
            user.password = ""
        }
        return user
    }

    private void validateRawPassword(String password) {
        if (password.startsWith('$2a$') || password.startsWith('$2b$') || password.startsWith('$2y$')) {
            throw new IllegalArgumentException("Pre-encoded passwords are not accepted")
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters")
        }
        String pattern = '^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$'
        if (!password.matches(pattern)) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character")
        }
    }
}
