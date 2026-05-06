package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.chemical.ChemicalAdminResponse;
import com.keywords2dr.lablab.dto.chemical.ChemicalRequestDTO;
import com.keywords2dr.lablab.entity.Chemical;
import com.keywords2dr.lablab.entity.RoomInventory;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
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

    @Override
    @Transactional
    public Chemical createChemical(ChemicalRequestDTO request) {
        validateItemCodeUniqueness(request.getItemCode());

        if (chemicalRepository.existsByNameIgnoreCaseAndSupplierIgnoreCase(request.getName(), request.getSupplier())) {
            throw new RuntimeException(
                    String.format("Hóa chất [%s] của nhà cung cấp [%s] đã tồn tại trong hệ thống!",
                            request.getName(), request.getSupplier())
            );
        }

        normalizeChemicalRequest(request);

        Chemical chemical = chemicalMapper.toEntity(request);
        Chemical savedChemical = chemicalRepository.save(chemical);

        ChemicalAdminResponse newState = chemicalMapper.toAdminResponse(savedChemical);
        auditLogService.logAction("CREATE", "CHEMICAL", savedChemical.getItemId(), null, newState);

        return savedChemical;
    }

    @Override
    @Transactional
    public Chemical updateChemical(UUID id, ChemicalRequestDTO request) {
        Chemical chemical = findChemicalById(id);

        if (!chemical.getItemCode().equals(request.getItemCode())) {
            validateItemCodeUniqueness(request.getItemCode());
        }

        if (!chemical.getUnit().equals(request.getUnit())) {
            boolean hasStock = roomInventoryRepository.existsByItem_ItemIdAndTotalQuantityGreaterThan(id, BigDecimal.ZERO);
            if (hasStock) {
                throw new RuntimeException("Không thể đổi Đơn vị tính! Hóa chất này đang còn tồn kho thực tế. Vui lòng thu hồi về 0 trước khi đổi.");
            }
        }

        normalizeChemicalRequest(request);

        ChemicalAdminResponse oldState = chemicalMapper.toAdminResponse(chemical);

        chemicalMapper.updateEntityFromDto(request, chemical);
        Chemical updatedChemical = chemicalRepository.save(chemical);

        ChemicalAdminResponse newState = chemicalMapper.toAdminResponse(updatedChemical);
        auditLogService.logAction("UPDATE", "CHEMICAL", updatedChemical.getItemId(), oldState, newState);

        return updatedChemical;
    }

    @Override
    @Transactional
    public String deleteChemical(UUID id) {
        Chemical chemical = findChemicalById(id);
        List<RoomInventory> activeInventories = getInventoriesWithStock(id);

        ChemicalAdminResponse oldState = chemicalMapper.toAdminResponse(chemical);

        chemicalRepository.softDeleteById(id);
        auditLogService.logAction("DELETE", "CHEMICAL", id, oldState, null);

        if (activeInventories.isEmpty()) {
            return "Đã xóa (ẩn) hóa chất thành công.";
        }
        return buildRecallInstruction(activeInventories, chemical.getUnit());
    }

    @Override
    @Transactional
    public void restoreChemical(UUID id) {
        int rowsAffected = chemicalRepository.restoreById(id);

        if (rowsAffected == 0) {
            throw new RuntimeException("Không tìm thấy hóa chất bị ẩn để khôi phục (ID có thể sai)!");
        }

        Chemical restoredChemical = findChemicalById(id);
        ChemicalAdminResponse newState = chemicalMapper.toAdminResponse(restoredChemical);
        auditLogService.logAction("RESTORE", "CHEMICAL", id, null, newState);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChemicalAdminResponse> getDeletedChemicalsForAdmin() {
        List<Chemical> deletedChemicals = chemicalRepository.findDeletedChemicals();

        return deletedChemicals.stream()
                .map(chemicalMapper::toAdminResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ChemicalAdminResponse> filterChemicals(String keyword, String packaging, String supplier, String unit, String category, Pageable pageable) {

        String k = cleanSearchParam(keyword);
        String p = cleanSearchParam(packaging);
        String s = cleanSearchParam(supplier);
        String u = cleanSearchParam(unit);
        String c = cleanSearchParam(category);

        Specification<Chemical> spec = ChemicalSpecification.filter(k, p, s, u, c);

        Page<Chemical> chemicalPage = chemicalRepository.findAll(spec, pageable);
        return chemicalPage.map(chemicalMapper::toAdminResponse);
    }

    // CÁC HÀM PHỤ TRỢ (HELPER METHODS)
    private void normalizeChemicalRequest(ChemicalRequestDTO request) {
        request.setPackaging(normalizationService.normalizeAndLearn(request.getPackaging(), "PACKAGING"));
        request.setUnit(normalizationService.normalizeAndLearn(request.getUnit(), "UNIT"));
    }

    private String cleanSearchParam(String param) {
        return StringUtils.hasText(param) ? param.trim() : null;
    }

    private void validateItemCodeUniqueness(String itemCode) {
        if (itemRepository.existsByItemCode(itemCode)) {
            throw new RuntimeException("Mã hóa chất [" + itemCode + "] đã tồn tại!");
        }
    }

    private Chemical findChemicalById(UUID id) {
        return chemicalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy dữ liệu hóa chất!"));
    }

    private List<RoomInventory> getInventoriesWithStock(UUID itemId) {
        return roomInventoryRepository.findAllByItem_ItemIdAndTotalQuantityGreaterThan(itemId, BigDecimal.ZERO);
    }

    private String buildRecallInstruction(List<RoomInventory> inventories, String unit) {
        String rooms = inventories.stream()
                .map(inv -> String.format("[%s: %s %s]",
                        inv.getRoom().getRoomName(),
                        inv.getTotalQuantity(),
                        unit))
                .collect(Collectors.joining(", "));
        return "Đã ẩn hóa chất khỏi hệ thống. LƯU Ý: Bạn cần thực hiện thu hồi thực tế tại các phòng sau: " + rooms;
    }
}