package finance.service

import finance.services.TokenBlacklistService
import spock.lang.Specification

class TokenBlacklistServiceSpec extends Specification {

    TokenBlacklistService service = new TokenBlacklistService()

    def 'isBlacklisted returns false for unknown token'() {
        expect:
        !service.isBlacklisted('unknown-token')
    }

    def 'isBlacklisted returns true for blacklisted non-expired token'() {
        given:
        long futureExpiry = System.currentTimeMillis() + 60_000L

        when:
        service.blacklistToken('my-token', futureExpiry)

        then:
        service.isBlacklisted('my-token')
    }

    def 'isBlacklisted returns false and removes expired token'() {
        given:
        long pastExpiry = System.currentTimeMillis() - 1_000L
        service.blacklistToken('expired-token', pastExpiry)

        when:
        boolean result = service.isBlacklisted('expired-token')

        then:
        !result
        service.blacklistSize == 0
    }

    def 'blacklistSize increases with each added token'() {
        when:
        service.blacklistToken('token-1', System.currentTimeMillis() + 60_000L)
        service.blacklistToken('token-2', System.currentTimeMillis() + 60_000L)

        then:
        service.blacklistSize == 2
    }

    def 'different tokens are independently tracked'() {
        given:
        service.blacklistToken('token-a', System.currentTimeMillis() + 60_000L)

        expect:
        service.isBlacklisted('token-a')
        !service.isBlacklisted('token-b')
    }

    def 'same token blacklisted twice is still blacklisted'() {
        given:
        long expiry = System.currentTimeMillis() + 60_000L
        service.blacklistToken('my-token', expiry)
        service.blacklistToken('my-token', expiry)

        expect:
        service.isBlacklisted('my-token')
        service.blacklistSize == 1
    }
}
