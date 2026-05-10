package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.chemical.ChemicalAdminResponse;
import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.dto.chemical.DeleteChemicalResponse;
import com.keywords2dr.lablab.entity.Chemical;
import com.keywords2dr.lablab.entity.RoomInventory;
import com.keywords2dr.lablab.exception.BadRequestException;
import com.keywords2dr.lablab.exception.ConflictException;
import com.keywords2dr.lablab.exception.ResourceNotFoundException;
import com.keywords2dr.lablab.mapper.ChemicalMapper;
import com.keywords2dr.lablab.repository.ChemicalRepository;
import com.keywords2dr.lablab.repository.ItemRepository;
import com.keywords2dr.lablab.repository.RoomInventoryRepository;
import com.keywords2dr.lablab.repository.specification.ChemicalSpecification;
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.ChemicalService;
import com.keywords2dr.lablab.service.DataNormalizationService;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class ChemicalServiceImpl implements ChemicalService {

    private final ChemicalRepository chemicalRepository;
    private final ItemRepository itemRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final ChemicalMapper chemicalMapper;
    private final DataNormalizationService normalizationService;
    private final AuditLogService auditLogService;

    // PUBLIC CORE METHODS (CRUD)
    @Override
    @Transactional
    public ChemicalAdminResponse createChemical(ChemicalRequestDTO request) {
        validateItemCodeUniqueness(request.getItemCode());

        if (chemicalRepository.existsByNameIgnoreCaseAndSupplierIgnoreCaseAndIsDeletedFalse(
                request.getName(), request.getSupplier())) {
            throw new ConflictException(String.format(
                    "Hóa chất [%s] của nhà cung cấp [%s] đã tồn tại trong hệ thống!",
                    request.getName(), request.getSupplier()));
        }

        if (chemicalRepository.existsDeletedByNameIgnoreCaseAndSupplierIgnoreCase(
                request.getName(), request.getSupplier())) {
            throw new ConflictException(String.format(
                    "Hóa chất [%s] của nhà cung cấp [%s] đã tồn tại nhưng đang bị ẩn trong thùng rác. " +
                            "Vui lòng vào mục 'Thùng rác' và chọn 'Khôi phục' thay vì tạo mới!",
                    request.getName(), request.getSupplier()));
        }

        normalizeChemicalRequest(request);
        Chemical chemical = chemicalMapper.toEntity(request);
        Chemical savedChemical = chemicalRepository.save(chemical);

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
        if (!chemical.getUnit().equals(request.getUnit())) {
            if (roomInventoryRepository.existsByItem_ItemIdAndTotalQuantityGreaterThan(id, BigDecimal.ZERO)) {
                throw new BadRequestException("Không thể đổi Đơn vị tính! Hóa chất này đang còn tồn kho thực tế. Vui lòng thu hồi về 0 trước khi đổi.");
            }
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

        Chemical restoredChemical = findChemicalByIdIgnoreDeleted(id);
        auditLogService.logAction("RESTORE", "CHEMICAL", id, null, chemicalMapper.toAdminResponse(restoredChemical));
    }

    // PUBLIC FEATURE METHODS (GET & BATCH)
    @Override
    @Transactional(readOnly = true)
    public Page<ChemicalAdminResponse> filterChemicals(String keyword, String packaging, String supplier, String unit, String category, Pageable pageable) {
        Specification<Chemical> spec = ChemicalSpecification.filter(
                cleanSearchParam(keyword), cleanSearchParam(packaging), cleanSearchParam(supplier), cleanSearchParam(unit), cleanSearchParam(category)
        );
        return chemicalRepository.findAll(spec, pageable).map(chemicalMapper::toAdminResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChemicalAdminResponse> getDeletedChemicalsForAdmin() {
        return chemicalRepository.findDeletedChemicals().stream().map(chemicalMapper::toAdminResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getChemicalFormOptions() {
        Map<String, List<String>> options = new HashMap<>();

        options.put("packagings", chemicalRepository.findDistinctPackagings());
        options.put("units", chemicalRepository.findDistinctUnits());

        List<String> rawSuppliers = chemicalRepository.findDistinctSuppliers();

        List<String> processedSuppliers = rawSuppliers.stream()
                .filter(StringUtils::hasText)
                .flatMap(s -> Arrays.stream(s.split("[,/\\-]")))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .sorted()
                .toList();

        options.put("suppliers", processedSuppliers);

        return options;
    }

    @Override
    public Map<String, Object> processBatchImport(List<ChemicalRequestDTO> dtoList, String fileName) {
        int successCount = 0;
        List<Map<String, String>> failures = new ArrayList<>();

        for (ChemicalRequestDTO dto : dtoList) {
            try {
                createChemicalInNewTransaction(dto);
                successCount++;
            } catch (Exception e) {
                failures.add(Map.of(
                        "itemCode", dto.getItemCode() != null ? dto.getItemCode() : "(không có mã)",
                        "name", dto.getName() != null ? dto.getName() : "(không có tên)",
                        "reason", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
                ));
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("fileName", fileName);
        summary.put("totalRowsParsed", dtoList.size());
        summary.put("successCount", successCount);
        summary.put("failCount", failures.size());

        auditLogService.logAction("IMPORT_EXCEL", "CHEMICAL_IMPORT", null, null, summary);

        Map<String, Object> response = new HashMap<>(summary);
        response.put("message", String.format("Import hoàn tất! Thành công: %d. Thất bại (bỏ qua): %d", successCount, failures.size()));
        if (!failures.isEmpty()) response.put("failures", failures);

        return response;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createChemicalInNewTransaction(ChemicalRequestDTO request) {
        createChemical(request);
    }

    // HELPER METHODS
    private void normalizeChemicalRequest(ChemicalRequestDTO request) {
        request.setPackaging(normalizationService.normalizeAndLearn(request.getPackaging(), "PACKAGING"));
        request.setUnit(normalizationService.normalizeAndLearn(request.getUnit(), "UNIT"));
    }

    private String cleanSearchParam(String param) {
        return StringUtils.hasText(param) ? param.trim() : null;
    }

    private void validateItemCodeUniqueness(String itemCode) {
        if (itemRepository.existsByItemCode(itemCode)) {
            throw new ConflictException("Mã hóa chất [" + itemCode + "] đã tồn tại!");
        }
    }

    private Chemical findChemicalById(UUID id) {
        return chemicalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy dữ liệu hóa chất!"));
    }

    private Chemical findChemicalByIdIgnoreDeleted(UUID id) {
        return chemicalRepository.findDeletedChemicals().stream()
                .filter(c -> c.getItemId().equals(id))
                .findFirst()
                .orElseGet(() -> findChemicalById(id));
    }

    private List<RoomInventory> getInventoriesWithStock(UUID itemId) {
        return roomInventoryRepository.findAllByItem_ItemIdAndTotalQuantityGreaterThan(itemId, BigDecimal.ZERO);
    }
}