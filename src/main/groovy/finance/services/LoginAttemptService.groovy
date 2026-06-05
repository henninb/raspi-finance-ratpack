package finance.services

import groovy.transform.CompileStatic
import groovy.util.logging.Log
import ratpack.core.service.Service

import javax.inject.Inject
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Log
@CompileStatic
class LoginAttemptService implements Service {

    private static final int MAX_ATTEMPTS = 10
    private static final long LOCKOUT_DURATION_SECONDS = 900L
    private static final int MAX_TRACKED_USERNAMES = 10_000
    private static final long ENTRY_TTL_SECONDS = 1800L

    private static class AttemptRecord {
        final int count
        final Instant lockedUntil
        final Instant lastAttemptAt

        AttemptRecord(int count, Instant lockedUntil, Instant lastAttemptAt) {
            this.count = count
            this.lockedUntil = lockedUntil
            this.lastAttemptAt = lastAttemptAt
        }
    }

    private final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>()

    @Inject
    LoginAttemptService() {}

    boolean isLocked(String username) {
        AttemptRecord record = attempts.get(username.toLowerCase())
        if (!record) return false
        boolean locked = record.lockedUntil != null && Instant.now().isBefore(record.lockedUntil)
        if (!locked && record.lockedUntil != null) {
            attempts.remove(username.toLowerCase())
        }
        return locked
    }

    void recordFailure(String username) {
        if (attempts.size() >= MAX_TRACKED_USERNAMES) {
            log.warning("SECURITY: LoginAttemptService at capacity — evicting oldest entries")
            evictOldestEntries()
        }
        String key = username.toLowerCase()
        attempts.compute(key) { String k, AttemptRecord existing ->
            AttemptRecord record = existing ?: new AttemptRecord(0, null, Instant.now())
            int newCount = record.count + 1
            Instant lockedUntil
            if (record.lockedUntil != null) {
                lockedUntil = record.lockedUntil
            } else if (newCount >= MAX_ATTEMPTS) {
                log.warning("SECURITY: account locked after ${newCount} failed attempts: ${key}")
                lockedUntil = Instant.now().plusSeconds(LOCKOUT_DURATION_SECONDS)
            } else {
                lockedUntil = null
            }
            return new AttemptRecord(newCount, lockedUntil, Instant.now())
        }
    }

    void recordSuccess(String username) {
        attempts.remove(username.toLowerCase())
    }

    long remainingLockSeconds(String username) {
        AttemptRecord record = attempts.get(username.toLowerCase())
        if (!record) return 0L
        Instant until = record.lockedUntil
        if (!until) return 0L
        return Math.max(0L, until.epochSecond - Instant.now().epochSecond)
    }

    private void evictOldestEntries() {
        int toRemoveCount = MAX_TRACKED_USERNAMES.intdiv(4)
        List<String> toRemove = attempts.entrySet()
                .findAll { Map.Entry<String, AttemptRecord> e -> e.value.lockedUntil == null }
                .sort(false) { Map.Entry<String, AttemptRecord> a, Map.Entry<String, AttemptRecord> b ->
                    a.value.lastAttemptAt <=> b.value.lastAttemptAt
                }
                .take(toRemoveCount)
                .collect { Map.Entry<String, AttemptRecord> e -> e.key }
        toRemove.each { String k -> attempts.remove(k) }
    }
}
