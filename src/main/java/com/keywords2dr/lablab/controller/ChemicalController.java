package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.chemical.ChemicalAdminResponse;
import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.dto.chemical.DeleteChemicalResponse;
import com.keywords2dr.lablab.service.ChemicalExcelService;
import com.keywords2dr.lablab.service.ChemicalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chemicals")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class ChemicalController {

    private final ChemicalService chemicalService;
    private final ChemicalExcelService chemicalExcelService;

    // CRUD APIs
    @PostMapping
    public ResponseEntity<ChemicalAdminResponse> createChemical(@Valid @RequestBody ChemicalRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chemicalService.createChemical(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChemicalAdminResponse> updateChemical(@PathVariable UUID id, @Valid @RequestBody ChemicalRequestDTO request) {
        return ResponseEntity.ok(chemicalService.updateChemical(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeleteChemicalResponse> deleteChemical(@PathVariable UUID id) {
        return ResponseEntity.ok(chemicalService.deleteChemical(id));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<Map<String, String>> restoreChemical(@PathVariable UUID id) {
        chemicalService.restoreChemical(id);
        return ResponseEntity.ok(Map.of("message", "Hóa chất đã được khôi phục thành công!"));
    }

    // GET APIs (Danh sách & Lọc)
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ChemicalAdminResponse>> getChemicals(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String packaging,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "itemCode") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ChemicalAdminResponse> result = chemicalService.filterChemicals(keyword, packaging, supplier, unit, category, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/trash")
    public ResponseEntity<List<ChemicalAdminResponse>> getDeletedChemicals() {
        return ResponseEntity.ok(chemicalService.getDeletedChemicalsForAdmin());
    }

    @GetMapping("/form-options")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, List<String>>> getChemicalFormOptions() {
        return ResponseEntity.ok(chemicalService.getChemicalFormOptions());
    }

    // EXCEL APIs

    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importChemicals(@RequestParam("file") MultipartFile file) {
        List<ChemicalRequestDTO> parsedDTOs = chemicalExcelService.parseChemicalsFromExcel(file);
        Map<String, Object> summary = chemicalService.processBatchImport(parsedDTOs, file.getOriginalFilename());
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> exportChemicals() {
        InputStreamResource file = new InputStreamResource(chemicalExcelService.exportChemicalsToExcel());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=danh_sach_hoa_chat.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}