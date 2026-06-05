package finance.services

import groovy.util.logging.Log
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import ratpack.core.service.Service

import javax.crypto.SecretKey
import javax.inject.Inject

@Log
class JwtTokenService implements Service {

    static final String ISSUER = "raspi-finance-endpoint"
    static final String AUDIENCE = "raspi-finance-endpoint"
    static final String CLAIM_USERNAME = "username"
    static final String CLAIM_KEEP_LOGGED_IN = "keepLoggedIn"
    static final long JWT_EXPIRY_MS = 3_600_000L
    static final long JWT_LONG_EXPIRY_MS = 30L * 24 * 60 * 60 * 1_000L
    static final long JWT_EXPIRY_SECONDS = 3_600L
    static final long JWT_LONG_EXPIRY_SECONDS = 30L * 24 * 60 * 60L
    private static final int MIN_JWT_KEY_BYTES = 32

    private final SecretKey secretKey

    @Inject
    JwtTokenService() {
        String jwtKey = System.getenv("JWT_KEY") ?: "dev-jwt-key-minimum-32-bytes-here!!"
        byte[] keyBytes = jwtKey.getBytes("UTF-8")
        if (keyBytes.length < MIN_JWT_KEY_BYTES) {
            throw new IllegalStateException("JWT_KEY must be at least ${MIN_JWT_KEY_BYTES} bytes but is ${keyBytes.length}")
        }
        secretKey = Keys.hmacShaKeyFor(keyBytes)
        log.info("JwtTokenService initialized with ${keyBytes.length}-byte key")
    }

    String extractToken(String cookieHeader, String authHeader) {
        if (cookieHeader) {
            String found = cookieHeader.split(';').collect { it.trim() }.find { it.startsWith("token=") }
            if (found) return found.substring("token=".length())
        }
        if (authHeader?.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length()).trim()
        }
        return null
    }

    Claims parseClaims(String token) {
        return Jwts.parser()
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .payload
    }

    String buildToken(String username, boolean keepLoggedIn = false) {
        long expiryMs = keepLoggedIn ? JWT_LONG_EXPIRY_MS : JWT_EXPIRY_MS
        Date now = new Date()
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(ISSUER)
                .audience().add(AUDIENCE).and()
                .subject(username)
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_KEEP_LOGGED_IN, keepLoggedIn)
                .issuedAt(now)
                .notBefore(now)
                .expiration(new Date(now.time + expiryMs))
                .signWith(secretKey)
                .compact()
    }

    long expirySecondsFor(boolean keepLoggedIn) {
        return keepLoggedIn ? JWT_LONG_EXPIRY_SECONDS : JWT_EXPIRY_SECONDS
    }

    String buildSetCookieHeader(String token, boolean keepLoggedIn) {
        long maxAge = expirySecondsFor(keepLoggedIn)
        return "token=${token}; Path=/; Max-Age=${maxAge}; HttpOnly; SameSite=Lax"
    }

    String buildClearCookieHeader() {
        return "token=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
    }
}
