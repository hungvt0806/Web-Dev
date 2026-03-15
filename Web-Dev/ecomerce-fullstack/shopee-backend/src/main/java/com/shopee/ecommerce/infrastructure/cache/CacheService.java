package com.shopee.ecommerce.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Programmatic cache operations for bulk invalidation scenarios
 * (e.g. evict all product list caches when any product is updated).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;

    public void evictProductCaches() {
        evictAll(CacheNames.PRODUCTS_LIST);
        evictAll(CacheNames.PRODUCTS_FEATURED);
        log.debug("Evicted product list caches");
    }

    public void evictProductDetail(Object key) {
        evict(CacheNames.PRODUCTS_DETAIL, key);
        log.debug("Evicted product detail cache key={}", key);
    }

    public void evictCategoryCache() {
        evictAll(CacheNames.CATEGORIES);
    }

    public void evictAll(String cacheName) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.clear();
    }

    public void evict(String cacheName, Object key) {
        var cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.evict(key);
    }
}
