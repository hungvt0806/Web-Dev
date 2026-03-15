package com.shopee.ecommerce.module.product.controller;

import com.shopee.ecommerce.module.product.dto.request.ProductSearchRequest;
import com.shopee.ecommerce.module.product.dto.response.ProductDetailResponse;
import com.shopee.ecommerce.module.product.dto.response.ProductSummaryResponse;
import com.shopee.ecommerce.module.product.service.ProductService;
import com.shopee.ecommerce.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public product catalog controller.
 *
 * Base path: /api/products
 *
 * All endpoints are public (no JWT required).
 * Configured as permitted in SecurityConfig.
 *
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │  GET  /api/products              — paginated catalog with filters   │
 * │  GET  /api/products/{id}         — product detail by ID             │
 * │  GET  /api/products/slug/{slug}  — product detail by slug (SEO)     │
 * │  GET  /api/products/featured     — featured products carousel       │
 * │  GET  /api/products/category/{id}— products in a category           │
 * │  GET  /api/products/search       — full-text search (alias)         │
 * └─────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Public product catalog — browse, search, and filter")
public class ProductController {

    private final ProductService productService;

    // ── Catalog listing with filters ─────────────────────────────────────────

    @GetMapping
    @Operation(
        summary     = "Search and browse products",
        description = "Returns a paginated list of ACTIVE products matching all supplied filters. " +
                      "All filter parameters are optional and combined with AND."
    )
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> list(
            @Valid @ModelAttribute ProductSearchRequest request
    ) {
        Page<ProductSummaryResponse> page = productService.search(request);
        return ResponseEntity.ok(ApiResponse.ofPage(page));
    }

    // ── Product detail ────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get product detail by numeric ID")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(productService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(
        summary     = "Get product detail by URL slug",
        description = "Preferred endpoint for frontend product pages — stable URLs for SEO."
    )
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getBySlug(
            @PathVariable String slug
    ) {
        return ResponseEntity.ok(ApiResponse.success(productService.getBySlug(slug)));
    }

    // ── Featured products ─────────────────────────────────────────────────────

    @GetMapping("/featured")
    @Operation(
        summary = "Get featured products",
        description = "Returns up to {limit} featured ACTIVE products, ordered by newest first."
    )
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> featured(
            @Parameter(description = "Max number of products to return (1–50)")
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(productService.getFeatured(limit)));
    }

    // ── Products by category ──────────────────────────────────────────────────

    @GetMapping("/category/{categoryId}")
    @Operation(
        summary     = "Products in a specific category",
        description = "When includeDescendants=true (default), recursively includes products " +
                      "from all sub-categories as well."
    )
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> byCategory(
            @PathVariable Long categoryId,
            @RequestParam(defaultValue = "true") boolean includeDescendants,
            @Valid @ModelAttribute ProductSearchRequest request
    ) {
        Page<ProductSummaryResponse> page = productService.getByCategory(
                categoryId, includeDescendants, request);
        return ResponseEntity.ok(ApiResponse.ofPage(page));
    }

    // ── Search alias ──────────────────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(
        summary     = "Search products by keyword",
        description = "Alias for GET /api/products?keyword=…  Provided for explicit search UX."
    )
    public ResponseEntity<ApiResponse<List<ProductSummaryResponse>>> search(
            @RequestParam String keyword,
            @Valid @ModelAttribute ProductSearchRequest request
    ) {
        request.setKeyword(keyword);
        Page<ProductSummaryResponse> page = productService.search(request);
        return ResponseEntity.ok(ApiResponse.ofPage(page));
    }
}
