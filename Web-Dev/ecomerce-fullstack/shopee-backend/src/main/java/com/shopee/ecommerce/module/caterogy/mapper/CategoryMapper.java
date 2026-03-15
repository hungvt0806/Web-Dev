package com.shopee.ecommerce.module.category.mapper;

import com.shopee.ecommerce.module.category.dto.response.CategoryResponse;
import com.shopee.ecommerce.module.category.entity.Category;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Named("toResponse")
    @Mapping(target = "parent.id",   source = "parent.id")
    @Mapping(target = "parent.name", source = "parent.name")
    @Mapping(target = "parent.slug", source = "parent.slug")
    @Mapping(target = "children",    ignore = true)  // set ở service khi build tree
    @Mapping(target = "productCount",ignore = true)  // chỉ dùng cho admin
    CategoryResponse toResponse(Category category);

    @IterableMapping(qualifiedByName = "toResponse")
    List<CategoryResponse> toResponseList(List<Category> categories);

    /**
     * Build category tree node
     */
    @Named("toTreeNode")
    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "productCount", ignore = true)
    CategoryResponse toTreeNode(Category category);
}