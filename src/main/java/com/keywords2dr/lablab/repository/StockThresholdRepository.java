package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.StockThreshold;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockThresholdRepository extends JpaRepository<StockThreshold, UUID> {

    Optional<StockThreshold> findByItem_ItemId(UUID itemId);

    boolean existsByItem_ItemId(UUID itemId);

    @Query("SELECT st FROM StockThreshold st JOIN FETCH st.item")
    List<StockThreshold> findAllWithItem();
}