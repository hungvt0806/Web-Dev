package com.shopee.ecommerce.infrastructure.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.api.StatefulRedisConnection;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Distributed rate limiting via Bucket4j + Redis.
 *
 * Limits (production defaults, overridable via env):
 *   PUBLIC  — 100 req / min per IP  (product browse, search)
 *   AUTH    —  10 req / min per IP  (login, register — brute-force guard)
 *   API     — 300 req / min per userId (authenticated API calls)
 *   UPLOAD  —  20 req / hour per userId (S3 uploads)
 *   SEARCH  —  60 req / min per IP  (ES search)
 *
 * Buckets are stored as Lettuce hashes in Redis with key pattern:
 *   rate:{type}:{key}
 */
@Slf4j
@Configuration
public class RateLimitConfig {

    @Value("${rate-limit.public.capacity:100}")
    private long publicCapacity;

    @Value("${rate-limit.auth.capacity:10}")
    private long authCapacity;

    @Value("${rate-limit.api.capacity:300}")
    private long apiCapacity;

    @Value("${rate-limit.upload.capacity:20}")
    private long uploadCapacity;

    @Value("${rate-limit.search.capacity:60}")
    private long searchCapacity;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public ProxyManager<String> bucket4jProxyManager() {
        String uri = redisPassword.isBlank()
                ? "redis://" + redisHost + ":" + redisPort
                : "redis://:" + redisPassword + "@" + redisHost + ":" + redisPort;

        RedisClient client = RedisClient.create(uri);
        StatefulRedisConnection<String, byte[]> connection = client.connect(
                RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
        );
        return LettuceBasedProxyManager.builderFor(connection)
                .build();
    }

    // ── Bucket configuration factories ─────────────────────────

    public Supplier<BucketConfiguration> publicBucketConfig() {
        return () -> BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(publicCapacity, Refill.greedy(publicCapacity, Duration.ofMinutes(1))))
            .build();
    }

    public Supplier<BucketConfiguration> authBucketConfig() {
        return () -> BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(authCapacity, Refill.greedy(authCapacity, Duration.ofMinutes(1))))
            .build();
    }

    public Supplier<BucketConfiguration> apiBucketConfig() {
        return () -> BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(apiCapacity, Refill.greedy(apiCapacity, Duration.ofMinutes(1))))
            .build();
    }

    public Supplier<BucketConfiguration> uploadBucketConfig() {
        return () -> BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(uploadCapacity, Refill.greedy(uploadCapacity, Duration.ofHours(1))))
            .build();
    }

    public Supplier<BucketConfiguration> searchBucketConfig() {
        return () -> BucketConfiguration.builder()
            .addLimit(Bandwidth.classic(searchCapacity, Refill.greedy(searchCapacity, Duration.ofMinutes(1))))
            .build();
    }
}
