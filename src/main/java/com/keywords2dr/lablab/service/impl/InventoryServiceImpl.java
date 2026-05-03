package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.chemical.GlobalInventoryResponse;
import com.keywords2dr.lablab.entity.Chemical;
import com.keywords2dr.lablab.entity.RoomInventory;
import com.keywords2dr.lablab.repository.ChemicalRepository;
import com.keywords2dr.lablab.repository.RoomInventoryRepository;
import com.keywords2dr.lablab.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ChemicalRepository chemicalRepository;
    private final RoomInventoryRepository roomInventoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<GlobalInventoryResponse> getGlobalChemicalInventory() {

        List<Chemical> allChemicals = chemicalRepository.findAll();

        return allChemicals.stream().map(chemical -> {
            GlobalInventoryResponse response = new GlobalInventoryResponse();
            response.setItemId(chemical.getItemId());
            response.setItemCode(chemical.getItemCode());
            response.setName(chemical.getName());
            response.setUnit(chemical.getUnit());

            // Lấy dữ liệu kho
            List<RoomInventory> stocks = roomInventoryRepository
                    .findAllByItem_ItemIdAndTotalQuantityGreaterThan(chemical.getItemId(), BigDecimal.ZERO);

            // Tính tổng (Grand Total)
            BigDecimal total = stocks.stream()
                    .map(RoomInventory::getTotalQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Lấy chi tiết từng phòng
            List<GlobalInventoryResponse.RoomStockDetail> details = stocks.stream()
                    .map(s -> new GlobalInventoryResponse.RoomStockDetail(s.getRoom().getRoomName(), s.getTotalQuantity()))
                    .collect(Collectors.toList());

            response.setGrandTotal(total);
            response.setRoomDetails(details);

            return response;
        }).collect(Collectors.toList());
    }
}