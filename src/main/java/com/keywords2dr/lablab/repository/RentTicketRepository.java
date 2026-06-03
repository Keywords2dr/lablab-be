package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.RentTicket;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RentTicketRepository extends JpaRepository<RentTicket, UUID>,
        JpaSpecificationExecutor<RentTicket> {

    // ── Fetch theo ID ─────────────────────────────────────────────────────────

    // Fetch room + staff (dùng cho các thao tác write: approve, cancel...)
    @Query("""
            SELECT DISTINCT t FROM RentTicket t
            JOIN FETCH t.requester req
            LEFT JOIN FETCH req.profile
            JOIN FETCH t.fromRoom r
            LEFT JOIN FETCH r.staffAssignments sa
            LEFT JOIN FETCH sa.user u
            LEFT JOIN FETCH u.profile
            WHERE t.ticketId = :ticketId
            """)
    Optional<RentTicket> findByIdWithRoomDetails(@Param("ticketId") UUID ticketId);

    // Fetch toàn bộ trong 1 query (dùng cho getTicketById - xem chi tiết)
    @Query("""
            SELECT DISTINCT t FROM RentTicket t
            JOIN FETCH t.requester req
            LEFT JOIN FETCH req.profile
            JOIN FETCH t.fromRoom r
            LEFT JOIN FETCH r.staffAssignments sa
            LEFT JOIN FETCH sa.user u
            LEFT JOIN FETCH u.profile
            LEFT JOIN FETCH t.ticketDetails td
            LEFT JOIN FETCH td.item
            WHERE t.ticketId = :ticketId
            """)
    Optional<RentTicket> findByIdFull(@Param("ticketId") UUID ticketId);

    // ── Lịch sử của tôi (requester) ──────────────────────────────────────────

    // Bước 1: lấy IDs có phân trang đúng (không JOIN FETCH collection)
    @Query(
            value = """
                    SELECT t.ticketId FROM RentTicket t
                    WHERE t.requester.userId = :requesterId
                    ORDER BY t.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM RentTicket t
                    WHERE t.requester.userId = :requesterId
                    """
    )
    Page<UUID> findMyTicketIds(
            @Param("requesterId") UUID requesterId,
            Pageable pageable);

    // Bước 1 (filter theo status): lấy IDs
    @Query(
            value = """
                    SELECT t.ticketId FROM RentTicket t
                    WHERE t.requester.userId = :requesterId
                      AND t.status = :status
                    ORDER BY t.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM RentTicket t
                    WHERE t.requester.userId = :requesterId
                      AND t.status = :status
                    """
    )
    Page<UUID> findMyTicketIdsByStatus(
            @Param("requesterId") UUID requesterId,
            @Param("status") TicketStatus status,
            Pageable pageable);

    // Bước 2: fetch đủ data cho danh sách IDs
    @Query("""
            SELECT DISTINCT t FROM RentTicket t
            JOIN FETCH t.requester req
            LEFT JOIN FETCH req.profile
            JOIN FETCH t.fromRoom
            WHERE t.ticketId IN :ids
            """)
    List<RentTicket> findAllByIds(@Param("ids") List<UUID> ids);

    // ── Admin: lấy IDs phân trang đúng ───────────────────────────────────────

    @Query(
            value = """
                    SELECT t.ticketId FROM RentTicket t
                    JOIN t.fromRoom r
                    WHERE (:roomId IS NULL OR r.roomId = :roomId)
                      AND (:status IS NULL OR t.status = :status)
                      AND (:ticketType IS NULL OR t.ticketType = :ticketType)
                      AND (:requesterId IS NULL OR t.requester.userId = :requesterId)
                    ORDER BY t.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM RentTicket t
                    JOIN t.fromRoom r
                    WHERE (:roomId IS NULL OR r.roomId = :roomId)
                      AND (:status IS NULL OR t.status = :status)
                      AND (:ticketType IS NULL OR t.ticketType = :ticketType)
                      AND (:requesterId IS NULL OR t.requester.userId = :requesterId)
                    """
    )
    Page<UUID> findAdminTicketIds(
            @Param("roomId") UUID roomId,
            @Param("status") TicketStatus status,
            @Param("ticketType") com.keywords2dr.lablab.entity.enums.TicketType ticketType,
            @Param("requesterId") UUID requesterId,
            Pageable pageable);

    // ── Teacher ───────────────────────────────────────────────────────────────

    @Query("""
            SELECT t FROM RentTicket t
            JOIN FETCH t.requester req
            LEFT JOIN FETCH req.profile
            JOIN FETCH t.fromRoom r
            JOIN r.staffAssignments sa
            WHERE sa.user.userId = :teacherId
              AND t.status = 'PENDING_OWNER'
            ORDER BY t.createdAt DESC
            """)
    List<RentTicket> findPendingTicketsByTeacher(@Param("teacherId") UUID teacherId);

    @Query(
            value = """
                    SELECT t.ticketId FROM RentTicket t
                    JOIN t.fromRoom r
                    JOIN r.staffAssignments sa
                    WHERE sa.user.userId = :teacherId
                    ORDER BY t.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT t) FROM RentTicket t
                    JOIN t.fromRoom r
                    JOIN r.staffAssignments sa
                    WHERE sa.user.userId = :teacherId
                    """
    )
    Page<UUID> findTeacherTicketIds(
            @Param("teacherId") UUID teacherId,
            Pageable pageable);

    @Query(
            value = """
                    SELECT t.ticketId FROM RentTicket t
                    JOIN t.fromRoom r
                    JOIN r.staffAssignments sa
                    WHERE sa.user.userId = :teacherId
                      AND t.status = :status
                    ORDER BY t.createdAt DESC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT t) FROM RentTicket t
                    JOIN t.fromRoom r
                    JOIN r.staffAssignments sa
                    WHERE sa.user.userId = :teacherId
                      AND t.status = :status
                    """
    )
    Page<UUID> findTeacherTicketIdsByStatus(
            @Param("teacherId") UUID teacherId,
            @Param("status") TicketStatus status,
            Pageable pageable);

    // ── Các query khác ────────────────────────────────────────────────────────

    @Query("""
            SELECT t FROM RentTicket t
            JOIN FETCH t.requester req
            LEFT JOIN FETCH req.profile
            JOIN FETCH t.fromRoom
            WHERE t.status = :status
            ORDER BY t.createdAt DESC
            """)
    List<RentTicket> findAllByStatusFetched(@Param("status") TicketStatus status);

    // Dùng cho getMyTicketsFiltered (Specification) - filter đã thu hẹp kết quả nên OK
    @Override
    @EntityGraph(attributePaths = {
            "requester", "requester.profile", "fromRoom"
    })
    Page<RentTicket> findAll(Specification<RentTicket> spec, Pageable pageable);

    List<RentTicket> findAllByRequester_UserIdAndStatus(
            UUID requesterId,
            TicketStatus status);

    @Query("""
        SELECT COUNT(t) > 0 FROM RentTicket t
        WHERE t.fromRoom.roomId = :roomId
          AND t.status NOT IN ('CANCELLED', 'REJECTED', 'RETURNED')
          AND t.borrowDate < :expectedReturnDate
          AND t.expectedReturnDate > :borrowDate
        """)
    boolean existsConflictingBooking(
            @Param("roomId") UUID roomId,
            @Param("borrowDate") LocalDateTime borrowDate,
            @Param("expectedReturnDate") LocalDateTime expectedReturnDate);

    @Query("""
            SELECT COUNT(t) FROM RentTicket t
            WHERE t.createdAt >= :from
              AND t.createdAt < :to
              AND t.status IN :statuses
            """)
    long countByCreatedAtBetweenAndStatusIn(
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to,
            @Param("statuses") List<TicketStatus> statuses);

    @Query("""
            SELECT t FROM RentTicket t
            JOIN FETCH t.fromRoom
            JOIN FETCH t.requester r
            LEFT JOIN FETCH r.profile
            WHERE t.status = 'BORROWED'
            ORDER BY t.borrowDate ASC
            """)
    List<RentTicket> findAllBorrowedWithRoomAndRequester();

    @Query("""
            SELECT DISTINCT t FROM RentTicket t
            JOIN FETCH t.fromRoom r
            LEFT JOIN FETCH r.staffAssignments sa
            LEFT JOIN FETCH sa.user
            JOIN FETCH t.requester req
            LEFT JOIN FETCH req.profile
            WHERE t.status = 'BORROWED'
              AND t.expectedReturnDate > :from
              AND t.expectedReturnDate <= :to
            """)
    List<RentTicket> findBorrowedExpiringBetween(
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to);

    @Query("""
            SELECT DISTINCT t FROM RentTicket t
            JOIN FETCH t.fromRoom r
            LEFT JOIN FETCH r.staffAssignments sa
            LEFT JOIN FETCH sa.user
            JOIN FETCH t.requester req
            LEFT JOIN FETCH req.profile
            WHERE t.status = 'BORROWED'
              AND t.expectedReturnDate < :now
            """)
    List<RentTicket> findBorrowedExpiredBefore(@Param("now") LocalDateTime now);
}