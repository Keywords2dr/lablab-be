package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.chemical.GlobalInventoryResponse;
import com.keywords2dr.lablab.dto.inventory.*;
import com.keywords2dr.lablab.entity.Chemical;
import com.keywords2dr.lablab.entity.Item;
import com.keywords2dr.lablab.entity.Room;
import com.keywords2dr.lablab.entity.RoomInventory;
import com.keywords2dr.lablab.event.NotificationEvent;
import com.keywords2dr.lablab.mapper.RoomInventoryMapper;
import com.keywords2dr.lablab.repository.ItemRepository;
import com.keywords2dr.lablab.repository.RoomInventoryRepository;
import com.keywords2dr.lablab.repository.RoomRepository;
import com.keywords2dr.lablab.service.AuditLogService;
import com.keywords2dr.lablab.service.RoomInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomInventoryServiceImpl implements RoomInventoryService {

    private final RoomInventoryRepository roomInventoryRepository;
    private final RoomRepository roomRepository;
    private final ItemRepository itemRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final RoomInventoryMapper roomInventoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<GlobalInventoryResponse> getGlobalChemicalInventory() {
        List<RoomInventory> allStocks = roomInventoryRepository.findAllPositiveStockWithItemAndRoom();

        Map<UUID, List<RoomInventory>> byItem = allStocks.stream()
                .filter(ri -> ri.getItem() instanceof Chemical)
                .collect(Collectors.groupingBy(ri -> ri.getItem().getItemId()));

        return byItem.values().stream().map(stocks -> {
            Item item = stocks.get(0).getItem();
            Chemical chemical = (Chemical) item;

            BigDecimal total = stocks.stream()
                    .map(RoomInventory::getTotalQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<GlobalInventoryResponse.RoomStockDetail> details = stocks.stream()
                    .map(s -> new GlobalInventoryResponse.RoomStockDetail(
                            s.getRoom().getRoomName(), s.getTotalQuantity()))
                    .toList();

            GlobalInventoryResponse response = new GlobalInventoryResponse();
            response.setItemId(chemical.getItemId());
            response.setItemCode(chemical.getItemCode());
            response.setName(chemical.getName());
            response.setUnit(chemical.getUnit());
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
        Map<UUID, Room> roomMap = batchLoadRoomsWithStaff(
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

                int addedPackages = dto.getPackageCount();
                BigDecimal addedQuantity = resolveQuantity(item, addedPackages);

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

            // ✅ Truyền thêm room.getRoomName() vào buildAuditPayload
            Object auditPayload = buildAuditPayload(room.getRoomId(), room.getRoomName(), allocation.getItems(), itemMap);
            auditLogService.logAction("ALLOCATE_INVENTORY", "ROOM_INVENTORY", room.getRoomId(), null, auditPayload);

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
        Map<UUID, Room> roomMap = batchLoadRoomsWithStaff(
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

                int reducePackages = dto.getPackageCount();
                BigDecimal reduceQuantity = resolveQuantity(item, reducePackages);

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

            Object auditPayload = buildAuditPayload(room.getRoomId(), room.getRoomName(), revocation.getItems(), itemMap);
            auditLogService.logAction("REVOKE_INVENTORY", "ROOM_INVENTORY", room.getRoomId(), null, auditPayload);

            notifyManagers(room,
                    "Hàng xuất kho",
                    String.format("Admin vừa thu hồi %d loại vật tư khỏi phòng %s của bạn.",
                            revocation.getItems().size(), room.getRoomName()),
                    "INVENTORY_REVOKE");
        }
    }

    // PRIVATE HELPERS
    private BigDecimal resolveQuantity(Item item, int packages) {
        if (item instanceof Chemical chemical) {
            if (chemical.getAmountPerPackage() == null) {
                throw new RuntimeException(
                        "Hóa chất [" + chemical.getName() + "] chưa có định mức theo gói (amountPerPackage). " +
                                "Vui lòng cập nhật thông tin hóa chất trước khi phân bổ theo gói!"
                );
            }
            return chemical.getAmountPerPackage().multiply(new BigDecimal(packages));
        }
        return new BigDecimal(packages);
    }

    private <T> Map<String, Object> buildAuditPayload(UUID roomId,
                                                      String roomName,
                                                      List<T> items,
                                                      Map<UUID, Item> itemMap) {
        List<Map<String, Object>> itemDetails = items.stream().map(dto -> {
            UUID itemId;
            int packageCount;

            if (dto instanceof AllocateItemDTO a) {
                itemId = a.getItemId();
                packageCount = a.getPackageCount();
            } else if (dto instanceof RevokeItemDTO r) {
                itemId = r.getItemId();
                packageCount = r.getPackageCount();
            } else {
                throw new IllegalArgumentException("Unsupported DTO type: " + dto.getClass());
            }

            Item item = itemMap.get(itemId);
            BigDecimal totalQuantity = resolveQuantity(item, packageCount);

            Map<String, Object> detail = new HashMap<>();
            detail.put("itemId", itemId);
            detail.put("itemCode", item.getItemCode());
            detail.put("itemName", item.getName());
            detail.put("packageCount", packageCount);
            detail.put("totalQuantity", totalQuantity);
            return detail;
        }).toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", roomId);
        payload.put("roomName", roomName);
        payload.put("items", itemDetails);
        return payload;
    }

    private void notifyManagers(Room room, String title, String message, String type) {
        if (room.getStaffAssignments() == null || room.getStaffAssignments().isEmpty()) return;
        room.getStaffAssignments().forEach(assignment ->
                eventPublisher.publishEvent(new NotificationEvent(
                        assignment.getUser().getUserId(), title, message, type)));
    }

    private Map<UUID, Room> batchLoadRoomsWithStaff(Set<UUID> ids) {
        return roomRepository.findAllByIdWithStaff(ids)
                .stream()
                .collect(Collectors.toMap(Room::getRoomId, r -> r));
    }

    private Map<UUID, Item> batchLoadItems(Set<UUID> ids) {
        return itemRepository.findAllById(ids)
                .stream()
                .collect(Collectors.toMap(Item::getItemId, i -> i));
    }
}