package org.example.micro.mapper;

import org.example.micro.dto.request.CategoryCreateRequest;
import org.example.micro.dto.request.CategoryUpdateRequest;
import org.example.micro.dto.response.CategoryResponse;
import org.example.micro.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CategoryMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    Category toEntity(CategoryCreateRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    void updateEntity(CategoryUpdateRequest request, @MappingTarget Category category);

    @Mapping(target = "parentId", source = "parentId.id")
    CategoryResponse toResponse(Category category);
}
