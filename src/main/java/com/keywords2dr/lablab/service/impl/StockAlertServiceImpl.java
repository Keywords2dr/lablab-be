package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.stock.StockAlertResponse;
import com.keywords2dr.lablab.dto.stock.StockAlertResponse.AlertLevel;
import com.keywords2dr.lablab.dto.stock.StockAlertResponse.RoomStock;
import com.keywords2dr.lablab.dto.stock.StockThresholdRequest;
import com.keywords2dr.lablab.dto.stock.StockThresholdResponse;
import com.keywords2dr.lablab.entity.Item;
import com.keywords2dr.lablab.entity.RoomInventory;
import com.keywords2dr.lablab.entity.StockThreshold;
import com.keywords2dr.lablab.event.NotificationEvent;
import com.keywords2dr.lablab.exception.ConflictException;
import com.keywords2dr.lablab.exception.ResourceNotFoundException;
import com.keywords2dr.lablab.repository.ItemRepository;
import com.keywords2dr.lablab.repository.RoomInventoryRepository;
import com.keywords2dr.lablab.repository.StockThresholdRepository;
import com.keywords2dr.lablab.repository.UserRepository;
import com.keywords2dr.lablab.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockAlertServiceImpl implements StockAlertService {

    private final StockThresholdRepository thresholdRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public StockThresholdResponse setThreshold(StockThresholdRequest request) {
        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hóa chất với ID: " + request.getItemId()));

        StockThreshold threshold = thresholdRepository
                .findByItem_ItemId(request.getItemId())
                .orElse(StockThreshold.builder().item(item).build());

        threshold.setMinQuantity(request.getMinQuantity());
        threshold.setNote(request.getNote());

        StockThreshold saved = thresholdRepository.save(threshold);
        log.info("Đã cài ngưỡng tồn kho cho hóa chất [{}]: min={}{}",
                item.getName(), request.getMinQuantity(), item.getUnit());

        return toThresholdResponse(saved);
    }

    @Override
    @Transactional
    public void deleteThreshold(UUID itemId) {
        StockThreshold threshold = thresholdRepository.findByItem_ItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chưa cài ngưỡng cảnh báo cho hóa chất này!"));
        thresholdRepository.delete(threshold);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockThresholdResponse> getAllThresholds() {
        return thresholdRepository.findAllWithItem()
                .stream()
                .map(this::toThresholdResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAlertResponse> getStockAlerts() {
        List<StockThreshold> thresholds = thresholdRepository.findAllWithItem();

        return thresholds.stream()
                .map(threshold -> {
                    UUID itemId = threshold.getItem().getItemId();

                    List<RoomInventory> inventories =
                            roomInventoryRepository.findAllByItem_ItemId(itemId);

                    BigDecimal grandTotal = inventories.stream()
                            .map(RoomInventory::getTotalQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    AlertLevel level = resolveAlertLevel(grandTotal, threshold.getMinQuantity());

                    if (level == null) return null;

                    List<RoomStock> roomStocks = inventories.stream()
                            .map(inv -> RoomStock.builder()
                                    .roomId(inv.getRoom().getRoomId())
                                    .roomName(inv.getRoom().getRoomName())
                                    .quantity(inv.getTotalQuantity())
                                    .lockedQuantity(inv.getLockedQuantity())
                                    .availableQuantity(
                                            inv.getTotalQuantity().subtract(inv.getLockedQuantity()))
                                    .build())
                            .collect(Collectors.toList());

                    return StockAlertResponse.builder()
                            .itemId(itemId)
                            .itemCode(threshold.getItem().getItemCode())
                            .itemName(threshold.getItem().getName())
                            .unit(threshold.getItem().getUnit())
                            .totalQuantity(grandTotal)
                            .minQuantity(threshold.getMinQuantity())
                            .alertLevel(level)
                            .roomStocks(roomStocks)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted((a, b) -> {
                    if (a.getAlertLevel() != b.getAlertLevel()) {
                        return a.getAlertLevel() == AlertLevel.OUT_OF_STOCK ? -1 : 1;
                    }
                    return a.getItemName().compareToIgnoreCase(b.getItemName());
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void checkAndNotifyLowStock(UUID itemId) {
        Optional<StockThreshold> opt = thresholdRepository.findByItem_ItemId(itemId);
        if (opt.isEmpty()) return;

        StockThreshold threshold = opt.get();
        Item item = threshold.getItem();

        List<RoomInventory> inventories = roomInventoryRepository.findAllByItem_ItemId(itemId);
        BigDecimal grandTotal = inventories.stream()
                .map(RoomInventory::getTotalQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AlertLevel level = resolveAlertLevel(grandTotal, threshold.getMinQuantity());
        if (level == null) return;

        String title;
        String message;

        if (level == AlertLevel.OUT_OF_STOCK) {
            title = "Hóa chất hết hàng!";
            message = String.format(
                    "Hóa chất [%s] (%s) đã HẾT HÀNG toàn hệ thống. Vui lòng nhập thêm ngay!",
                    item.getName(), item.getItemCode());
        } else {
            title = "Hóa chất sắp hết hàng";
            message = String.format(
                    "Hóa chất [%s] (%s) còn %.2f %s, dưới ngưỡng cảnh báo %.2f %s. Cần nhập thêm!",
                    item.getName(), item.getItemCode(),
                    grandTotal, item.getUnit(),
                    threshold.getMinQuantity(), item.getUnit());
        }

        String notificationType = level == AlertLevel.OUT_OF_STOCK
                ? "STOCK_OUT" : "STOCK_LOW";

        // Gửi notification cho Admin
        notifyAllAdmins(title, message, notificationType);

        log.warn("[STOCK ALERT] {} — hóa chất: [{}], tồn kho: {} {}, ngưỡng: {} {}",
                level, item.getName(), grandTotal, item.getUnit(),
                threshold.getMinQuantity(), item.getUnit());
    }

    // PRIVATE HELPERS
    private AlertLevel resolveAlertLevel(BigDecimal totalQuantity, BigDecimal minQuantity) {
        if (totalQuantity.compareTo(BigDecimal.ZERO) <= 0) {
            return AlertLevel.OUT_OF_STOCK;
        }
        if (totalQuantity.compareTo(minQuantity) < 0) {
            return AlertLevel.LOW_STOCK;
        }
        return null; // Bình thường
    }

    private void notifyAllAdmins(String title, String message, String type) {
        userRepository.findAllByRole("ADMIN").forEach(admin ->
                eventPublisher.publishEvent(
                        new NotificationEvent(admin.getUserId(), title, message, type)));
    }

    private StockThresholdResponse toThresholdResponse(StockThreshold threshold) {
        StockThresholdResponse res = new StockThresholdResponse();
        res.setId(threshold.getId());
        res.setItemId(threshold.getItem().getItemId());
        res.setItemCode(threshold.getItem().getItemCode());
        res.setItemName(threshold.getItem().getName());
        res.setUnit(threshold.getItem().getUnit());
        res.setMinQuantity(threshold.getMinQuantity());
        res.setNote(threshold.getNote());
        res.setCreatedAt(threshold.getCreatedAt());
        res.setUpdatedAt(threshold.getUpdatedAt());
        return res;
    }
}