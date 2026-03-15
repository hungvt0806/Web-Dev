package com.shopee.ecommerce.module.product.search;

import lombok.Data;

/** Parameters for the full-text product search endpoint. */
@Data
public class ProductSearchRequest {

    /** Free-text query — matched against name, description, tags. */
    private String q;

    /** Filter by category slug. */
    private String category;

    /** Min price (inclusive). */
    private Double minPrice;

    /** Max price (inclusive). */
    private Double maxPrice;

    /** Sort: relevance | price_asc | price_desc | rating | newest | popular */
    private String sort = "relevance";

    /** Pagination */
    private int page = 0;
    private int size = 24;

    /** Only return ACTIVE products (false = admin query). */
    private boolean activeOnly = true;
}
