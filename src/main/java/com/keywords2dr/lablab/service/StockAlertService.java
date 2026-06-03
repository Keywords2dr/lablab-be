package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.dto.stock.StockAlertResponse;
import com.keywords2dr.lablab.dto.stock.StockThresholdRequest;
import com.keywords2dr.lablab.dto.stock.StockThresholdResponse;

import java.util.List;
import java.util.UUID;

public interface StockAlertService {

    // ── Ngưỡng cảnh báo ───────────────────────────────────────────────────────

    StockThresholdResponse setThreshold(StockThresholdRequest request);

    void deleteThreshold(UUID itemId);

    List<StockThresholdResponse> getAllThresholds();

    // ── Cảnh báo tồn kho ──────────────────────────────────────────────────────

    List<StockAlertResponse> getStockAlerts();

    void checkAndNotifyLowStock(UUID itemId);
}