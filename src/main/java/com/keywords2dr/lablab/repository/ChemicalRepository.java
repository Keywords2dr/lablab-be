package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.Chemical;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Import thêm cái này
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChemicalRepository extends JpaRepository<Chemical, UUID>, JpaSpecificationExecutor<Chemical> {

    @Modifying
    @Query("UPDATE Item i SET i.isDeleted = true WHERE i.itemId = :id")
    int softDeleteById(@Param("id") UUID id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE items SET is_deleted = false WHERE item_id = :id", nativeQuery = true)
    int restoreById(@Param("id") UUID id);

    @Query(value = "SELECT c.*, i.* FROM items i JOIN chemicals c ON i.item_id = c.item_id WHERE i.is_deleted = true", nativeQuery = true)
    List<Chemical> findDeletedChemicals();

    List<Chemical> findAllByIsDeletedFalse();

    boolean existsByNameIgnoreCaseAndSupplierIgnoreCase(String name, String supplier);
}