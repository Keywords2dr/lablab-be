package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

public interface ChemicalExcelService {

    Map<String, Object> processAndSaveImport(MultipartFile file);

    List<ChemicalRequestDTO> parseChemicalsFromExcel(MultipartFile file);

    ByteArrayInputStream exportChemicalsToExcel();
}