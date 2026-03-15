package com.shopee.ecommerce.module.product.service;

import com.shopee.ecommerce.module.product.dto.request.CreateProductRequest;
import com.shopee.ecommerce.module.product.dto.request.ProductSearchRequest;
import com.shopee.ecommerce.module.product.dto.request.UpdateProductRequest;
import com.shopee.ecommerce.module.product.dto.response.AdminProductResponse;
import com.shopee.ecommerce.module.product.dto.response.ProductDetailResponse;
import com.shopee.ecommerce.module.product.dto.response.ProductSummaryResponse;
import com.shopee.ecommerce.util.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Product service contract.
 *
 * Separated into public (buyer-facing) and admin (seller/admin) operations.
 * The controller layer calls only this interface — never the repository directly.
 */
public interface ProductService {

    // ── Public / buyer-facing ─────────────────────────────────────────────────

    /**
     * Paginated, filtered, sorted product listing for the catalog page.
     * Applies all criteria in {@link ProductSearchRequest}: keyword, category,
     * price range, rating, in-stock, featured, sort order.
     *
     * Only ACTIVE products are returned.
     */
    Page<ProductSummaryResponse> search(ProductSearchRequest request);

    /**
     * Full product detail by ID.
     * Includes variants, seller info, and up to 6 related products.
     *
     * @throws com.shopee.ecommerce.exception.ResourceNotFoundException if not found or not ACTIVE
     */
    ProductDetailResponse getById(Long id);

    /**
     * Full product detail by URL slug.
     * Used for SEO-friendly URLs: /products/{slug}
     */
    ProductDetailResponse getBySlug(String slug);

    /**
     * Featured products for homepage carousel.
     * Returns up to {@code limit} ACTIVE + featured products.
     */
    List<ProductSummaryResponse> getFeatured(int limit);

    /**
     * Products in a specific category.
     * When {@code includeDescendants=true}, expands to all sub-categories recursively.
     */
    Page<ProductSummaryResponse> getByCategory(Long categoryId, boolean includeDescendants,
                                                ProductSearchRequest request);

    // ── Admin / seller operations ─────────────────────────────────────────────

    /**
     * Create a new product (seller or admin).
     * Auto-generates slug from name. Saves variants if provided.
     *
     * @param sellerId the authenticated seller's UUID
     */
    AdminProductResponse create(CreateProductRequest request, UUID sellerId);

    /**
     * Update an existing product.
     * Applies only non-null fields (patch semantics).
     * Only the owning seller or ADMIN may update.
     *
     * @throws com.shopee.ecommerce.exception.AccessDeniedException if caller is not owner or admin
     */
    AdminProductResponse update(Long id, UpdateProductRequest request, UUID callerId);

    /**
     * Soft-delete a product by setting status = DELETED.
     * The product becomes invisible to all public queries immediately.
     */
    void delete(Long id, UUID callerId);

    /**
     * Upload one or more product images.
     * Returns the list of public image URLs.
     * The first image becomes the thumbnail if none is set yet.
     *
     * @param productId the target product
     * @param files     1–10 image files (JPEG, PNG, WebP, GIF; max 5 MB each)
     */
    List<String> uploadImages(Long productId, List<MultipartFile> files, UUID callerId);

    /**
     * Delete a single product image by URL.
     * Updates image_urls array and thumbnail_url accordingly.
     */
    void deleteImage(Long productId, String imageUrl, UUID callerId);

    /**
     * Admin: paginated product listing with status filter.
     */
    Page<AdminProductResponse> listForAdmin(UUID sellerId, ProductSearchRequest request);

    /**
     * Admin: get full product details regardless of status.
     */
    AdminProductResponse getByIdForAdmin(Long id);
}
