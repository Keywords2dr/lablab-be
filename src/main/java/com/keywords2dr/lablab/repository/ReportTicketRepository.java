package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.ReportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReportTicketRepository
        extends JpaRepository<ReportTicket, UUID>, JpaSpecificationExecutor<ReportTicket> {

    @Query("""
            SELECT t FROM ReportTicket t
            JOIN FETCH t.reporter
            JOIN FETCH t.room
            LEFT JOIN FETCH t.item
            WHERE t.reportId = :id
            """)
    Optional<ReportTicket> findByIdFetch(@Param("id") UUID id);

    @Query(
            value = """
                    SELECT t FROM ReportTicket t
                    JOIN FETCH t.reporter
                    JOIN FETCH t.room
                    LEFT JOIN FETCH t.item
                    WHERE t.reporter.userId = :userId
                    """,
            countQuery = "SELECT COUNT(t) FROM ReportTicket t WHERE t.reporter.userId = :userId"
    )
    Page<ReportTicket> findByReporterIdFetch(@Param("userId") UUID userId, Pageable pageable);

    @EntityGraph(value = "ReportTicket.withRelations")
    @Override
    Page<ReportTicket> findAll(Specification<ReportTicket> spec, Pageable pageable);
}