package com.shopee.ecommerce.module.product.search;

import com.shopee.ecommerce.infrastructure.cache.CacheNames;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch._types.SortOrder;
import org.springframework.stereotype.Service;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Full-text search service backed by Elasticsearch.
 *
 * Search strategy:
 * 1. multi_match across name (^3 boost), shortDescription (^2), description, tags
 * 2. Filter by category, price range, status
 * 3. Sort: relevance / price / rating / popularity
 * 4. Results cached in Redis (2 min TTL)
 *
 * Index maintenance:
 * - ProductIndexingService calls indexProduct() on create/update
 * - deleteProduct() on soft-delete / status change to DELETED
 */
@Slf4j
@Service
public class ProductSearchService {

    private final ElasticsearchOperations esOps;
    private final ProductSearchRepository searchRepository;

    public ProductSearchService(
            ElasticsearchOperations esOps,
            @Autowired(required = false) ProductSearchRepository searchRepository) {
        this.esOps = esOps;
        this.searchRepository = searchRepository;
    }
    // ── Search ─────────────────────────────────────────────────

    @Cacheable(value = CacheNames.SEARCH_RESULTS, key = "#req.q + ':' + #req.category + ':' + #req.minPrice + ':' + #req.maxPrice + ':' + #req.sort + ':' + #req.page + ':' + #req.size")
    public ProductSearchResult search(ProductSearchRequest req) {
        List<Query> must    = new ArrayList<>();
        List<Query> filters = new ArrayList<>();

        // ── Full-text query ────────────────────────────────────
        if (req.getQ() != null && !req.getQ().isBlank()) {
            must.add(Query.of(q -> q.multiMatch(mm -> mm
                .query(req.getQ())
                .fields("name^3", "shortDescription^2", "description", "tags")
                .type(TextQueryType.BestFields)
                .fuzziness("AUTO")
                .prefixLength(2)
            )));
        } else {
            must.add(Query.of(q -> q.matchAll(m -> m)));
        }

        // ── Filters ────────────────────────────────────────────
        if (req.isActiveOnly()) {
            filters.add(Query.of(q -> q.term(t -> t.field("status").value("ACTIVE"))));
        }
        if (req.getCategory() != null && !req.getCategory().isBlank()) {
            filters.add(Query.of(q -> q.term(t -> t.field("categorySlug").value(req.getCategory()))));
        }
        if (req.getMinPrice() != null || req.getMaxPrice() != null) {
            filters.add(Query.of(q -> q.range(r -> {
                r.field("basePrice");
                if (req.getMinPrice() != null) r.gte(co.elastic.clients.json.JsonData.of(req.getMinPrice()));
                if (req.getMaxPrice() != null) r.lte(co.elastic.clients.json.JsonData.of(req.getMaxPrice()));
                return r;
            })));
        }

        Query boolQuery = Query.of(q -> q.bool(b -> b.must(must).filter(filters)));

        // ── Sort ───────────────────────────────────────────────
        NativeQuery nativeQuery = NativeQuery.builder()
            .withQuery(boolQuery)
            .withSort(buildSort(req.getSort()))
            .withPageable(PageRequest.of(req.getPage(), Math.min(req.getSize(), 100)))
            .build();

        SearchHits<ProductDocument> hits = esOps.search(nativeQuery, ProductDocument.class);
        long total = hits.getTotalHits();

        List<ProductDocument> docs = hits.getSearchHits().stream()
            .map(h -> h.getContent())
            .toList();

        log.debug("ES search q='{}' hits={}", req.getQ(), total);
        return new ProductSearchResult(docs, total, req.getPage(), req.getSize());
    }

    // ── Indexing ───────────────────────────────────────────────

    public void indexProduct(ProductDocument doc) {
        searchRepository.save(doc);
        log.debug("Indexed product id={} slug={}", doc.getId(), doc.getSlug());
    }

    public void deleteFromIndex(String productId) {
        searchRepository.deleteById(productId);
        log.debug("Removed product {} from index", productId);
    }

    // ── Suggestions / Autocomplete ─────────────────────────────

    public List<String> suggest(String prefix) {
        if (prefix == null || prefix.length() < 2) return List.of();

        Query query = Query.of(q -> q.matchPhrasePrefix(m -> m
            .field("name")
            .query(prefix)
        ));

        NativeQuery nq = NativeQuery.builder()
            .withQuery(query)
            .withSourceFilter(new FetchSourceFilterBuilder().withIncludes("name").build())
            .withPageable(PageRequest.of(0, 8))
            .build();

        return esOps.search(nq, ProductDocument.class)
            .getSearchHits().stream()
            .map(h -> h.getContent().getName())
            .distinct()
            .toList();
    }

    // ── Private helpers ────────────────────────────────────────

    private List<co.elastic.clients.elasticsearch._types.SortOptions> buildSort(String sort) {
        return switch (sort == null ? "relevance" : sort) {
            case "price_asc"  -> List.of(sortField("basePrice",  SortOrder.Asc));
            case "price_desc" -> List.of(sortField("basePrice",  SortOrder.Desc));
            case "rating"     -> List.of(sortField("ratingAvg",  SortOrder.Desc));
            case "newest"     -> List.of(sortField("createdAt",  SortOrder.Desc));
            case "popular"    -> List.of(sortField("soldCount",  SortOrder.Desc));
            default           -> List.of(); // ES relevance (_score)
        };
    }

    private co.elastic.clients.elasticsearch._types.SortOptions sortField(String field, SortOrder order) {
        return co.elastic.clients.elasticsearch._types.SortOptions.of(s ->
            s.field(f -> f.field(field).order(order))
        );
    }
}
