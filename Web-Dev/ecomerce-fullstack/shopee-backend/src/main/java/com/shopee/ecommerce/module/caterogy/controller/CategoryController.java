package com.shopee.ecommerce.module.category.controller;

import com.shopee.ecommerce.module.category.dto.request.CreateCategoryRequest;
import com.shopee.ecommerce.module.category.dto.response.CategoryResponse;
import com.shopee.ecommerce.module.category.service.CategoryService;
import com.shopee.ecommerce.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Category controller.
 *
 * Public reads: GET /api/categories/**        (no auth)
 * Admin writes: POST/PUT/DELETE /api/categories/** (ROLE_ADMIN)
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Product category browsing and management")
public class CategoryController {

    private final CategoryService categoryService;

    // ── Public ────────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all active categories (flat list)")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllActive()));
    }

    @GetMapping("/tree")
    @Operation(
        summary     = "Get full category tree",
        description = "Returns root categories with nested children — useful for rendering menus."
    )
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> tree() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getTree()));
    }

    @GetMapping("/roots")
    @Operation(summary = "Get root-level categories only")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> roots() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getRootCategories()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getById(id)));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get category by slug")
    public ResponseEntity<ApiResponse<CategoryResponse>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getBySlug(slug)));
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create a new category (ADMIN)")
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created", categoryService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update a category (ADMIN)")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Category updated", categoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Deactivate a category (ADMIN)")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        categoryService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.message("Category deactivated"));
    }
}
