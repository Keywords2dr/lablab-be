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

    @Query("""
            SELECT DISTINCT t FROM RentTicket t
            JOIN FETCH t.requester req
            LEFT JOIN FETCH req.profile
            JOIN FETCH t.fromRoom
            LEFT JOIN FETCH t.ticketDetails
            WHERE t.status = :status
            ORDER BY t.createdAt DESC
            """)
    List<RentTicket> findAllByStatusFetched(@Param("status") TicketStatus status);

    @Query("""
            SELECT DISTINCT t FROM RentTicket t
            JOIN FETCH t.requester req
            LEFT JOIN FETCH req.profile
            JOIN FETCH t.fromRoom
            LEFT JOIN FETCH t.ticketDetails
            WHERE t.requester.userId = :requesterId
            ORDER BY t.createdAt DESC
            """)
    Page<RentTicket> findMyTicketsFetched(
            @Param("requesterId") UUID requesterId,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT t FROM RentTicket t
            JOIN FETCH t.requester req
            LEFT JOIN FETCH req.profile
            JOIN FETCH t.fromRoom
            LEFT JOIN FETCH t.ticketDetails
            WHERE t.requester.userId = :requesterId
              AND t.status = :status
            ORDER BY t.createdAt DESC
            """)
    Page<RentTicket> findMyTicketsByStatusFetched(
            @Param("requesterId") UUID requesterId,
            @Param("status") TicketStatus status,
            Pageable pageable);

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