package com.shopee.ecommerce.module.product.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class ProductSearchResult {
    private List<ProductDocument> products;
    private long  totalHits;
    private int   page;
    private int   size;
    public int getTotalPages() { return (int) Math.ceil((double) totalHits / size); }
}
