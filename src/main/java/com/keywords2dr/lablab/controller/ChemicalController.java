package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.chemical.ChemicalAdminResponse;
import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.entity.Chemical;
import com.keywords2dr.lablab.service.ChemicalExcelService;
import com.keywords2dr.lablab.service.ChemicalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    // ĐÃ NÂNG CẤP: Nhận thêm param phân trang và sắp xếp
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ChemicalAdminResponse>> getChemicals(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String packaging,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String unit,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page, // Trang mặc định là 0
            @RequestParam(defaultValue = "10") int size, // Mặc định lấy 10 item 1 trang
            @RequestParam(defaultValue = "itemCode") String sortBy, // Sắp xếp mặc định theo mã
            @RequestParam(defaultValue = "asc") String sortDir // Hướng sắp xếp mặc định là tăng dần
    ) {

        // 1. Tạo đối tượng Sort
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        // 2. Gói trang, kích thước và cách sắp xếp vào PageRequest
        Pageable pageable = PageRequest.of(page, size, sort);

        // 3. Gọi Service và truyền pageable vào
        Page<ChemicalAdminResponse> result = chemicalService.filterChemicals(keyword, packaging, supplier, unit, category, pageable);

        return ResponseEntity.ok(result);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> importChemicals(@RequestParam("file") MultipartFile file) {
        Map<String, String> result = chemicalExcelService.processAndSaveImport(file);
        return ResponseEntity.ok(result);
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