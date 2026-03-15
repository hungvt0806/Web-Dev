package com.shopee.ecommerce.module.product.controller;

import com.shopee.ecommerce.module.product.dto.request.CreateProductRequest;
import com.shopee.ecommerce.module.product.dto.request.ProductSearchRequest;
import com.shopee.ecommerce.module.product.dto.request.UpdateProductRequest;
import com.shopee.ecommerce.module.product.dto.response.AdminProductResponse;
import com.shopee.ecommerce.module.product.service.ProductService;
import com.shopee.ecommerce.security.UserPrincipal;
import com.shopee.ecommerce.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Admin / Seller product management controller.
 *
 * Base path: /api/admin/products
 *
 * Authorization:
 *  - SELLER  → can create, update, delete their own products; upload images
 *  - ADMIN   → can manage any product
 *
 * ┌──────────────────────────────────────────────────────────────────────────┐
 * │  GET    /api/admin/products            — list products (admin view)      │
 * │  GET    /api/admin/products/{id}       — get product detail (admin view) │
 * │  POST   /api/admin/products            — create product                  │
 * │  PUT    /api/admin/products/{id}       — update product                  │
 * │  DELETE /api/admin/products/{id}       — soft-delete product              │
 * │  POST   /api/admin/products/{id}/images       — upload images            │
 * │  DELETE /api/admin/products/{id}/images       — delete an image          │
 * └──────────────────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SELLER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin: Products", description = "Product management for sellers and admins")
public class AdminProductController {

    private final ProductService productService;

    // ── List (admin view, includes all statuses) ──────────────────────────────

    @GetMapping
    @Operation(
        summary     = "List products (admin)",
        description = "ADMIN: returns all products. SELLER: returns only their own products. " +
                      "Supports same search/filter params as public catalog."
    )
    public ResponseEntity<ApiResponse<List<AdminProductResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @ModelAttribute ProductSearchRequest request
    ) {
        // Admin can see all; sellers only see their own
        UUID sellerId = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))
                ? null
                : principal.getId();

        Page<AdminProductResponse> page = productService.listForAdmin(sellerId, request);
        return ResponseEntity.ok(ApiResponse.ofPage(page));
    }

    // ── Detail ────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @Operation(summary = "Get product detail (admin view, any status)")
    public ResponseEntity<ApiResponse<AdminProductResponse>> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(productService.getByIdForAdmin(id)));
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(
        summary     = "Create a new product",
        description = "Creates a product in DRAFT status unless status=ACTIVE is specified. " +
                      "Slug is auto-generated from the product name."
    )
    public ResponseEntity<ApiResponse<AdminProductResponse>> create(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminProductResponse product = productService.create(request, principal.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    @Operation(
        summary     = "Update a product",
        description = "Partial update — only non-null fields in the request body are applied. " +
                      "Sellers may only update their own products."
    )
    public ResponseEntity<ApiResponse<AdminProductResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminProductResponse updated = productService.update(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updated));
    }

    // ── Publish / Pause shortcuts ─────────────────────────────────────────────

    @PatchMapping("/{id}/publish")
    @Operation(summary = "Publish a DRAFT product (set status = ACTIVE)")
    public ResponseEntity<ApiResponse<AdminProductResponse>> publish(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UpdateProductRequest req = UpdateProductRequest.builder()
                .status(com.shopee.ecommerce.module.product.entity.Product.ProductStatus.ACTIVE)
                .build();
        return ResponseEntity.ok(ApiResponse.success(productService.update(id, req, principal.getId())));
    }

    @PatchMapping("/{id}/pause")
    @Operation(summary = "Pause an ACTIVE product (set status = PAUSED)")
    public ResponseEntity<ApiResponse<AdminProductResponse>> pause(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UpdateProductRequest req = UpdateProductRequest.builder()
                .status(com.shopee.ecommerce.module.product.entity.Product.ProductStatus.PAUSED)
                .build();
        return ResponseEntity.ok(ApiResponse.success(productService.update(id, req, principal.getId())));
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @Operation(
        summary     = "Soft-delete a product",
        description = "Sets product status to DELETED. The product immediately disappears " +
                      "from all public queries. This action can be reversed by an admin."
    )
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        productService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.message("Product deleted successfully"));
    }

    // ── Image management ──────────────────────────────────────────────────────

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary     = "Upload product images",
        description = "Upload 1–10 image files (JPEG, PNG, WebP, GIF; max 5 MB each). " +
                      "The first uploaded image becomes the thumbnail if none exists. " +
                      "Returns the list of public image URLs."
    )
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @PathVariable Long id,
            @RequestPart("files")
            @Size(min = 1, max = 10, message = "Upload 1 to 10 images at a time")
            List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<String> urls = productService.uploadImages(id, files, principal.getId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        urls.size() + " image(s) uploaded successfully", urls));
    }

    @DeleteMapping("/{id}/images")
    @Operation(
        summary     = "Delete a product image",
        description = "Remove an image by its URL. Updates thumbnail if the deleted image was the thumbnail."
    )
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long id,
            @RequestParam String imageUrl,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        productService.deleteImage(id, imageUrl, principal.getId());
        return ResponseEntity.ok(ApiResponse.message("Image deleted successfully"));
    }
}
