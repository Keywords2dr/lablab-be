package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.RoomInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomInventoryRepository extends JpaRepository<RoomInventory, UUID> {

    boolean existsByItem_ItemIdAndTotalQuantityGreaterThan(UUID itemId, BigDecimal quantity);

    List<RoomInventory> findAllByItem_ItemIdAndTotalQuantityGreaterThan(UUID itemId, BigDecimal quantity);

    Optional<RoomInventory> findByRoom_RoomIdAndItem_ItemId(UUID roomId, UUID itemId);

    List<RoomInventory> findAllByRoom_RoomId(UUID roomId);

    @Query("""
            SELECT ri FROM RoomInventory ri
            JOIN FETCH ri.item
            JOIN FETCH ri.room
            WHERE ri.totalQuantity > 0
            """)
    List<RoomInventory> findAllPositiveStockWithItemAndRoom();
}