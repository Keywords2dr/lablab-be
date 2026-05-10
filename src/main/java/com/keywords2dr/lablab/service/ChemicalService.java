package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.chemical.ChemicalAdminResponse;
import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.dto.chemical.DeleteChemicalResponse;
import com.keywords2dr.lablab.entity.Chemical;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ChemicalService {
    ChemicalAdminResponse createChemical(ChemicalRequestDTO request);
    ChemicalAdminResponse updateChemical(UUID id, ChemicalRequestDTO request);
    DeleteChemicalResponse deleteChemical(UUID id);
    void restoreChemical(UUID id);
    List<ChemicalAdminResponse> getDeletedChemicalsForAdmin();
    Map<String, List<String>> getChemicalFormOptions();

    Page<ChemicalAdminResponse> filterChemicals(String keyword, String packaging, String supplier, String unit, String category, Pageable pageable);
    Map<String, Object> processBatchImport(List<ChemicalRequestDTO> dtoList, String fileName);
}