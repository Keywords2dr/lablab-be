package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.RentTicketDetail;
import com.keywords2dr.lablab.entity.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RentTicketDetailRepository extends JpaRepository<RentTicketDetail, UUID> {

    /**
     * Lấy tất cả chi tiết của 1 phiếu.
     */
    List<RentTicketDetail> findAllByTicket_TicketId(UUID ticketId);

    /**
     * Kiểm tra phiếu còn dòng nào chưa trả không.
     * Dùng khi xác nhận trả để validate toàn bộ đã điền returnStatus.
     */
    boolean existsByTicket_TicketIdAndReturnStatus(UUID ticketId, ReturnStatus returnStatus);

    /**
     * Lấy các dòng bị mất hoặc hỏng của 1 phiếu — dùng để notify Admin.
     */
    @Query("""
            SELECT d FROM RentTicketDetail d
            WHERE d.ticket.ticketId = :ticketId
              AND d.returnStatus IN ('DAMAGED', 'LOST', 'PARTIAL')
            """)
    List<RentTicketDetail> findProblematicDetails(@Param("ticketId") UUID ticketId);

    /**
     * Kiểm tra item này có đang được mượn trong phiếu nào của phòng đó không.
     * Dùng khi Admin muốn thu hồi hoặc xóa hóa chất.
     */
    @Query("""
            SELECT COUNT(d) > 0 FROM RentTicketDetail d
            WHERE d.item.itemId = :itemId
              AND d.ticket.fromRoom.roomId = :roomId
              AND d.ticket.status IN ('APPROVED', 'BORROWED')
              AND d.returnStatus = 'NOT_RETURNED'
            """)
    boolean existsActiveBorrowingByItemAndRoom(
            @Param("itemId") UUID itemId,
            @Param("roomId") UUID roomId);
}