package finance.service

import finance.services.LoginAttemptService
import spock.lang.Specification

class LoginAttemptServiceSpec extends Specification {

    LoginAttemptService service = new LoginAttemptService()

    def 'isLocked returns false for unknown user'() {
        expect:
        !service.isLocked('nobody')
    }

    def 'isLocked returns false before reaching max attempts'() {
        given:
        9.times { service.recordFailure('henninb') }

        expect:
        !service.isLocked('henninb')
    }

    def 'isLocked returns true after max attempts'() {
        given:
        10.times { service.recordFailure('henninb') }

        expect:
        service.isLocked('henninb')
    }

    def 'isLocked is case-insensitive'() {
        given:
        10.times { service.recordFailure('HENNINB') }

        expect:
        service.isLocked('henninb')
        service.isLocked('HENNINB')
        service.isLocked('HeNnInB')
    }

    def 'recordSuccess clears lock state'() {
        given:
        10.times { service.recordFailure('henninb') }
        assert service.isLocked('henninb')

        when:
        service.recordSuccess('henninb')

        then:
        !service.isLocked('henninb')
    }

    def 'remainingLockSeconds returns 0 for unlocked user'() {
        expect:
        service.remainingLockSeconds('henninb') == 0L
    }

    def 'remainingLockSeconds returns positive value after lock'() {
        given:
        10.times { service.recordFailure('henninb') }

        when:
        long remaining = service.remainingLockSeconds('henninb')

        then:
        remaining > 0L
        remaining <= 900L
    }

    def 'additional failures after lock do not reset the lock timer'() {
        given:
        10.times { service.recordFailure('henninb') }
        long firstRemaining = service.remainingLockSeconds('henninb')

        when:
        service.recordFailure('henninb')
        long secondRemaining = service.remainingLockSeconds('henninb')

        then:
        secondRemaining <= firstRemaining
    }
}
