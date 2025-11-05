package com.carebridge.security;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    static class State { int fails; Instant windowStart; Instant blockedUntil; }
    private final Map<String, State> map = new ConcurrentHashMap<>();
    private final int maxFails; private final long windowSec; private final long blockSec;

    public RateLimiter(int maxFails, long windowSec, long blockSec) {
        this.maxFails = maxFails; this.windowSec = windowSec; this.blockSec = blockSec;
    }

    public boolean isBlocked(String key) {
        var s = map.get(key);
        return s != null && s.blockedUntil != null && s.blockedUntil.isAfter(Instant.now());
    }

    public void registerFail(String key) {
        var now = Instant.now();
        var s = map.computeIfAbsent(key, k -> new State());
        if (s.windowStart == null || s.windowStart.plusSeconds(windowSec).isBefore(now)) {
            s.windowStart = now; s.fails = 0;
        }
        if (++s.fails >= maxFails) s.blockedUntil = now.plusSeconds(blockSec);
    }

    public void reset(String key) { map.remove(key); }
    public long secondsUntilUnblock(String key) {
        var s = map.get(key);
        if (s == null || s.blockedUntil == null) return 0;
        long d = s.blockedUntil.getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(d, 0);
    }
}
