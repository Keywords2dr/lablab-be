package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.Chemical;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChemicalRepository extends JpaRepository<Chemical, UUID>, JpaSpecificationExecutor<Chemical> {

    @Modifying
    @Transactional
    @Query("UPDATE Item i SET i.isDeleted = true WHERE i.itemId = :id")
    int softDeleteById(@Param("id") UUID id);

    @Modifying
    @Query(value = "UPDATE items SET is_deleted = false WHERE item_id = :id", nativeQuery = true)
    int restoreById(@Param("id") UUID id);

    @Query(value = "SELECT * FROM chemicals c JOIN items i ON c.item_id = i.item_id WHERE i.is_deleted = true", nativeQuery = true)
    List<Chemical> findDeletedChemicals();

    List<Chemical> findAllByIsDeletedFalse();

    boolean existsByNameIgnoreCaseAndSupplierIgnoreCaseAndIsDeletedFalse(String name, String supplier);

    @Query(value = "SELECT COUNT(*) > 0 FROM chemicals c " +
            "JOIN items i ON c.item_id = i.item_id " +
            "WHERE LOWER(i.name) = LOWER(:name) " +
            "AND LOWER(c.supplier) = LOWER(:supplier) " +
            "AND i.is_deleted = true", nativeQuery = true)
    boolean existsDeletedByNameIgnoreCaseAndSupplierIgnoreCase(
            @Param("name") String name,
            @Param("supplier") String supplier);

    @Query("SELECT DISTINCT c.packaging FROM Chemical c WHERE c.packaging IS NOT NULL AND c.isDeleted = false ORDER BY c.packaging ASC")
    List<String> findDistinctPackagings();

    @Query("SELECT DISTINCT c.supplier FROM Chemical c WHERE c.supplier IS NOT NULL AND c.isDeleted = false ORDER BY c.supplier ASC")
    List<String> findDistinctSuppliers();

    @Query("SELECT DISTINCT c.unit FROM Chemical c WHERE c.unit IS NOT NULL AND c.isDeleted = false ORDER BY c.unit ASC")
    List<String> findDistinctUnits();
}