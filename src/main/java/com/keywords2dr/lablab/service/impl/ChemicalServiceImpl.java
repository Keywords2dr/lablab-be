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
import com.keywords2dr.lablab.service.ChemicalService;
import com.keywords2dr.lablab.service.DataNormalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public Chemical createChemical(ChemicalRequestDTO request) {
        validateItemCodeUniqueness(request.getItemCode());

        normalizeChemicalRequest(request);

        Chemical chemical = chemicalMapper.toEntity(request);
        return chemicalRepository.save(chemical);
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

        chemicalMapper.updateEntityFromDto(request, chemical);

        return chemicalRepository.save(chemical);
    }

    @Override
    @Transactional
    public String deleteChemical(UUID id) {
        Chemical chemical = findChemicalById(id);
        List<RoomInventory> activeInventories = getInventoriesWithStock(id);

        chemicalRepository.softDeleteById(id);

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
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChemicalAdminResponse> getAllChemicalsForAdmin() {
        List<Chemical> chemicals = chemicalRepository.findAll();
        return chemicals.stream()
                .map(chemicalMapper::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChemicalAdminResponse> getDeletedChemicalsForAdmin() {
        List<Chemical> deletedChemicals = chemicalRepository.findDeletedChemicals();

        return deletedChemicals.stream()
                .map(chemicalMapper::toAdminResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChemicalAdminResponse> filterChemicals(String keyword, String packaging, String supplier, String unit, String category) {

        String k = cleanSearchParam(keyword);
        String p = cleanSearchParam(packaging);
        String s = cleanSearchParam(supplier);
        String u = cleanSearchParam(unit);
        String c = cleanSearchParam(category);

        Specification<Chemical> spec = ChemicalSpecification.filter(k, p, s, u, c);

        List<Chemical> chemicals = chemicalRepository.findAll(spec);

        return chemicals.stream()
                .map(chemicalMapper::toAdminResponse)
                .collect(Collectors.toList());
    }

    // CÁC HÀM PHỤ TRỢ (HELPER METHODS)
    private void normalizeChemicalRequest(ChemicalRequestDTO request) {
        request.setPackaging(normalizationService.normalizeAndLearn(request.getPackaging(), "PACKAGING"));
        request.setUnit(normalizationService.normalizeAndLearn(request.getUnit(), "UNIT"));
    }

    private String cleanSearchParam(String param) {
        return (param != null && !param.trim().isEmpty()) ? param.trim() : null;
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
        StringBuilder sb = new StringBuilder("Đã ẩn hóa chất khỏi hệ thống. ");
        sb.append("LƯU Ý: Bạn cần thực hiện thu hồi thực tế tại các phòng sau: ");

        for (RoomInventory inv : inventories) {
            sb.append(String.format("[%s: %s %s], ",
                    inv.getRoom().getRoomName(),
                    inv.getTotalQuantity(),
                    unit));
        }
        return sb.substring(0, sb.length() - 2);
    }
}