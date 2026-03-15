package com.shopee.ecommerce.infrastructure.cache;

/** Centralised cache name constants — used in @Cacheable/@CacheEvict annotations. */
public final class CacheNames {
    private CacheNames() {}

    public static final String PRODUCTS_LIST     = "products:list";
    public static final String PRODUCTS_DETAIL   = "products:detail";
    public static final String PRODUCTS_FEATURED = "products:featured";
    public static final String CATEGORIES        = "categories";
    public static final String USER_PROFILE      = "user:profile";
    public static final String SEARCH_RESULTS    = "search:results";
    public static final String ORDER_SUMMARY     = "order:summary";
}
