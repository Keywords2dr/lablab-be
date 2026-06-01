package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.entity.Chemical;
import com.keywords2dr.lablab.entity.Room;
import com.keywords2dr.lablab.entity.RoomInventory;
import com.keywords2dr.lablab.exception.BadRequestException;
import com.keywords2dr.lablab.exception.ConflictException;
import com.keywords2dr.lablab.exception.ResourceNotFoundException;
import com.keywords2dr.lablab.mapper.ChemicalMapper;
import com.keywords2dr.lablab.repository.ChemicalRepository;
import com.keywords2dr.lablab.repository.ItemRepository;
import com.keywords2dr.lablab.repository.RoomInventoryRepository;
import com.keywords2dr.lablab.repository.RoomRepository;
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.DataNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChemicalImportProcessor {

    private final ChemicalRepository chemicalRepository;
    private final ItemRepository itemRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final RoomRepository roomRepository;
    private final ChemicalMapper chemicalMapper;
    private final DataNormalizationService normalizationService;
    private final AuditLogService auditLogService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processSingleRow(ChemicalRequestDTO request) {
        normalizeChemicalRequest(request);

        // Thiếu các trường định danh → không thể match → tạo mới trực tiếp
        boolean hasFullKey = request.getAmountPerPackage() != null
                && StringUtils.hasText(request.getPackaging())
                && StringUtils.hasText(request.getSupplier());

        if (!hasFullKey) {
            createChemicalSkipDuplicateCheck(request);
            return true;
        }

        // Bước 1: exact match 4 trường
        Optional<Chemical> exactMatch = chemicalRepository.findExistingChemical(
                request.getName(),
                request.getSupplier(),
                request.getPackaging(),
                request.getAmountPerPackage());

        if (exactMatch.isPresent()) {
            addStockToExistingChemical(exactMatch.get(), request);
            return false;
        }

        // Bước 2: fallback theo name + supplier (khác packaging/amountPerPackage)
        Optional<Chemical> fallbackMatch = chemicalRepository
                .findFirstByNameIgnoreCaseAndSupplierIgnoreCaseAndIsDeletedFalse(
                        request.getName(), request.getSupplier());

        if (fallbackMatch.isPresent()) {
            log.info("[IMPORT] Fallback match — tên: [{}], NCC: [{}] → cộng dồn vào hóa chất id=[{}]",
                    request.getName(), request.getSupplier(), fallbackMatch.get().getItemId());
            addStockToExistingChemical(fallbackMatch.get(), request);
            return false;
        }

        // Bước 3: không tìm thấy gì → tạo mới
        createChemicalSkipDuplicateCheck(request);
        return true;
    }

    private void addStockToExistingChemical(Chemical chemical, ChemicalRequestDTO request) {
        if (request.getPackageCount() == null || request.getPackageCount() <= 0) {
            log.debug("[IMPORT] Bỏ qua cộng dồn — không có số lượng cho hóa chất [{}]", chemical.getName());
            return;
        }
        if (!StringUtils.hasText(request.getRoomName())) {
            throw new BadRequestException(String.format(
                    "Hóa chất [%s] (id=%s) đã tồn tại — vui lòng chỉ định phòng lưu chứa để bổ sung số lượng!",
                    chemical.getName(), chemical.getItemId()));
        }

        Room room = findRoomByName(request.getRoomName());

        BigDecimal effectiveAmountPerPackage = chemical.getAmountPerPackage() != null
                ? chemical.getAmountPerPackage()
                : (request.getAmountPerPackage() != null ? request.getAmountPerPackage() : BigDecimal.ZERO);

        BigDecimal addedQty = effectiveAmountPerPackage.multiply(new BigDecimal(request.getPackageCount()));

        RoomInventory inventory = roomInventoryRepository
                .findByItem_ItemIdAndRoom_RoomId(chemical.getItemId(), room.getRoomId())
                .orElseGet(() -> RoomInventory.builder()
                        .room(room)
                        .item(chemical)
                        .totalQuantity(BigDecimal.ZERO)
                        .lockedQuantity(BigDecimal.ZERO)
                        .packageCount(0)
                        .build());

        inventory.setTotalQuantity(inventory.getTotalQuantity().add(addedQty));
        inventory.setPackageCount(inventory.getPackageCount() + request.getPackageCount());
        roomInventoryRepository.save(inventory);

        log.info("[IMPORT] Cộng dồn — hóa chất: [{}] (id={}), phòng: [{}], +{} chai ({} {})",
                chemical.getName(), chemical.getItemId(),
                room.getRoomName(),
                request.getPackageCount(), addedQty, chemical.getUnit());

        auditLogService.logAction("IMPORT_ADD_STOCK", "CHEMICAL", chemical.getItemId(), null,
                Map.of(
                        "chemicalName",   chemical.getName(),
                        "chemicalId",     chemical.getItemId().toString(),
                        "room",           room.getRoomName(),
                        "addedPackages",  request.getPackageCount(),
                        "addedQty",       addedQty,
                        "unit",           chemical.getUnit() != null ? chemical.getUnit() : ""
                ));
    }

    private void createChemicalSkipDuplicateCheck(ChemicalRequestDTO request) {
        validateItemCodeUniqueness(request.getItemCode());

        Chemical chemical = chemicalMapper.toEntity(request);
        Chemical savedChemical = chemicalRepository.save(chemical);

        if (request.getPackageCount() != null && request.getPackageCount() > 0
                && StringUtils.hasText(request.getRoomName())) {
            Room room = findRoomByName(request.getRoomName());
            BigDecimal amountPerPackage = request.getAmountPerPackage() != null
                    ? request.getAmountPerPackage()
                    : BigDecimal.ZERO;
            BigDecimal totalQuantity = amountPerPackage.multiply(new BigDecimal(request.getPackageCount()));

            roomInventoryRepository.save(RoomInventory.builder()
                    .room(room)
                    .item(savedChemical)
                    .totalQuantity(totalQuantity)
                    .lockedQuantity(BigDecimal.ZERO)
                    .packageCount(request.getPackageCount())
                    .build());
        }

        log.info("[IMPORT] Tạo mới hóa chất: [{}] (mã: {})", savedChemical.getName(), savedChemical.getItemCode());
        auditLogService.logAction("CREATE", "CHEMICAL", savedChemical.getItemId(),
                null, chemicalMapper.toAdminResponse(savedChemical));
    }

    private void normalizeChemicalRequest(ChemicalRequestDTO request) {
        request.setPackaging(normalizationService.normalizeAndLearn(request.getPackaging(), "PACKAGING"));
        request.setUnit(normalizationService.normalizeAndLearn(request.getUnit(), "UNIT"));
    }

    private void validateItemCodeUniqueness(String itemCode) {
        if (itemRepository.existsByItemCode(itemCode)) {
            throw new ConflictException("Mã hóa chất [" + itemCode + "] đã tồn tại!");
        }
    }

    private Room findRoomByName(String roomName) {
        return roomRepository.findByRoomNameIgnoreCase(roomName.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Phòng Lab khớp với tên: " + roomName));
    }
}