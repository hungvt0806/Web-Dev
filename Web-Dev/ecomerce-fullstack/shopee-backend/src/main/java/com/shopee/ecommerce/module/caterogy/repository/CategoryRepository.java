package com.shopee.ecommerce.module.category.repository;

import com.shopee.ecommerce.module.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** All active root categories (no parent), ordered by sort_order. */
    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.active = TRUE ORDER BY c.sortOrder")
    List<Category> findRootCategories();

    /** Direct children of a given parent. */
    List<Category> findByParentIdAndActiveTrueOrderBySortOrderAsc(Long parentId);

    /** All active categories for a flat dropdown list. */
    @Query("SELECT c FROM Category c WHERE c.active = TRUE ORDER BY c.name")
    List<Category> findAllActive();

    /**
     * Collect all descendant IDs (self + children + grandchildren …)
     * using a PostgreSQL recursive CTE for the sub-category filter feature.
     *
     * Returns a flat list of category IDs for use in ProductRepository.findActiveByCategoryIds().
     */
    @Query(value = """
        WITH RECURSIVE descendants AS (
            SELECT id FROM categories WHERE id = :rootId
            UNION ALL
            SELECT c.id FROM categories c
            INNER JOIN descendants d ON c.parent_id = d.id
        )
        SELECT id FROM descendants
        """, nativeQuery = true)
    List<Long> findDescendantIds(@Param("rootId") Long rootId);
}
