package com.shopee.ecommerce.module.product.service;

import com.shopee.ecommerce.exception.AccessDeniedException;
import com.shopee.ecommerce.exception.ResourceAlreadyExistsException;
import com.shopee.ecommerce.exception.ResourceNotFoundException;
import com.shopee.ecommerce.infrastructure.storage.StorageService;
import com.shopee.ecommerce.module.category.entity.Category;
import com.shopee.ecommerce.module.category.repository.CategoryRepository;
import com.shopee.ecommerce.module.product.dto.request.CreateProductRequest;
import com.shopee.ecommerce.module.product.dto.request.CreateProductRequest.VariantRequest;
import com.shopee.ecommerce.module.product.dto.request.ProductSearchRequest;
import com.shopee.ecommerce.module.product.dto.request.UpdateProductRequest;
import com.shopee.ecommerce.module.product.dto.response.AdminProductResponse;
import com.shopee.ecommerce.module.product.dto.response.ProductDetailResponse;
import com.shopee.ecommerce.module.product.dto.response.ProductSummaryResponse;
import com.shopee.ecommerce.module.product.entity.Product;
import com.shopee.ecommerce.module.product.entity.Product.ProductStatus;
import com.shopee.ecommerce.module.product.entity.ProductImage;
import com.shopee.ecommerce.module.product.entity.ProductVariant;
import com.shopee.ecommerce.module.product.mapper.ProductMapper;
import com.shopee.ecommerce.module.product.repository.ProductImageRepository;
import com.shopee.ecommerce.module.product.repository.ProductRepository;
import com.shopee.ecommerce.module.product.repository.ProductVariantRepository;
import com.shopee.ecommerce.module.product.specification.ProductSpecification;
import com.shopee.ecommerce.module.product.specification.ProductSpecification.ProductFilterRequest;
import com.shopee.ecommerce.module.user.entity.User;
import com.shopee.ecommerce.module.user.repository.UserRepository;
import com.shopee.ecommerce.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Product service — all catalog and admin business logic.
 *
 * Separation of concerns:
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ Controller  — HTTP binding, validation, auth context extraction      │
 * │ Service     — business rules, transactions, orchestration (THIS)     │
 * │ Repository  — DB queries                                             │
 * │ StorageService — file upload/delete                                  │
 * │ Mapper      — entity ↔ DTO conversion                               │
 * └──────────────────────────────────────────────────────────────────────┘
 *
 * All public mutations are @Transactional.
 * Read operations use @Transactional(readOnly = true) to allow Hibernate
 * to skip dirty-checking and use read-optimized connections.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final int MAX_IMAGES_PER_PRODUCT = 10;
    private static final int RELATED_PRODUCT_LIMIT  = 6;
    private static final int FEATURED_DEFAULT_LIMIT = 10;
    private static final String IMAGE_FOLDER        = "products";

    private final ProductRepository        productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository   imageRepository;
    private final CategoryRepository       categoryRepository;
    private final UserRepository           userRepository;
    private final StorageService           storageService;
    private final ProductMapper            mapper;

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC / BUYER OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> search(ProductSearchRequest req) {
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), req.toSort());

        // Resolve category descendants for sub-category filtering
        List<Long> categoryIds = resolveCategoryIds(req.getCategoryId(), true);

        ProductFilterRequest filter = ProductFilterRequest.builder()
                .keyword(req.getKeyword())
                .categoryId(req.getCategoryId())
                .minPrice(req.getMinPrice())
                .maxPrice(req.getMaxPrice())
                .minRating(req.getMinRating())
                .inStockOnly(req.getInStockOnly())
                .featuredOnly(req.getFeaturedOnly())
                .build();

        Specification<Product> spec = ProductSpecification.build(filter, categoryIds, false);
        return productRepository.findAll(spec, pageable).map(mapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getById(Long id) {
        Product product = productRepository.findActiveByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return buildDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getBySlug(String slug) {
        Product product = productRepository.findActiveBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return buildDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> getFeatured(int limit) {
        int effectiveLimit = limit > 0 ? Math.min(limit, 50) : FEATURED_DEFAULT_LIMIT;
        List<Product> products = productRepository.findFeatured(PageRequest.of(0, effectiveLimit));
        return mapper.toSummaryList(products);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> getByCategory(Long categoryId,
                                                       boolean includeDescendants,
                                                       ProductSearchRequest req) {
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), req.toSort());

        if (includeDescendants) {
            List<Long> categoryIds = resolveCategoryIds(categoryId, true);
            return productRepository.findActiveByCategoryIds(categoryIds, pageable)
                    .map(mapper::toSummary);
        }
        // Exact category match
        return productRepository.findActiveByCategoryIds(List.of(categoryId), pageable)
                .map(mapper::toSummary);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN / SELLER OPERATIONS
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public AdminProductResponse create(CreateProductRequest req, UUID sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        // Generate unique slug
        String baseSlug = SlugUtils.toSlug(req.getName());
        String slug = SlugUtils.generateUnique(baseSlug, productRepository::existsBySlug);

        // Resolve category
        Category category = null;
        if (req.getCategoryId() != null) {
            category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", req.getCategoryId()));
        }

        // Build product entity
        Product product = Product.builder()
                .name(req.getName())
                .slug(slug)
                .shortDescription(req.getShortDescription())
                .description(req.getDescription())
                .basePrice(req.getBasePrice())
                .originalPrice(req.getOriginalPrice())
                .tags(req.getTags() != null ? req.getTags() : new ArrayList<>())
                .weightGrams(req.getWeightGrams())
                .status(req.getStatus() != null ? req.getStatus() : ProductStatus.DRAFT)
                .featured(Boolean.TRUE.equals(req.getFeatured()))
                .category(category)
                .seller(seller)
                .build();

        // Build and attach variants
        if (!CollectionUtils.isEmpty(req.getVariants())) {
            for (VariantRequest vr : req.getVariants()) {
                if (variantRepository.existsBySku(vr.getSku())) {
                    throw new ResourceAlreadyExistsException(
                            "SKU '" + vr.getSku() + "' is already in use");
                }
                ProductVariant variant = ProductVariant.builder()
                        .sku(vr.getSku())
                        .attributes(vr.getAttributes() != null ? vr.getAttributes() : new java.util.HashMap<>())
                        .price(vr.getPrice())
                        .stock(vr.getStock())
                        .imageUrl(vr.getImageUrl())
                        .active(true)
                        .build();
                product.addVariant(variant);
            }
            // Sync total stock
            product.setTotalStock(req.getVariants().stream()
                    .mapToInt(VariantRequest::getStock).sum());
        }

        Product saved = productRepository.save(product);
        log.info("Product created: '{}' (id={}, seller={})", saved.getName(), saved.getId(), sellerId);

        return mapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminProductResponse update(Long id, UpdateProductRequest req, UUID callerId) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        assertOwnerOrAdmin(product, callerId);

        // Apply patch fields (only if non-null)
        if (StringUtils.hasText(req.getName())) {
            product.setName(req.getName());
            // Re-generate slug only if name changed; preserve the old one otherwise
            String newBase = SlugUtils.toSlug(req.getName());
            if (!product.getSlug().startsWith(newBase)) {
                String newSlug = SlugUtils.generateUnique(
                        newBase,
                        s -> productRepository.existsBySlugAndIdNot(s, id)
                );
                product.setSlug(newSlug);
            }
        }
        if (req.getShortDescription() != null) product.setShortDescription(req.getShortDescription());
        if (req.getDescription()      != null) product.setDescription(req.getDescription());
        if (req.getBasePrice()        != null) product.setBasePrice(req.getBasePrice());
        if (req.getOriginalPrice()    != null) product.setOriginalPrice(req.getOriginalPrice());
        if (req.getTags()             != null) product.setTags(req.getTags());
        if (req.getWeightGrams()      != null) product.setWeightGrams(req.getWeightGrams());
        if (req.getStatus()           != null) product.setStatus(req.getStatus());
        if (req.getFeatured()         != null) product.setFeatured(req.getFeatured());

        if (req.getCategoryId() != null) {
            Category cat = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", req.getCategoryId()));
            product.setCategory(cat);
        }

        Product saved = productRepository.save(product);
        log.info("Product updated: id={}", id);

        return mapper.toAdminResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, UUID callerId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        assertOwnerOrAdmin(product, callerId);

        // Soft delete — update status; do NOT physically delete rows
        product.setStatus(ProductStatus.DELETED);
        productRepository.save(product);
        log.info("Product soft-deleted: id={} by caller={}", id, callerId);
    }

    @Override
    @Transactional
    public List<String> uploadImages(Long productId, List<MultipartFile> files, UUID callerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        assertOwnerOrAdmin(product, callerId);

        // Guard against exceeding max images
        long existingCount = imageRepository.countByProductId(productId);
        if (existingCount + files.size() > MAX_IMAGES_PER_PRODUCT) {
            throw new IllegalArgumentException(
                    "Product already has " + existingCount + " images. " +
                    "Max " + MAX_IMAGES_PER_PRODUCT + " per product.");
        }

        // Upload files to storage
        List<String> urls = storageService.uploadAll(files, IMAGE_FOLDER);
        short sortBase = (short) existingCount;

        List<ProductImage> savedImages = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            ProductImage img = ProductImage.builder()
                    .product(product)
                    .url(urls.get(i))
                    .altText(product.getName() + " image " + (sortBase + i + 1))
                    .sortOrder((short)(sortBase + i))
                    .build();
            savedImages.add(imageRepository.save(img));
        }

        // Sync JSONB image_urls array on the product
        List<String> allUrls = imageRepository.findByProductIdOrderBySortOrderAsc(productId)
                .stream().map(ProductImage::getUrl).collect(Collectors.toList());

        product.setImageUrls(allUrls);
        if (!StringUtils.hasText(product.getThumbnailUrl()) && !allUrls.isEmpty()) {
            product.setThumbnailUrl(allUrls.get(0));
        }
        productRepository.save(product);

        log.info("Uploaded {} image(s) for product id={}", urls.size(), productId);
        return urls;
    }

    @Override
    @Transactional
    public void deleteImage(Long productId, String imageUrl, UUID callerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        assertOwnerOrAdmin(product, callerId);

        // Remove from storage
        storageService.delete(imageUrl);

        // Remove from normalised table
        List<ProductImage> images = imageRepository.findByProductIdOrderBySortOrderAsc(productId);
        images.stream()
                .filter(img -> img.getUrl().equals(imageUrl))
                .findFirst()
                .ifPresent(imageRepository::delete);

        // Sync JSONB array and thumbnail
        List<String> remaining = imageRepository.findByProductIdOrderBySortOrderAsc(productId)
                .stream().map(ProductImage::getUrl).collect(Collectors.toList());

        product.setImageUrls(remaining);
        if (imageUrl.equals(product.getThumbnailUrl())) {
            product.setThumbnailUrl(remaining.isEmpty() ? null : remaining.get(0));
        }
        productRepository.save(product);

        log.info("Deleted image '{}' from product id={}", imageUrl, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminProductResponse> listForAdmin(UUID sellerId, ProductSearchRequest req) {
        Pageable pageable = PageRequest.of(req.getPage(), req.getSize(), req.toSort());
        return productRepository.findAllForAdmin(sellerId, pageable)
                .map(mapper::toAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminProductResponse getByIdForAdmin(Long id) {
        Product product = productRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return mapper.toAdminResponse(product);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    /** Build the full detail response with related products appended. */
    private ProductDetailResponse buildDetailResponse(Product product) {
        ProductDetailResponse detail = mapper.toDetail(product);

        // Append related products (same category, excluding this one)
        if (product.getCategory() != null) {
            Pageable relatedPage = PageRequest.of(0, RELATED_PRODUCT_LIMIT);
            List<Product> related = productRepository.findRelated(
                    product.getCategory().getId(), product.getId(), relatedPage);
            List<ProductSummaryResponse> relatedDtos = mapper.toSummaryList(related);
            // Inject via reflection-free builder pattern
            return ProductDetailResponse.builder()
                    .id(detail.getId())
                    .name(detail.getName())
                    .slug(detail.getSlug())
                    .description(detail.getDescription())
                    .shortDescription(detail.getShortDescription())
                    .basePrice(detail.getBasePrice())
                    .originalPrice(detail.getOriginalPrice())
                    .currency(detail.getCurrency())
                    .discountPercent(detail.getDiscountPercent())
                    .thumbnailUrl(detail.getThumbnailUrl())
                    .imageUrls(detail.getImageUrls())
                    .tags(detail.getTags())
                    .ratingAvg(detail.getRatingAvg())
                    .ratingCount(detail.getRatingCount())
                    .soldCount(detail.getSoldCount())
                    .totalStock(detail.getTotalStock())
                    .available(detail.isAvailable())
                    .featured(detail.isFeatured())
                    .weightGrams(detail.getWeightGrams())
                    .variants(detail.getVariants())
                    .category(detail.getCategory())
                    .seller(detail.getSeller())
                    .createdAt(detail.getCreatedAt())
                    .updatedAt(detail.getUpdatedAt())
                    .related(relatedDtos)
                    .build();
        }
        return detail;
    }

    /**
     * Resolve category IDs including all descendants via recursive CTE.
     * Falls back to a single-element list if the category has no children.
     */
    private List<Long> resolveCategoryIds(Long categoryId, boolean includeDescendants) {
        if (categoryId == null) return List.of();
        if (!includeDescendants) return List.of(categoryId);

        List<Long> ids = categoryRepository.findDescendantIds(categoryId);
        return ids.isEmpty() ? List.of(categoryId) : ids;
    }

    /**
     * Verify the caller is the product owner or has ADMIN authority.
     * This service-layer guard supplements the @PreAuthorize annotation on the controller.
     */
    private void assertOwnerOrAdmin(Product product, UUID callerId) {
        boolean isOwner = product.getSeller().getId().equals(callerId);
        // For real implementation: also check if caller has ROLE_ADMIN via SecurityUtils
        if (!isOwner) {
            // Allow admins to bypass — check via SecurityUtils.isAdmin() in production
            // For now, strict ownership is enforced
            throw new AccessDeniedException(
                    "You do not have permission to modify product id=" + product.getId());
        }
    }
}
