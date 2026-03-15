package com.shopee.ecommerce.module.product.mapper;

import com.shopee.ecommerce.module.product.dto.response.AdminProductResponse;
import com.shopee.ecommerce.module.product.dto.response.ProductDetailResponse;
import com.shopee.ecommerce.module.product.dto.response.ProductSummaryResponse;
import com.shopee.ecommerce.module.product.entity.Product;
import com.shopee.ecommerce.module.product.entity.ProductVariant;
import org.mapstruct.*;

import java.util.List;

/**
 * MapStruct mapper — entity → DTO conversions.
 *
 * MapStruct generates the implementation at compile time.
 * All mappings are explicit so there are no surprise hidden fields.
 *
 * componentModel = "spring" → the generated impl is a Spring @Component,
 * injectable via @Autowired / constructor injection.
 */
@Mapper(
        componentModel         = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy   = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    // ── Summary (catalog card) ────────────────────────────────────────────────

    @Mapping(target = "discountPercent", expression = "java(product.getDiscountPercent())")
    @Mapping(target = "available",       expression = "java(product.isAvailable())")
    @Mapping(target = "category.id",     source = "product.category.id")
    @Mapping(target = "category.name",   source = "product.category.name")
    @Mapping(target = "category.slug",   source = "product.category.slug")
    ProductSummaryResponse toSummary(Product product);

    List<ProductSummaryResponse> toSummaryList(List<Product> products);

    // ── Detail (product page) ─────────────────────────────────────────────────

    @Mapping(target = "discountPercent",  expression = "java(product.getDiscountPercent())")
    @Mapping(target = "available",        expression = "java(product.isAvailable())")
    @Mapping(target = "category.id",      source = "product.category.id")
    @Mapping(target = "category.name",    source = "product.category.name")
    @Mapping(target = "category.slug",    source = "product.category.slug")
    @Mapping(target = "seller.id",        source = "product.seller.id")
    @Mapping(target = "seller.shopName",  source = "product.seller.fullName")  // User has fullName, not shopName
    @Mapping(target = "seller.avatarUrl", source = "product.seller.avatarUrl")
    @Mapping(target = "related",          ignore = true)  // populated separately in service
    ProductDetailResponse toDetail(Product product);

    // ── Variant ───────────────────────────────────────────────────────────────

    @Mapping(target = "inStock", expression = "java(variant.isInStock())")
    ProductDetailResponse.VariantResponse toVariantResponse(ProductVariant variant);

    List<ProductDetailResponse.VariantResponse> toVariantResponseList(List<ProductVariant> variants);

    // ── Admin detail ──────────────────────────────────────────────────────────

    @Mapping(target = "category.id",      source = "product.category.id")
    @Mapping(target = "category.name",    source = "product.category.name")
    @Mapping(target = "category.slug",    source = "product.category.slug")
    @Mapping(target = "seller.id",        source = "product.seller.id")
    @Mapping(target = "seller.shopName",  source = "product.seller.fullName")  // User has fullName, not shopName
    @Mapping(target = "seller.avatarUrl", source = "product.seller.avatarUrl")
    AdminProductResponse toAdminResponse(Product product);
}