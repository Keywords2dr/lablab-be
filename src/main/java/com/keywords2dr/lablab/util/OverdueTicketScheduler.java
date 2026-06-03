package com.keywords2dr.lablab.util;

import com.keywords2dr.lablab.entity.RentTicket;
import com.keywords2dr.lablab.event.NotificationEvent;
import com.keywords2dr.lablab.repository.RentTicketRepository;
import com.keywords2dr.lablab.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueTicketScheduler {

    private final RentTicketRepository rentTicketRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedRate = 15 * 60 * 1000)
    @Transactional(readOnly = true)
    public void remindBeforeExpiry() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusMinutes(30);

        List<RentTicket> soonExpiring = rentTicketRepository.findBorrowedExpiringBetween(now, soon);

        if (soonExpiring.isEmpty()) return;

        log.info("[SCHEDULER] Nhắc nhở sớm: {} phiếu sắp hết hạn trong 30 phút.", soonExpiring.size());

        for (RentTicket ticket : soonExpiring) {
            String roomName = ticket.getFromRoom().getRoomName();
            String borrower = UserNameResolver.resolve(ticket.getRequester());
            long minutesLeft = ChronoUnit.MINUTES.between(now, ticket.getExpectedReturnDate());

            eventPublisher.publishEvent(new NotificationEvent(
                    ticket.getRequester().getUserId(),
                    "Nhắc nhở: Sắp đến hạn trả",
                    String.format("Phiếu mượn tại phòng %s của bạn sẽ hết hạn sau khoảng %d phút. Vui lòng yêu cầu trả sớm để tránh quá hạn!", roomName, minutesLeft),
                    "TICKET_EXPIRY_REMINDER"
            ));

            if (ticket.getFromRoom().getStaffAssignments() != null) {
                ticket.getFromRoom().getStaffAssignments().forEach(sa ->
                        eventPublisher.publishEvent(new NotificationEvent(
                                sa.getUser().getUserId(),
                                "Phiếu mượn sắp hết hạn",
                                String.format("[%s] có phiếu mượn tại phòng %s sẽ hết hạn sau %d phút.", borrower, roomName, minutesLeft),
                                "TICKET_EXPIRY_REMINDER_STAFF"
                        ))
                );
            }
        }
    }

    @Scheduled(fixedRate = 15 * 60 * 1000, initialDelay = 7 * 60 * 1000)
    @Transactional(readOnly = true)
    public void processOverdueTickets() {
        LocalDateTime now = LocalDateTime.now();

        List<RentTicket> overdue = rentTicketRepository.findBorrowedExpiredBefore(now);

        if (overdue.isEmpty()) return;

        log.warn("[SCHEDULER] Phát hiện {} phiếu quá hạn, bắt đầu xử lý...", overdue.size());

        for (RentTicket ticket : overdue) {
            try {
                handleOverdueTicket(ticket, now);
            } catch (Exception e) {
                log.error("[SCHEDULER] Lỗi xử lý phiếu quá hạn {}: {}", ticket.getTicketId(), e.getMessage());
            }
        }
    }

    private void handleOverdueTicket(RentTicket ticket, LocalDateTime now) {
        String roomName = ticket.getFromRoom().getRoomName();
        String borrower = UserNameResolver.resolve(ticket.getRequester());
        long overdueMinutes = ChronoUnit.MINUTES.between(ticket.getExpectedReturnDate(), now);

        log.warn("[SCHEDULER] Phiếu {} quá hạn {} phút — người mượn: [{}], phòng: [{}]",
                ticket.getTicketId(), overdueMinutes, borrower, roomName);

        eventPublisher.publishEvent(new NotificationEvent(
                ticket.getRequester().getUserId(),
                "Phiếu mượn đã QUÁ HẠN",
                String.format("Phiếu mượn tại phòng %s của bạn đã QUÁ HẠN %d phút (hạn trả: %s). Vui lòng yêu cầu trả ngay hoặc liên hệ Teacher!",
                        roomName, overdueMinutes, ticket.getExpectedReturnDate()),
                "TICKET_OVERDUE"
        ));

        if (ticket.getFromRoom().getStaffAssignments() != null) {
            ticket.getFromRoom().getStaffAssignments().forEach(sa ->
                    eventPublisher.publishEvent(new NotificationEvent(
                            sa.getUser().getUserId(),
                            "Phiếu mượn quá hạn",
                            String.format("[%s] có phiếu mượn tại phòng %s đã QUÁ HẠN %d phút. Vui lòng nhắc người mượn trả lại.", borrower, roomName, overdueMinutes),
                            "TICKET_OVERDUE_STAFF"
                    ))
            );
        }

        if (overdueMinutes > 60) {
            userRepository.findAllByRole("ADMIN").forEach(admin ->
                    eventPublisher.publishEvent(new NotificationEvent(
                            admin.getUserId(),
                            "Cảnh báo: Phiếu quá hạn nghiêm trọng",
                            String.format("[%s] có phiếu mượn tại phòng %s đã QUÁ HẠN hơn %d phút nhưng chưa được yêu cầu trả. Cần xem xét xử lý thủ công.", borrower, roomName, overdueMinutes),
                            "TICKET_OVERDUE_CRITICAL"
                    ))
            );
        }
    }
}