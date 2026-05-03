package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.util.List;

public interface ChemicalExcelService {

    List<ChemicalRequestDTO> importChemicalsFromExcel(MultipartFile file);

    ByteArrayInputStream exportChemicalsToExcel();
}