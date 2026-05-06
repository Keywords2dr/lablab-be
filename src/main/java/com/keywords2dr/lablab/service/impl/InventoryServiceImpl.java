package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.chemical.GlobalInventoryResponse;
import com.keywords2dr.lablab.dto.inventory.*;
import com.keywords2dr.lablab.entity.Chemical;
import com.keywords2dr.lablab.entity.Item;
import com.keywords2dr.lablab.entity.Room;
import com.keywords2dr.lablab.entity.RoomInventory;
import com.keywords2dr.lablab.event.NotificationEvent;
import com.keywords2dr.lablab.mapper.RoomInventoryMapper;
import com.keywords2dr.lablab.repository.ChemicalRepository;
import com.keywords2dr.lablab.repository.ItemRepository;
import com.keywords2dr.lablab.repository.RoomInventoryRepository;
import com.keywords2dr.lablab.repository.RoomRepository;
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ChemicalRepository chemicalRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final RoomRepository roomRepository;
    private final ItemRepository itemRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomInventoryMapper roomInventoryMapper;

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

            List<RoomInventory> stocks = roomInventoryRepository
                    .findAllByItem_ItemIdAndTotalQuantityGreaterThan(chemical.getItemId(), BigDecimal.ZERO);

            BigDecimal total = stocks.stream()
                    .map(RoomInventory::getTotalQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<GlobalInventoryResponse.RoomStockDetail> details = stocks.stream()
                    .map(s -> new GlobalInventoryResponse.RoomStockDetail(
                            s.getRoom().getRoomName(), s.getTotalQuantity()))
                    .toList();

            response.setGrandTotal(total);
            response.setRoomDetails(details);
            return response;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomInventoryResponseDTO> getInventoryByRoom(UUID roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new RuntimeException("Không tìm thấy Phòng Lab!");
        }
        return roomInventoryRepository.findAllByRoom_RoomId(roomId)
                .stream()
                .map(roomInventoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void allocateItems(AllocateRequestDTO request) {
        Map<UUID, Room> roomMap = batchLoadRooms(
                request.getAllocations().stream()
                        .map(RoomAllocationDTO::getRoomId)
                        .collect(Collectors.toSet())
        );
        Map<UUID, Item> itemMap = batchLoadItems(
                request.getAllocations().stream()
                        .flatMap(a -> a.getItems().stream().map(AllocateItemDTO::getItemId))
                        .collect(Collectors.toSet())
        );

        for (RoomAllocationDTO allocation : request.getAllocations()) {
            Room room = roomMap.get(allocation.getRoomId());
            if (room == null) {
                throw new RuntimeException("Không tìm thấy Phòng Lab với ID: " + allocation.getRoomId());
            }
            if (Boolean.FALSE.equals(room.getIsActive())) {
                throw new RuntimeException("Phòng Lab [" + room.getRoomName() + "] đang bị khóa!");
            }

            for (AllocateItemDTO dto : allocation.getItems()) {
                Item item = itemMap.get(dto.getItemId());
                if (item == null) {
                    throw new RuntimeException("Không tìm thấy vật tư có ID: " + dto.getItemId());
                }
                if (item.isDeleted()) {
                    throw new RuntimeException("Vật tư [" + item.getName() + "] đã bị xóa khỏi hệ thống!");
                }

                int addedPackages = dto.getPackageCount() != null ? dto.getPackageCount() : 0;
                BigDecimal addedQuantity = resolveQuantity(item, dto.getQuantity(), addedPackages);

                if (addedQuantity.compareTo(BigDecimal.ZERO) == 0 && addedPackages == 0) {
                    throw new RuntimeException("Phải nhập số lượng hộp/chai hoặc dung tích lẻ cho: " + item.getName());
                }

                RoomInventory inventory = roomInventoryRepository
                        .findByRoom_RoomIdAndItem_ItemId(room.getRoomId(), item.getItemId())
                        .orElse(null);

                if (inventory != null) {
                    inventory.setPackageCount((inventory.getPackageCount() == null ? 0 : inventory.getPackageCount()) + addedPackages);
                    inventory.setTotalQuantity(inventory.getTotalQuantity().add(addedQuantity));
                    if (StringUtils.hasText(request.getNote())) {
                        inventory.setNote(request.getNote());
                    }
                } else {
                    inventory = RoomInventory.builder()
                            .room(room)
                            .item(item)
                            .totalQuantity(addedQuantity)
                            .lockedQuantity(BigDecimal.ZERO)
                            .packageCount(addedPackages)
                            .note(request.getNote())
                            .build();
                }
                roomInventoryRepository.save(inventory);
            }

            auditLogService.logAction("ALLOCATE_INVENTORY", "ROOM", room.getRoomId(), null, allocation);

            notifyManagers(room,
                    "Hàng nhập kho",
                    String.format("Admin vừa phân bổ %d loại vật tư vào phòng %s của bạn.",
                            allocation.getItems().size(), room.getRoomName()),
                    "INVENTORY_ALLOCATE");
        }
    }

    @Override
    @Transactional
    public void revokeItems(RevokeRequestDTO request) {
        Map<UUID, Room> roomMap = batchLoadRooms(
                request.getRevocations().stream()
                        .map(RoomRevokeDTO::getRoomId)
                        .collect(Collectors.toSet())
        );
        Map<UUID, Item> itemMap = batchLoadItems(
                request.getRevocations().stream()
                        .flatMap(r -> r.getItems().stream().map(RevokeItemDTO::getItemId))
                        .collect(Collectors.toSet())
        );

        for (RoomRevokeDTO revocation : request.getRevocations()) {
            Room room = roomMap.get(revocation.getRoomId());
            if (room == null) {
                throw new RuntimeException("Không tìm thấy Phòng Lab với ID: " + revocation.getRoomId());
            }

            for (RevokeItemDTO dto : revocation.getItems()) {
                Item item = itemMap.get(dto.getItemId());
                if (item == null) {
                    throw new RuntimeException("Không tìm thấy vật tư có ID: " + dto.getItemId());
                }

                RoomInventory inventory = roomInventoryRepository
                        .findByRoom_RoomIdAndItem_ItemId(room.getRoomId(), item.getItemId())
                        .orElseThrow(() -> new RuntimeException(
                                "Vật tư [" + item.getName() + "] không tồn tại trong phòng ["
                                        + room.getRoomName() + "] để thu hồi!"));

                int reducePackages = dto.getPackageCount() != null ? dto.getPackageCount() : 0;
                BigDecimal reduceQuantity = resolveQuantity(item, dto.getQuantity(), reducePackages);

                if (reduceQuantity.compareTo(BigDecimal.ZERO) == 0 && reducePackages == 0) {
                    throw new RuntimeException("Phải nhập số lượng hộp/chai hoặc dung tích lẻ để thu hồi cho: " + item.getName());
                }

                int currentPackages = inventory.getPackageCount() != null ? inventory.getPackageCount() : 0;
                if (currentPackages < reducePackages) {
                    throw new RuntimeException(
                            "Phòng [" + room.getRoomName() + "] chỉ còn " + currentPackages
                                    + " vỏ chai/hộp " + item.getName()
                                    + ", không thể thu hồi " + reducePackages + "!");
                }

                BigDecimal available = inventory.getTotalQuantity().subtract(inventory.getLockedQuantity());
                if (reduceQuantity.compareTo(available) > 0) {
                    throw new RuntimeException(
                            "Số lượng thu hồi (" + reduceQuantity + ") vượt quá số lượng khả dụng ("
                                    + available + ") trong phòng " + room.getRoomName() + "!");
                }

                inventory.setPackageCount(currentPackages - reducePackages);
                inventory.setTotalQuantity(inventory.getTotalQuantity().subtract(reduceQuantity));
                if (StringUtils.hasText(request.getNote())) {
                    inventory.setNote(request.getNote());
                }
                roomInventoryRepository.save(inventory);
            }

            auditLogService.logAction("REVOKE_INVENTORY", "ROOM", room.getRoomId(), null, revocation);

            notifyManagers(room,
                    "Hàng xuất kho",
                    String.format("Admin vừa thu hồi %d loại vật tư khỏi phòng %s của bạn.",
                            revocation.getItems().size(), room.getRoomName()),
                    "INVENTORY_REVOKE");
        }
    }

    // PRIVATE HELPERS
    /**
     * Tính tổng số lượng thực từ số lượng lẻ + số package.
     * Chemical: 1 package = amountPerPackage đơn vị thể tích.
     * Item thường: 1 package = 1 đơn vị.
     */
    private BigDecimal resolveQuantity(Item item, BigDecimal baseQuantity, int packages) {
        BigDecimal result = baseQuantity != null ? baseQuantity : BigDecimal.ZERO;
        if (packages <= 0) return result;

        if (item instanceof Chemical chemical && chemical.getAmountPerPackage() != null) {
            result = result.add(chemical.getAmountPerPackage().multiply(new BigDecimal(packages)));
        } else if (!(item instanceof Chemical)) {
            result = result.add(new BigDecimal(packages));
        }
        return result;
    }

    private void notifyManagers(Room room, String title, String message, String type) {
        if (room.getManagers() == null || room.getManagers().isEmpty()) return;
        room.getManagers().forEach(manager ->
                eventPublisher.publishEvent(new NotificationEvent(
                        manager.getUser().getUserId(), title, message, type)));
    }

    /**
     * Batch-load Room theo danh sách ID, trả về Map để tra cứu O(1).
     */
    private Map<UUID, Room> batchLoadRooms(Set<UUID> ids) {
        return roomRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Room::getRoomId, r -> r));
    }

    /**
     * Batch-load Item theo danh sách ID, trả về Map để tra cứu O(1).
     */
    private Map<UUID, Item> batchLoadItems(Set<UUID> ids) {
        return itemRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Item::getItemId, i -> i));
    }

}