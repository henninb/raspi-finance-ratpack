package finance.service

import finance.services.JwtTokenService
import io.jsonwebtoken.Claims
import spock.lang.Specification

class JwtTokenServiceSpec extends Specification {

    JwtTokenService service = new JwtTokenService()

    def 'buildToken returns a non-null compact JWT'() {
        when:
        String token = service.buildToken('henninb')

        then:
        token != null
        token.split('\\.').length == 3
    }

    def 'parseClaims round-trips username from built token'() {
        given:
        String token = service.buildToken('henninb')

        when:
        Claims claims = service.parseClaims(token)

        then:
        claims.get('username', String) == 'henninb'
        claims.getSubject() == 'henninb'
    }

    def 'parseClaims round-trips keepLoggedIn flag'() {
        given:
        String shortToken = service.buildToken('henninb', false)
        String longToken = service.buildToken('henninb', true)

        expect:
        service.parseClaims(shortToken).get('keepLoggedIn', Boolean) == false
        service.parseClaims(longToken).get('keepLoggedIn', Boolean) == true
    }

    def 'extractToken finds token in cookie header'() {
        given:
        String cookieHeader = 'session=abc; token=my-jwt-value; lang=en'

        expect:
        service.extractToken(cookieHeader, null) == 'my-jwt-value'
    }

    def 'extractToken finds token in Authorization Bearer header'() {
        expect:
        service.extractToken(null, 'Bearer my-bearer-token') == 'my-bearer-token'
    }

    def 'extractToken prefers cookie over Bearer header'() {
        given:
        String cookieHeader = 'token=cookie-token'

        expect:
        service.extractToken(cookieHeader, 'Bearer bearer-token') == 'cookie-token'
    }

    def 'extractToken returns null when neither header is present'() {
        expect:
        service.extractToken(null, null) == null
    }

    def 'expirySecondsFor returns short expiry for keepLoggedIn false'() {
        expect:
        service.expirySecondsFor(false) == JwtTokenService.JWT_EXPIRY_SECONDS
    }

    def 'expirySecondsFor returns long expiry for keepLoggedIn true'() {
        expect:
        service.expirySecondsFor(true) == JwtTokenService.JWT_LONG_EXPIRY_SECONDS
    }

    def 'buildSetCookieHeader contains token and correct max-age'() {
        given:
        String token = 'some-token'

        when:
        String header = service.buildSetCookieHeader(token, false)

        then:
        header.contains("token=${token}")
        header.contains("Max-Age=${JwtTokenService.JWT_EXPIRY_SECONDS}")
        header.contains('HttpOnly')
    }

    def 'buildClearCookieHeader sets Max-Age to 0'() {
        when:
        String header = service.buildClearCookieHeader()

        then:
        header.contains('Max-Age=0')
        header.contains('token=;')
    }
}
