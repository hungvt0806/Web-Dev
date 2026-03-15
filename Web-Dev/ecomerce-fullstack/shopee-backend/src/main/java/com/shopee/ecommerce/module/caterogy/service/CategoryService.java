package com.shopee.ecommerce.module.category.service;

import com.shopee.ecommerce.exception.ResourceAlreadyExistsException;
import com.shopee.ecommerce.exception.ResourceNotFoundException;
import com.shopee.ecommerce.module.category.dto.request.CreateCategoryRequest;
import com.shopee.ecommerce.module.category.dto.response.CategoryResponse;
import com.shopee.ecommerce.module.category.entity.Category;
import com.shopee.ecommerce.module.category.mapper.CategoryMapper;
import com.shopee.ecommerce.module.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Category service — hierarchical category management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper      categoryMapper;

    // ── Public reads ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CategoryResponse> getRootCategories() {
        return categoryMapper.toResponseList(categoryRepository.findRootCategories());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActive() {
        return categoryMapper.toResponseList(categoryRepository.findAllActive());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        return categoryMapper.toResponse(
                categoryRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id))
        );
    }

    @Transactional(readOnly = true)
    public CategoryResponse getBySlug(String slug) {
        return categoryMapper.toResponse(
                categoryRepository.findBySlug(slug)
                        .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug))
        );
    }

    /**
     * Build the full nested category tree.
     * Root categories → their children → grandchildren etc.
     */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getTree() {
        List<Category> roots = categoryRepository.findRootCategories();
        return roots.stream()
                .map(this::buildTreeNode)
                .collect(Collectors.toList());
    }

    // ── Admin mutations ───────────────────────────────────────────────────────

    @Transactional
    public CategoryResponse create(CreateCategoryRequest req) {
        if (categoryRepository.existsBySlug(req.getSlug())) {
            throw new ResourceAlreadyExistsException(
                    "Category with slug '" + req.getSlug() + "' already exists");
        }

        Category parent = null;
        if (req.getParentId() != null) {
            parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", req.getParentId()));
        }

        Category category = Category.builder()
                .name(req.getName())
                .slug(req.getSlug())
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .parent(parent)
                .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : (short) 0)
                .active(true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created: '{}' (id={})", saved.getName(), saved.getId());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CreateCategoryRequest req) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        if (StringUtils.hasText(req.getSlug()) &&
                categoryRepository.existsBySlugAndIdNot(req.getSlug(), id)) {
            throw new ResourceAlreadyExistsException(
                    "Category slug '" + req.getSlug() + "' is already taken");
        }

        if (StringUtils.hasText(req.getName()))  category.setName(req.getName());
        if (StringUtils.hasText(req.getSlug()))  category.setSlug(req.getSlug());
        if (req.getDescription() != null)        category.setDescription(req.getDescription());
        if (req.getImageUrl() != null)           category.setImageUrl(req.getImageUrl());
        if (req.getSortOrder() != null)          category.setSortOrder(req.getSortOrder());

        if (req.getParentId() != null) {
            Category parent = categoryRepository.findById(req.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category", "id", req.getParentId()));
            // Guard: a category cannot be its own ancestor
            if (parent.getId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            category.setParent(parent);
        }

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deactivate(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        category.setActive(false);
        categoryRepository.save(category);
        log.info("Category deactivated: id={}", id);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private CategoryResponse buildTreeNode(Category category) {
        List<Category> children = categoryRepository
                .findByParentIdAndActiveTrueOrderBySortOrderAsc(category.getId());

        List<CategoryResponse> childResponses = children.stream()
                .map(this::buildTreeNode)
                .collect(Collectors.toList());

        CategoryResponse response = categoryMapper.toTreeNode(category);
        // Inject children — requires a mutable response or using a builder
        return CategoryResponse.builder()
                .id(response.getId())
                .name(response.getName())
                .slug(response.getSlug())
                .description(response.getDescription())
                .imageUrl(response.getImageUrl())
                .sortOrder(response.getSortOrder())
                .active(response.isActive())
                .children(childResponses)
                .createdAt(response.getCreatedAt())
                .updatedAt(response.getUpdatedAt())
                .build();
    }
}
