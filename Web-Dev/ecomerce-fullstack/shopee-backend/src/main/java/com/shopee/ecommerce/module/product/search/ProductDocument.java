package com.shopee.ecommerce.module.product.search;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Elasticsearch document for full-text product search.
 *
 * Index design:
 * - name / shortDescription → analyzed with Japanese (kuromoji) + English
 * - tags                    → keyword (exact + aggregation)
 * - basePrice               → double (range queries)
 * - categorySlug            → keyword (filter)
 * - status                  → keyword (filter: ACTIVE only in public API)
 * - ratingAvg               → float  (sort)
 * - soldCount               → long   (popularity sort)
 */
@Document(indexName = "products", createIndex = true)
@Setting(settingPath = "/elasticsearch/product-settings.json")
@Mapping(mappingPath = "/elasticsearch/product-mapping.json")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDocument {

    @Id
    private String id;

    @Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard")
    private String name;

    @Field(type = FieldType.Keyword)
    private String slug;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String shortDescription;

    @Field(type = FieldType.Text, analyzer = "standard")
    private String description;

    @Field(type = FieldType.Double)
    private Double basePrice;

    @Field(type = FieldType.Double)
    private Double originalPrice;

    @Field(type = FieldType.Keyword)
    private String categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String categorySlug;

    @Field(type = FieldType.Keyword)
    private List<String> tags;

    @Field(type = FieldType.Float)
    private Float ratingAvg;

    @Field(type = FieldType.Integer)
    private Integer ratingCount;

    @Field(type = FieldType.Long)
    private Long soldCount;

    @Field(type = FieldType.Integer)
    private Integer totalStock;

    @Field(type = FieldType.Keyword)
    private String status; // ACTIVE | DRAFT | PAUSED

    @Field(type = FieldType.Boolean)
    private Boolean featured;

    @Field(type = FieldType.Keyword)
    private String thumbnailUrl;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant updatedAt;
}
