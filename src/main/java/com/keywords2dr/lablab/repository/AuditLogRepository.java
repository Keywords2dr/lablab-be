package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    @Query("SELECT DISTINCT a.entityName FROM AuditLog a WHERE a.entityName IS NOT NULL ORDER BY a.entityName ASC")
    List<String> findDistinctModules();
}