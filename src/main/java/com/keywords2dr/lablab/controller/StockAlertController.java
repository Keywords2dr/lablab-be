package com.keywords2dr.lablab.controller;

import com.keywords2dr.lablab.dto.stock.StockAlertResponse;
import com.keywords2dr.lablab.dto.stock.StockThresholdRequest;
import com.keywords2dr.lablab.dto.stock.StockThresholdResponse;
import com.keywords2dr.lablab.service.StockAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/stock-alerts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class StockAlertController {

    private final StockAlertService stockAlertService;

    // ── Cảnh báo tồn kho ──────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<StockAlertResponse>> getStockAlerts() {
        return ResponseEntity.ok(stockAlertService.getStockAlerts());
    }

    // ── Ngưỡng cảnh báo ───────────────────────────────────────────────────────

    @GetMapping("/thresholds")
    public ResponseEntity<List<StockThresholdResponse>> getAllThresholds() {
        return ResponseEntity.ok(stockAlertService.getAllThresholds());
    }

    @PostMapping("/thresholds")
    public ResponseEntity<StockThresholdResponse> setThreshold(
            @Valid @RequestBody StockThresholdRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockAlertService.setThreshold(request));
    }

    @DeleteMapping("/thresholds/{itemId}")
    public ResponseEntity<Map<String, String>> deleteThreshold(@PathVariable UUID itemId) {
        stockAlertService.deleteThreshold(itemId);
        return ResponseEntity.ok(Map.of("message", "Đã xóa ngưỡng cảnh báo thành công!"));
    }
}