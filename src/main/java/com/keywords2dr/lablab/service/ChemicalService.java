package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.chemical.ChemicalAdminResponse;
import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.entity.Chemical;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ChemicalService {
    Chemical createChemical(ChemicalRequestDTO request);
    Chemical updateChemical(UUID id, ChemicalRequestDTO request);
    String deleteChemical(UUID id);
    void restoreChemical(UUID id);
    List<ChemicalAdminResponse> getDeletedChemicalsForAdmin();

    Page<ChemicalAdminResponse> filterChemicals(String keyword, String packaging, String supplier, String unit, String category, Pageable pageable);
}