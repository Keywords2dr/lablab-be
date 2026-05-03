package com.keywords2dr.lablab.mapper;

import com.keywords2dr.lablab.dto.chemical.ChemicalAdminResponse;
import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.entity.Chemical;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ChemicalMapper {

    @Mapping(target = "categoryType", constant = "CHEMICAL")
    @Mapping(target = "deleted", constant = "false")
    Chemical toEntity(ChemicalRequestDTO request);

    ChemicalAdminResponse toAdminResponse(Chemical chemical);

    @Mapping(target = "itemId", ignore = true)
    @Mapping(target = "categoryType", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromDto(ChemicalRequestDTO dto, @MappingTarget Chemical entity);
}