package com.shopee.ecommerce.infrastructure.cache;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;
    @Value("${spring.data.redis.port:6379}")
    private int port;
    @Value("${spring.data.redis.password:}")
    private String password;
    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration();
        cfg.setHostName(host);
        cfg.setPort(port);
        cfg.setDatabase(database);
        if (password != null && !password.isBlank()) cfg.setPassword(password);
        log.info("Redis connecting → {}:{} db={}", host, port, database);
        return new LettuceConnectionFactory(cfg);
    }

    /** Redis-only ObjectMapper — NOT exposed as primary bean, only used internally. */
    private ObjectMapper buildRedisObjectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .activateDefaultTyping(
                        LaissezFaireSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL,
                        JsonTypeInfo.As.PROPERTY);
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        var tpl = new RedisTemplate<String, Object>();
        tpl.setConnectionFactory(factory);
        var json = new GenericJackson2JsonRedisSerializer(buildRedisObjectMapper());
        var str  = new StringRedisSerializer();
        tpl.setKeySerializer(str);
        tpl.setHashKeySerializer(str);
        tpl.setValueSerializer(json);
        tpl.setHashValueSerializer(json);
        tpl.afterPropertiesSet();
        return tpl;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        var json = new GenericJackson2JsonRedisSerializer(buildRedisObjectMapper());
        var str  = new StringRedisSerializer();

        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .prefixCacheNameWith("shopee:")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(str))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(json))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
        perCache.put(CacheNames.PRODUCTS_LIST,     base.entryTtl(Duration.ofMinutes(5)));
        perCache.put(CacheNames.PRODUCTS_DETAIL,   base.entryTtl(Duration.ofMinutes(10)));
        perCache.put(CacheNames.PRODUCTS_FEATURED, base.entryTtl(Duration.ofMinutes(5)));
        perCache.put(CacheNames.CATEGORIES,        base.entryTtl(Duration.ofHours(1)));
        perCache.put(CacheNames.USER_PROFILE,      base.entryTtl(Duration.ofMinutes(5)));
        perCache.put(CacheNames.SEARCH_RESULTS,    base.entryTtl(Duration.ofMinutes(2)));
        perCache.put(CacheNames.ORDER_SUMMARY,     base.entryTtl(Duration.ofMinutes(3)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(base)
                .withInitialCacheConfigurations(perCache)
                .transactionAware()
                .build();
    }
}