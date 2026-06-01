package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.chemical.ChemicalAdminResponse;
import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.dto.chemical.DeleteChemicalResponse;
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
import com.keywords2dr.lablab.repository.specification.ChemicalSpecification;
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.ChemicalService;
import com.keywords2dr.lablab.service.DataNormalizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChemicalServiceImpl implements ChemicalService {

    private final ChemicalRepository chemicalRepository;
    private final ItemRepository itemRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final RoomRepository roomRepository;
    private final ChemicalMapper chemicalMapper;
    private final DataNormalizationService normalizationService;
    private final AuditLogService auditLogService;

    // ==================== CRUD ====================

    @Override
    @Transactional
    public ChemicalAdminResponse createChemical(ChemicalRequestDTO request) {
        validateItemCodeUniqueness(request.getItemCode());
        checkDuplicateChemical(request.getName(), request.getSupplier());

        if (request.getPackageCount() != null && request.getPackageCount() > 0
                && !StringUtils.hasText(request.getRoomName())) {
            throw new BadRequestException(
                    "Vui lòng chọn phòng lưu chứa khi nhập số lượng hóa chất ban đầu!");
        }

        normalizeChemicalRequest(request);
        Chemical chemical = chemicalMapper.toEntity(request);
        Chemical savedChemical = chemicalRepository.save(chemical);

        if (request.getPackageCount() != null && request.getPackageCount() > 0
                && StringUtils.hasText(request.getRoomName())) {
            Room room = findRoomByName(request.getRoomName());
            BigDecimal totalQuantity = request.getAmountPerPackage()
                    .multiply(new BigDecimal(request.getPackageCount()));

            RoomInventory inventory = RoomInventory.builder()
                    .room(room)
                    .item(savedChemical)
                    .totalQuantity(totalQuantity)
                    .lockedQuantity(BigDecimal.ZERO)
                    .packageCount(request.getPackageCount())
                    .build();
            roomInventoryRepository.save(inventory);
        }

        ChemicalAdminResponse responseDTO = chemicalMapper.toAdminResponse(savedChemical);
        auditLogService.logAction("CREATE", "CHEMICAL", savedChemical.getItemId(), null, responseDTO);
        return responseDTO;
    }

    @Override
    @Transactional
    public ChemicalAdminResponse updateChemical(UUID id, ChemicalRequestDTO request) {
        Chemical chemical = findChemicalById(id);

        if (!chemical.getItemCode().equals(request.getItemCode())) {
            validateItemCodeUniqueness(request.getItemCode());
        }
        if (!chemical.getUnit().equals(request.getUnit())
                && roomInventoryRepository.existsByItem_ItemIdAndTotalQuantityGreaterThan(id, BigDecimal.ZERO)) {
            throw new BadRequestException(
                    "Không thể đổi Đơn vị tính! Hóa chất này đang còn tồn kho thực tế. " +
                            "Vui lòng thu hồi về 0 trước khi đổi.");
        }

        normalizeChemicalRequest(request);
        ChemicalAdminResponse oldState = chemicalMapper.toAdminResponse(chemical);
        chemicalMapper.updateEntityFromDto(request, chemical);
        Chemical updatedChemical = chemicalRepository.save(chemical);

        ChemicalAdminResponse newState = chemicalMapper.toAdminResponse(updatedChemical);
        auditLogService.logAction("UPDATE", "CHEMICAL", updatedChemical.getItemId(), oldState, newState);
        return newState;
    }

    @Override
    @Transactional
    public DeleteChemicalResponse deleteChemical(UUID id) {
        Chemical chemical = findChemicalById(id);
        List<RoomInventory> activeInventories = getInventoriesWithStock(id);
        ChemicalAdminResponse oldState = chemicalMapper.toAdminResponse(chemical);

        chemicalRepository.softDeleteById(id);
        auditLogService.logAction("DELETE", "CHEMICAL", id, oldState, null);

        if (activeInventories.isEmpty()) {
            return DeleteChemicalResponse.builder()
                    .message("Đã xóa (ẩn) hóa chất thành công.")
                    .hasActiveInventory(false)
                    .affectedRooms(List.of())
                    .build();
        }

        List<DeleteChemicalResponse.AffectedRoom> affectedRooms = activeInventories.stream()
                .map(inv -> DeleteChemicalResponse.AffectedRoom.builder()
                        .roomName(inv.getRoom().getRoomName())
                        .remainingQuantity(inv.getTotalQuantity())
                        .unit(chemical.getUnit())
                        .build())
                .collect(Collectors.toList());

        return DeleteChemicalResponse.builder()
                .message("Đã ẩn hóa chất khỏi hệ thống. LƯU Ý: Cần thực hiện thu hồi thực tế tại các phòng bên dưới.")
                .hasActiveInventory(true)
                .affectedRooms(affectedRooms)
                .build();
    }

    @Override
    @Transactional
    public void restoreChemical(UUID id) {
        if (chemicalRepository.restoreById(id) == 0) {
            throw new ResourceNotFoundException("Không tìm thấy hóa chất bị ẩn để khôi phục!");
        }

        Chemical restoredChemical = chemicalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lỗi sau khi khôi phục: không đọc được hóa chất!"));
        auditLogService.logAction("RESTORE", "CHEMICAL", id, null, chemicalMapper.toAdminResponse(restoredChemical));
    }

    // ==================== QUERY ====================

    @Override
    @Transactional(readOnly = true)
    public Page<ChemicalAdminResponse> filterChemicals(
            String keyword, String packaging, String supplier,
            String unit, String category, Pageable pageable) {
        Specification<Chemical> spec = ChemicalSpecification.filter(
                cleanSearchParam(keyword), cleanSearchParam(packaging),
                cleanSearchParam(supplier), cleanSearchParam(unit), cleanSearchParam(category));
        return chemicalRepository.findAll(spec, pageable).map(chemicalMapper::toAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChemicalAdminResponse> getDeletedChemicalsForAdmin() {
        return chemicalRepository.findDeletedChemicals().stream()
                .map(chemicalMapper::toAdminResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getChemicalFormOptions() {
        List<String> processedSuppliers = chemicalRepository.findDistinctSuppliers().stream()
                .filter(StringUtils::hasText)
                .flatMap(s -> Arrays.stream(s.split("[,/\\-]")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();

        return Map.of(
                "packagings", chemicalRepository.findDistinctPackagings(),
                "units", chemicalRepository.findDistinctUnits(),
                "suppliers", processedSuppliers
        );
    }

    // ==================== EXCEL IMPORT ====================

    /**
     * Xử lý batch import từ Excel. Mỗi dòng chạy trong transaction riêng —
     * lỗi 1 dòng không rollback cả batch.
     */
    @Override
    public Map<String, Object> processBatchImport(List<ChemicalRequestDTO> dtoList, String fileName) {
        int newCount = 0;
        int updatedCount = 0;
        List<Map<String, String>> failures = new ArrayList<>();

        for (ChemicalRequestDTO dto : dtoList) {
            try {
                boolean isNew = createOrUpdateChemicalInNewTransaction(dto);
                if (isNew) newCount++;
                else updatedCount++;
            } catch (Exception e) {
                log.warn("[IMPORT] Bỏ qua dòng — tên: [{}], mã: [{}], lý do: {}",
                        dto.getName(), dto.getItemCode(), e.getMessage());
                failures.add(Map.of(
                        "itemCode", dto.getItemCode() != null ? dto.getItemCode() : "(không có mã)",
                        "name",     dto.getName()     != null ? dto.getName()     : "(không có tên)",
                        "reason",   e.getMessage()    != null ? e.getMessage()    : e.getClass().getSimpleName()
                ));
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("fileName",        fileName);
        summary.put("totalRowsParsed", dtoList.size());
        summary.put("newCount",        newCount);
        summary.put("updatedCount",    updatedCount);
        summary.put("failCount",       failures.size());
        summary.put("message", String.format(
                "Import hoàn tất! Tạo mới: %d. Bổ sung số lượng: %d. Thất bại (bỏ qua): %d",
                newCount, updatedCount, failures.size()));
        if (!failures.isEmpty()) summary.put("failures", failures);

        auditLogService.logAction("IMPORT_EXCEL", "CHEMICAL_IMPORT", null, null, summary);
        return summary;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean createOrUpdateChemicalInNewTransaction(ChemicalRequestDTO request) {
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

    /**
     * Cộng dồn số lượng cho hóa chất đã tồn tại.
     * Tạo mới RoomInventory nếu phòng đó chưa có record.
     */
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

    /**
     * Tạo hóa chất mới, bỏ qua kiểm tra duplicate (đã xác nhận không trùng ở tầng trên).
     * Dùng riêng cho import Excel để tránh false positive ConflictException.
     */
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

    // ==================== PRIVATE HELPERS ====================

    /**
     * Kiểm tra duplicate cho luồng tạo thủ công (API).
     * Ném ConflictException nếu name+supplier đã tồn tại (active hoặc trong thùng rác).
     */
    private void checkDuplicateChemical(String name, String supplier) {
        if (chemicalRepository.existsByNameIgnoreCaseAndSupplierIgnoreCaseAndIsDeletedFalse(name, supplier)) {
            throw new ConflictException(String.format(
                    "Hóa chất [%s] của nhà cung cấp [%s] đã tồn tại trong hệ thống!", name, supplier));
        }
        if (chemicalRepository.existsDeletedByNameIgnoreCaseAndSupplierIgnoreCase(name, supplier)) {
            throw new ConflictException(String.format(
                    "Hóa chất [%s] của nhà cung cấp [%s] đã tồn tại nhưng đang bị ẩn trong thùng rác. " +
                            "Vui lòng vào mục 'Thùng rác' và chọn 'Khôi phục' thay vì tạo mới!", name, supplier));
        }
    }

    private String cleanSearchParam(String param) {
        return StringUtils.hasText(param) ? param.trim() : null;
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

    /** Dùng query có sẵn trong repo thay vì load toàn bộ phòng vào memory. */
    private Room findRoomByName(String roomName) {
        return roomRepository.findByRoomNameIgnoreCase(roomName.trim())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Phòng Lab khớp với tên: " + roomName));
    }

    private Chemical findChemicalById(UUID id) {
        return chemicalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu hóa chất!"));
    }

    private List<RoomInventory> getInventoriesWithStock(UUID itemId) {
        return roomInventoryRepository.findAllByItem_ItemIdAndTotalQuantityGreaterThan(itemId, BigDecimal.ZERO);
    }
}