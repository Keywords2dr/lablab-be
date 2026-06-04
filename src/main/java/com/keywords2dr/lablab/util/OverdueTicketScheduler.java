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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class OverdueTicketScheduler {

    private final RentTicketRepository rentTicketRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── Cấu hình cooldown ─────────────────────────────────────────────────────

    /** Nhắc nhở sắp hết hạn: chỉ gửi 1 lần duy nhất cho mỗi phiếu. */
    /** Thông báo quá hạn thông thường: 3 tiếng mới gửi lại 1 lần. */
    private static final long OVERDUE_NOTIFY_COOLDOWN_HOURS = 3;

    /** Thông báo quá hạn nghiêm trọng (>60 phút, gửi cho Admin): 6 tiếng mới gửi lại 1 lần. */
    private static final long OVERDUE_CRITICAL_COOLDOWN_HOURS = 6;

    private final Map<UUID, LocalDateTime> lastOverdueNotified  = new ConcurrentHashMap<>();
    private final Map<UUID, LocalDateTime> lastCriticalNotified = new ConcurrentHashMap<>();
    private final Map<UUID, LocalDateTime> lastExpiryReminded   = new ConcurrentHashMap<>();

    // ── Nhắc nhở trước 30 phút ────────────────────────────────────────────────

    @Scheduled(fixedRate = 15 * 60 * 1000)
    @Transactional
    public void remindBeforeExpiry() {
        LocalDateTime now  = LocalDateTime.now();
        LocalDateTime soon = now.plusMinutes(30);

        List<RentTicket> soonExpiring = rentTicketRepository.findBorrowedExpiringBetween(now, soon);
        if (soonExpiring.isEmpty()) return;

        log.info("[SCHEDULER] Kiểm tra nhắc nhở sớm: {} phiếu sắp hết hạn trong 30 phút.",
                soonExpiring.size());

        for (RentTicket ticket : soonExpiring) {
            try {
                // Chỉ gửi 1 lần duy nhất, không lặp lại mỗi 15 phút
                if (lastExpiryReminded.containsKey(ticket.getTicketId())) {
                    continue;
                }
                remindExpirySoon(ticket, now);
                lastExpiryReminded.put(ticket.getTicketId(), now);
            } catch (Exception e) {
                log.error("[SCHEDULER] Lỗi nhắc nhở phiếu {}: {}",
                        ticket.getTicketId(), e.getMessage());
            }
        }
    }

    // ── Xử lý phiếu quá hạn ──────────────────────────────────────────────────

    @Scheduled(fixedRate = 15 * 60 * 1000, initialDelay = 7 * 60 * 1000)
    @Transactional
    public void processOverdueTickets() {
        LocalDateTime now = LocalDateTime.now();

        List<RentTicket> overdue = rentTicketRepository.findBorrowedExpiredBefore(now);

        if (overdue.isEmpty()) {
            // Dọn Map khi không còn phiếu quá hạn
            lastOverdueNotified.clear();
            lastCriticalNotified.clear();
            return;
        }

        log.warn("[SCHEDULER] Phát hiện {} phiếu quá hạn.", overdue.size());

        // Dọn các ticketId đã hết quá hạn (trả xong / hủy)
        var overdueIds = overdue.stream().map(RentTicket::getTicketId).toList();
        lastOverdueNotified.keySet().retainAll(overdueIds);
        lastCriticalNotified.keySet().retainAll(overdueIds);

        for (RentTicket ticket : overdue) {
            try {
                handleOverdueTicket(ticket, now);
            } catch (Exception e) {
                log.error("[SCHEDULER] Lỗi xử lý phiếu quá hạn {}: {}",
                        ticket.getTicketId(), e.getMessage());
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void remindExpirySoon(RentTicket ticket, LocalDateTime now) {
        String roomName  = ticket.getFromRoom().getRoomName();
        String borrower  = UserNameResolver.resolve(ticket.getRequester());
        long minutesLeft = ChronoUnit.MINUTES.between(now, ticket.getExpectedReturnDate());

        // Chỉ gửi cho người mượn
        eventPublisher.publishEvent(new NotificationEvent(
                ticket.getRequester().getUserId(),
                "Nhắc nhở: Sắp đến hạn trả",
                String.format(
                        "Phiếu mượn tại phòng %s của bạn sẽ hết hạn sau khoảng %d phút. "
                                + "Vui lòng yêu cầu trả sớm để tránh quá hạn!",
                        roomName, minutesLeft),
                "TICKET_EXPIRY_REMINDER"
        ));

        // Chỉ gửi cho staff phòng đó
        if (ticket.getFromRoom().getStaffAssignments() != null) {
            ticket.getFromRoom().getStaffAssignments().forEach(sa ->
                    eventPublisher.publishEvent(new NotificationEvent(
                            sa.getUser().getUserId(),
                            "Phiếu mượn sắp hết hạn",
                            String.format("[%s] có phiếu mượn tại phòng %s sẽ hết hạn sau %d phút.",
                                    borrower, roomName, minutesLeft),
                            "TICKET_EXPIRY_REMINDER_STAFF"
                    ))
            );
        }
    }

    private void handleOverdueTicket(RentTicket ticket, LocalDateTime now) {
        UUID ticketId       = ticket.getTicketId();
        String roomName     = ticket.getFromRoom().getRoomName();
        String borrower     = UserNameResolver.resolve(ticket.getRequester());
        long overdueMinutes = ChronoUnit.MINUTES.between(ticket.getExpectedReturnDate(), now);

        // ── Thông báo quá hạn thông thường (cooldown 3 tiếng) ────────────────
        LocalDateTime lastOverdue = lastOverdueNotified.get(ticketId);
        boolean canSendOverdue = lastOverdue == null
                || ChronoUnit.HOURS.between(lastOverdue, now) >= OVERDUE_NOTIFY_COOLDOWN_HOURS;

        if (canSendOverdue) {
            log.warn("[SCHEDULER] Gửi thông báo quá hạn — phiếu: {}, {} phút, phòng: [{}]",
                    ticketId, overdueMinutes, roomName);

            // Gửi cho người mượn
            eventPublisher.publishEvent(new NotificationEvent(
                    ticket.getRequester().getUserId(),
                    "Phiếu mượn đã QUÁ HẠN",
                    String.format(
                            "Phiếu mượn tại phòng %s của bạn đã QUÁ HẠN %d phút (hạn trả: %s). "
                                    + "Vui lòng yêu cầu trả ngay hoặc liên hệ Teacher!",
                            roomName, overdueMinutes, ticket.getExpectedReturnDate()),
                    "TICKET_OVERDUE"
            ));

            // Gửi cho staff phòng đó
            if (ticket.getFromRoom().getStaffAssignments() != null) {
                ticket.getFromRoom().getStaffAssignments().forEach(sa ->
                        eventPublisher.publishEvent(new NotificationEvent(
                                sa.getUser().getUserId(),
                                "Phiếu mượn quá hạn",
                                String.format(
                                        "[%s] có phiếu mượn tại phòng %s đã QUÁ HẠN %d phút. "
                                                + "Vui lòng nhắc người mượn trả lại.",
                                        borrower, roomName, overdueMinutes),
                                "TICKET_OVERDUE_STAFF"
                        ))
                );
            }

            lastOverdueNotified.put(ticketId, now);
        } else {
            long nextNotifyIn = OVERDUE_NOTIFY_COOLDOWN_HOURS
                    - ChronoUnit.HOURS.between(lastOverdue, now);
            log.debug("[SCHEDULER] Bỏ qua phiếu {} — còn ~{} tiếng nữa mới gửi lại thông báo.",
                    ticketId, nextNotifyIn);
        }

        // ── Thông báo nghiêm trọng cho Admin (cooldown 6 tiếng, chỉ khi >60 phút) ──
        if (overdueMinutes > 60) {
            LocalDateTime lastCritical = lastCriticalNotified.get(ticketId);
            boolean canSendCritical = lastCritical == null
                    || ChronoUnit.HOURS.between(lastCritical, now) >= OVERDUE_CRITICAL_COOLDOWN_HOURS;

            if (canSendCritical) {
                userRepository.findAllByRole("ADMIN").forEach(admin ->
                        eventPublisher.publishEvent(new NotificationEvent(
                                admin.getUserId(),
                                "Cảnh báo: Phiếu quá hạn nghiêm trọng",
                                String.format(
                                        "[%s] có phiếu mượn tại phòng %s đã QUÁ HẠN hơn %d phút "
                                                + "nhưng chưa được yêu cầu trả. Cần xem xét xử lý thủ công.",
                                        borrower, roomName, overdueMinutes),
                                "TICKET_OVERDUE_CRITICAL"
                        ))
                );
                lastCriticalNotified.put(ticketId, now);
            }
        }
    }
}