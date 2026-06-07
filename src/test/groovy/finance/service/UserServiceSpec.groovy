package finance.service

import finance.domain.User
import finance.repositories.UserRepository
import finance.services.UserService
import org.mindrot.jbcrypt.BCrypt
import spock.lang.Specification

class UserServiceSpec extends Specification {

    UserRepository userRepository = Mock()
    UserService service = new UserService(userRepository)

    private static final String VALID_PASSWORD = 'Secret1@'

    def 'signIn returns user when credentials are correct'() {
        given:
        String hash = BCrypt.hashpw(VALID_PASSWORD, BCrypt.gensalt(4))
        User dbUser = new User(username: 'henninb', password: hash, activeStatus: true)
        userRepository.findByUsername('henninb') >> dbUser

        when:
        Optional<User> result = service.signIn('henninb', VALID_PASSWORD)

        then:
        result.isPresent()
        result.get().username == 'henninb'
    }

    def 'signIn returns empty when password is wrong'() {
        given:
        String hash = BCrypt.hashpw(VALID_PASSWORD, BCrypt.gensalt(4))
        User dbUser = new User(username: 'henninb', password: hash, activeStatus: true)
        userRepository.findByUsername('henninb') >> dbUser

        when:
        Optional<User> result = service.signIn('henninb', 'WrongPass1@')

        then:
        !result.isPresent()
    }

    def 'signIn returns empty when user does not exist'() {
        given:
        userRepository.findByUsername('nobody') >> null

        when:
        Optional<User> result = service.signIn('nobody', VALID_PASSWORD)

        then:
        !result.isPresent()
    }

    def 'signUp hashes password and saves user'() {
        given:
        User user = new User(username: 'newuser', password: VALID_PASSWORD, activeStatus: true)
        userRepository.existsByUsername('newuser') >> false
        User saved = new User(username: 'newuser', password: 'hashed', activeStatus: true)
        userRepository.findByUsername('newuser') >> saved

        when:
        User result = service.signUp(user)

        then:
        1 * userRepository.saveUser({ User u -> BCrypt.checkpw(VALID_PASSWORD, u.password) })
        result.username == 'newuser'
    }

    def 'signUp throws when username already exists'() {
        given:
        userRepository.existsByUsername('henninb') >> true

        when:
        service.signUp(new User(username: 'henninb', password: VALID_PASSWORD))

        then:
        thrown(IllegalArgumentException)
    }

    def 'signUp throws when password is too short'() {
        given:
        userRepository.existsByUsername('newuser') >> false

        when:
        service.signUp(new User(username: 'newuser', password: 'Ab1@'))

        then:
        IllegalArgumentException ex = thrown()
        ex.message.contains('8 characters')
    }

    def 'signUp throws when password is pre-encoded'() {
        given:
        userRepository.existsByUsername('newuser') >> false

        when:
        service.signUp(new User(username: 'newuser', password: '$2a$10$alreadyhashed'))

        then:
        IllegalArgumentException ex = thrown()
        ex.message.contains('Pre-encoded')
    }

    def 'signUp throws when password lacks required complexity'() {
        given:
        userRepository.existsByUsername('newuser') >> false

        when:
        service.signUp(new User(username: 'newuser', password: 'allowercase1'))

        then:
        thrown(IllegalArgumentException)
    }

    def 'findUserByUsername clears password before returning'() {
        given:
        User dbUser = new User(username: 'henninb', password: 'secrethash', activeStatus: true)
        userRepository.findByUsername('henninb') >> dbUser

        when:
        User result = service.findUserByUsername('henninb')

        then:
        result.password == ''
    }

    def 'findUserByUsername returns null when user not found'() {
        given:
        userRepository.findByUsername('nobody') >> null

        expect:
        service.findUserByUsername('nobody') == null
    }
}
