package com.keywords2dr.lablab.mapper;

import com.keywords2dr.lablab.dto.chemical.ChemicalAdminResponse;
import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.entity.Chemical;
import com.keywords2dr.lablab.exception.BadRequestException;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ChemicalMapper {

    @Mapping(target = "categoryType", constant = "CHEMICAL")
    @Mapping(target = "deleted", constant = "false")
    Chemical toEntity(ChemicalRequestDTO request);

    ChemicalAdminResponse toAdminResponse(Chemical chemical);

    @Mapping(target = "itemId",       ignore = true)
    @Mapping(target = "categoryType", ignore = true)
    @Mapping(target = "deleted",      ignore = true)
    void updateEntityFromDto(ChemicalRequestDTO dto, @MappingTarget Chemical entity);


    @AfterMapping
    default void validateCategoryTypeAfterUpdate(@MappingTarget Chemical entity) {
        if (entity.getCategoryType() == null || entity.getCategoryType().isBlank()) {
            throw new BadRequestException(
                    "Hóa chất này đang có categoryType = null (có thể do import lỗi trước đây). " +
                            "Vui lòng liên hệ Admin để fix trực tiếp trong database trước khi cập nhật."
            );
        }
    }
}