package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.RoomInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface RoomInventoryRepository extends JpaRepository<RoomInventory, UUID> {
    boolean existsByItem_ItemIdAndTotalQuantityGreaterThan(UUID itemId, BigDecimal quantity);

    List<RoomInventory> findAllByItem_ItemIdAndTotalQuantityGreaterThan(UUID itemId, java.math.BigDecimal quantity);
}