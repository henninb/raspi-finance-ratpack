package finance.services

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service
import ratpack.core.service.StartEvent
import ratpack.core.service.StopEvent

import javax.inject.Inject
import java.security.MessageDigest
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Log
@CompileStatic
class TokenBlacklistService implements Service {

    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>()
    private final ScheduledExecutorService cleanup = Executors.newSingleThreadScheduledExecutor { Runnable r ->
        Thread t = new Thread(r, "token-blacklist-cleanup")
        t.daemon = true
        return t
    }

    @Inject
    TokenBlacklistService() {}

    @Override
    void onStart(StartEvent event) {
        cleanup.scheduleWithFixedDelay(this::cleanupExpiredTokens, 1L, 1L, TimeUnit.HOURS)
        log.info("TokenBlacklistService initialized with hourly cleanup")
    }

    @Override
    void onStop(StopEvent event) {
        cleanup.shutdown()
        try {
            if (!cleanup.awaitTermination(5L, TimeUnit.SECONDS)) {
                cleanup.shutdownNow()
            }
        } catch (InterruptedException ex) {
            cleanup.shutdownNow()
            Thread.currentThread().interrupt()
        }
        log.info("TokenBlacklistService cleanup executor shut down")
    }

    void blacklistToken(String token, long expirationTime) {
        String hash = hashToken(token)
        Instant expiresAt = Instant.ofEpochMilli(expirationTime)
        blacklist.put(hash, expiresAt)
        log.info("Token blacklisted (hash=${hash.take(8)}...), expires: ${expiresAt}")
    }

    boolean isBlacklisted(String token) {
        String hash = hashToken(token)
        Instant expiresAt = blacklist.get(hash)
        if (!expiresAt) return false
        if (Instant.now().isAfter(expiresAt)) {
            blacklist.remove(hash)
            return false
        }
        return true
    }

    int getBlacklistSize() {
        return blacklist.size()
    }

    private void cleanupExpiredTokens() {
        Instant now = Instant.now()
        int removed = 0
        Iterator<Map.Entry<String, Instant>> iter = blacklist.entrySet().iterator()
        while (iter.hasNext()) {
            Map.Entry<String, Instant> entry = iter.next()
            if (now.isAfter(entry.value)) {
                iter.remove()
                removed++
            }
        }
        if (removed > 0) {
            log.info("Cleaned up ${removed} expired tokens from in-memory blacklist")
        }
    }

    private String hashToken(String token) {
        MessageDigest digest = MessageDigest.getInstance("SHA-256")
        byte[] bytes = digest.digest(token.getBytes("UTF-8"))
        StringBuilder sb = new StringBuilder()
        for (byte b : bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }
}
