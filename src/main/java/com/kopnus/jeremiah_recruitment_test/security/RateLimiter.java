package com.kopnus.jeremiah_recruitment_test.security;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kopnus.jeremiah_recruitment_test.config.RateLimitProperties.Limit;
import com.kopnus.jeremiah_recruitment_test.exception.RateLimitExceededException;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimiter {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(100_000)
            .build();

    /**
     * Takes one token from the key's bucket.
     *
     * @return tokens remaining after this request
     * @throws RateLimitExceededException if the bucket is empty
     */
    public long consume(String key, Limit limit) {
        Bucket bucket = buckets.get(key, k -> newBucket(limit));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            // Round up: telling a client to retry after 0 seconds would be wrong
            long seconds = (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0);
            throw new RateLimitExceededException(Math.max(1, seconds));
        }
        return probe.getRemainingTokens();
    }

    private static Bucket newBucket(Limit limit) {
        return Bucket.builder()
                .addLimit(bandwidth -> bandwidth
                        .capacity(limit.capacity())
                        .refillGreedy(limit.capacity(), limit.period()))
                .build();
    }
}
