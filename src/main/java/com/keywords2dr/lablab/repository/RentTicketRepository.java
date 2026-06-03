package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.RentTicket;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RentTicketRepository extends JpaRepository<RentTicket, UUID>,
        JpaSpecificationExecutor<RentTicket> {

    // ── TEACHER ──────────────────────────────────────────────────────────────

    @Query("""
            SELECT t FROM RentTicket t
            JOIN t.fromRoom r
            JOIN r.staffAssignments sa
            WHERE sa.user.userId = :teacherId
              AND t.status = 'PENDING_OWNER'
            ORDER BY t.createdAt DESC
            """)
    List<RentTicket> findPendingTicketsByTeacher(@Param("teacherId") UUID teacherId);

    @Query("""
            SELECT t FROM RentTicket t
            JOIN t.fromRoom r
            JOIN r.staffAssignments sa
            WHERE sa.user.userId = :teacherId
            ORDER BY t.createdAt DESC
            """)
    Page<RentTicket> findAllTicketsByTeacher(
            @Param("teacherId") UUID teacherId,
            Pageable pageable);

    @Query("""
            SELECT t FROM RentTicket t
            JOIN t.fromRoom r
            JOIN r.staffAssignments sa
            WHERE sa.user.userId = :teacherId
              AND t.status = :status
            ORDER BY t.createdAt DESC
            """)
    Page<RentTicket> findTicketsByTeacherAndStatus(
            @Param("teacherId") UUID teacherId,
            @Param("status") TicketStatus status,
            Pageable pageable);

    // ── ADMIN ─────────────────────────────────────────────────────────────────

    List<RentTicket> findAllByStatusOrderByCreatedAtDesc(TicketStatus status);

    // ── REQUESTER ─────────────────────────────────────────────────────────────

    Page<RentTicket> findAllByRequester_UserIdOrderByCreatedAtDesc(
            UUID requesterId,
            Pageable pageable);

    Page<RentTicket> findAllByRequester_UserIdAndStatusOrderByCreatedAtDesc(
            UUID requesterId,
            TicketStatus status,
            Pageable pageable);

    List<RentTicket> findAllByRequester_UserIdAndStatus(
            UUID requesterId,
            TicketStatus status);

    // ── KIỂM TRA CONFLICT THỜI GIAN ───────────────────────────────────────────

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

    // ── DASHBOARD: Weekly Stats ───────────────────────────────────────────────

    /**
     * Đếm số phiếu có createdAt trong khoảng [from, to) và status thuộc danh sách statuses.
     * Dùng cho endpoint GET /api/tickets/admin/stats/weekly
     */
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

    // ── DASHBOARD: Room Current Usage ─────────────────────────────────────────

    /**
     * Lấy tất cả phiếu đang ở trạng thái BORROWED kèm thông tin phòng và người mượn.
     * Dùng cho endpoint GET /api/rooms/current-usage
     */
    @Query("""
            SELECT t FROM RentTicket t
            JOIN FETCH t.fromRoom
            JOIN FETCH t.requester r
            LEFT JOIN FETCH r.profile
            WHERE t.status = 'BORROWED'
            ORDER BY t.borrowDate ASC
            """)
    List<RentTicket> findAllBorrowedWithRoomAndRequester();
}