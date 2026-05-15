package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.RentTicket;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RentTicketRepository extends JpaRepository<RentTicket, UUID>,
        JpaSpecificationExecutor<RentTicket> {

    // ── TEACHER ──────────────────────────────────────────────────────────────

    /**
     * Lấy danh sách phiếu đang chờ Teacher duyệt của phòng mà Teacher đó quản lý.
     */
    @Query("""
            SELECT t FROM RentTicket t
            JOIN t.fromRoom r
            JOIN r.staffAssignments sa
            WHERE sa.user.userId = :teacherId
              AND t.status = 'PENDING_OWNER'
            ORDER BY t.createdAt DESC
            """)
    List<RentTicket> findPendingTicketsByTeacher(@Param("teacherId") UUID teacherId);

    /**
     * Lấy toàn bộ phiếu của phòng mà Teacher quản lý (tất cả trạng thái), có phân trang.
     */
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

    /**
     * Lọc phiếu theo status cho Teacher — chỉ phiếu thuộc phòng mình quản lý.
     */
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

    /**
     * Lấy danh sách phiếu đang chờ Admin duyệt (sau khi Teacher đã duyệt).
     */
    List<RentTicket> findAllByStatusOrderByCreatedAtDesc(TicketStatus status);

    // ── REQUESTER (Student / Teacher xem phiếu của chính mình) ───────────────

    /**
     * Lấy danh sách phiếu của người tạo, có phân trang.
     */
    Page<RentTicket> findAllByRequester_UserIdOrderByCreatedAtDesc(
            UUID requesterId,
            Pageable pageable);

    /**
     * Lọc phiếu của người tạo theo trạng thái, có phân trang.
     */
    Page<RentTicket> findAllByRequester_UserIdAndStatusOrderByCreatedAtDesc(
            UUID requesterId,
            TicketStatus status,
            Pageable pageable);

    /**
     * Lấy phiếu của người tạo theo trạng thái cụ thể (list, không phân trang).
     */
    List<RentTicket> findAllByRequester_UserIdAndStatus(
            UUID requesterId,
            TicketStatus status);

    // ── KIỂM TRA CONFLICT THỜI GIAN ───────────────────────────────────────────

    /**
     * Kiểm tra phòng đã có phiếu được duyệt/đang mượn trong khoảng thời gian
     * yêu cầu chưa — tránh trùng lịch mượn phòng.
     */
    @Query("""
        SELECT COUNT(t) > 0 FROM RentTicket t
        WHERE t.fromRoom.roomId = :roomId
          AND t.status NOT IN ('CANCELLED', 'REJECTED', 'RETURNED')
          AND t.borrowDate < :expectedReturnDate
          AND t.expectedReturnDate > :borrowDate
        """)
    boolean existsConflictingBooking(
            @Param("roomId") UUID roomId,
            @Param("borrowDate") java.time.LocalDateTime borrowDate,
            @Param("expectedReturnDate") java.time.LocalDateTime expectedReturnDate);
}