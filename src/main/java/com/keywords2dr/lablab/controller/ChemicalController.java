package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.chemical.ChemicalAdminResponse;
import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.entity.Chemical;
import com.keywords2dr.lablab.service.ChemicalExcelService;
import com.keywords2dr.lablab.service.ChemicalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

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

    @PostMapping
    public ResponseEntity<Chemical> createChemical(@Valid @RequestBody ChemicalRequestDTO request) {
        Chemical savedChemical = chemicalService.createChemical(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedChemical);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Chemical> updateChemical(@PathVariable UUID id, @Valid @RequestBody ChemicalRequestDTO request) {
        Chemical updatedChemical = chemicalService.updateChemical(id, request);
        return ResponseEntity.ok(updatedChemical);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteChemical(@PathVariable UUID id) {
        String resultMessage = chemicalService.deleteChemical(id);
        return ResponseEntity.ok(Map.of("message", resultMessage));
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> restoreChemical(@PathVariable UUID id) {
        chemicalService.restoreChemical(id);
        return ResponseEntity.ok(Map.of("message", "Hóa chất đã được khôi phục thành công!"));
    }

    @GetMapping("/trash")
    public ResponseEntity<List<ChemicalAdminResponse>> getDeletedChemicals() {
        List<ChemicalAdminResponse> list = chemicalService.getDeletedChemicalsForAdmin();
        return ResponseEntity.ok(list);
    }

    @GetMapping
    public ResponseEntity<List<ChemicalAdminResponse>> getAllForAdmin() {
        List<ChemicalAdminResponse> list = chemicalService.getAllChemicalsForAdmin();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChemicalAdminResponse>> searchChemicals(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String packaging,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String category) {

        List<ChemicalAdminResponse> result = chemicalService.filterChemicals(keyword, packaging, supplier, unit, category);
        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> importChemicals(@RequestParam("file") MultipartFile file) {
        List<ChemicalRequestDTO> dtoList = chemicalExcelService.importChemicalsFromExcel(file);

        int successCount = 0;
        int failCount = 0;

        // Vòng lặp lưu từng hóa chất vào Database
        for (ChemicalRequestDTO dto : dtoList) {
            try {
                chemicalService.createChemical(dto);
                successCount++;
            } catch (Exception e) {
                // Nếu dòng này lỗi (Trùng mã, thiếu trường...), in ra log và tăng biến failCount
                System.err.println("❌ Bỏ qua hóa chất [" + dto.getItemCode() + "] do lỗi: " + e.getMessage());
                failCount++;
            }
        }

        String message = String.format("Import hoàn tất! Thành công: %d. Thất bại (bỏ qua): %d", successCount, failCount);
        return ResponseEntity.ok(Map.of("message", message));
    }

    @GetMapping("/export")
    public ResponseEntity<Resource> exportChemicals() {
        String filename = "danh_sach_hoa_chat.xlsx";
        InputStreamResource file = new InputStreamResource(chemicalExcelService.exportChemicalsToExcel());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }
}