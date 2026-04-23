package com.techup.course_flow_server.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Map<String, RateLimitEntry> limitStore = new ConcurrentHashMap<>();

    /**
     * Check if request is allowed based on rate limit.
     * 
     * @param key Unique key (e.g. "user:{userId}:material-progress-update")
     * @param maxRequests Maximum requests allowed in window
     * @param windowSeconds Time window in seconds
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        long nowEpochSecond = Instant.now().getEpochSecond();
        RateLimitEntry entry = limitStore.compute(key, (k, existing) -> {
            if (existing == null) {
                return new RateLimitEntry(nowEpochSecond, 1);
            }
            long windowStart = nowEpochSecond - windowSeconds;
            if (existing.windowStart < windowStart) {
                // Window expired, reset
                return new RateLimitEntry(nowEpochSecond, 1);
            }
            // Within window, increment count
            return new RateLimitEntry(existing.windowStart, existing.count + 1);
        });

        return entry.count <= maxRequests;
    }

    /**
     * Build rate limit key for material progress update.
     */
    public String buildMaterialProgressUpdateKey(UUID userId) {
        return "user:" + userId + ":material-progress-update";
    }

    /**
     * Clean up expired entries (optional, for memory management).
     * Call periodically or when store size exceeds threshold.
     */
    public void cleanup(int windowSeconds) {
        long nowEpochSecond = Instant.now().getEpochSecond();
        long cutoff = nowEpochSecond - windowSeconds;
        limitStore.entrySet().removeIf(entry -> entry.getValue().windowStart < cutoff);
    }

    private static class RateLimitEntry {
        final long windowStart;
        final int count;

        RateLimitEntry(long windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
