package com.keywords2dr.lablab.service.impl;

import com.keywords2dr.lablab.dto.dashboard.ActivityFeedItemDTO;
import com.keywords2dr.lablab.dto.dashboard.DailyTicketStatsDTO;
import com.keywords2dr.lablab.dto.dashboard.RoomCurrentUsageDTO;
import com.keywords2dr.lablab.entity.AuditLog;
import com.keywords2dr.lablab.entity.RentTicket;
import com.keywords2dr.lablab.entity.Room;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import com.keywords2dr.lablab.repository.AuditLogRepository;
import com.keywords2dr.lablab.repository.RentTicketRepository;
import com.keywords2dr.lablab.repository.RoomRepository;
import com.keywords2dr.lablab.service.DashboardService;
import com.keywords2dr.lablab.util.UserNameResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final RentTicketRepository rentTicketRepository;
    private final RoomRepository roomRepository;
    private final AuditLogRepository auditLogRepository;

    // ── Nhóm status cho "đang xử lý" ──────────────────────────────────────────
    private static final List<TicketStatus> PENDING_STATUSES = List.of(
            TicketStatus.PENDING_OWNER,
            TicketStatus.PENDING_ADMIN,
            TicketStatus.BORROWED,
            TicketStatus.PENDING_RETURN
    );

    private static final List<TicketStatus> APPROVED_STATUSES = List.of(
            TicketStatus.APPROVED,
            TicketStatus.RETURNED
    );

    private static final List<TicketStatus> REJECTED_STATUSES = List.of(
            TicketStatus.REJECTED,
            TicketStatus.CANCELLED
    );

    // ── Nhãn thứ tiếng Việt ───────────────────────────────────────────────────
    private static final String[] DAY_LABELS = {"CN", "T2", "T3", "T4", "T5", "T6", "T7"};

    // ══════════════════════════════════════════════════════════════════════════
    // 1. WEEKLY TICKET STATS
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<DailyTicketStatsDTO> getWeeklyTicketStats() {
        // 7 ngày: từ 6 ngày trước đến hôm nay
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);

        List<DailyTicketStatsDTO> result = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = startDate.plusDays(i);
            LocalDateTime from = date.atStartOfDay();
            LocalDateTime to   = date.plusDays(1).atStartOfDay();

            long approved = rentTicketRepository.countByCreatedAtBetweenAndStatusIn(
                    from, to, APPROVED_STATUSES);
            long rejected = rentTicketRepository.countByCreatedAtBetweenAndStatusIn(
                    from, to, REJECTED_STATUSES);
            long pending  = rentTicketRepository.countByCreatedAtBetweenAndStatusIn(
                    from, to, PENDING_STATUSES);

            // DayOfWeek: MONDAY=1, …, SUNDAY=7 → map sang index 1-7 của mảng DAY_LABELS
            int dow = date.getDayOfWeek().getValue(); // 1=Mon … 7=Sun
            // DAY_LABELS index: 0=CN, 1=T2, …, 6=T7
            // dow 1(Mon)→1, 2(Tue)→2, …, 6(Sat)→6, 7(Sun)→0
            int labelIndex = (dow == 7) ? 0 : dow;
            String label = DAY_LABELS[labelIndex];

            result.add(DailyTicketStatsDTO.builder()
                    .date(date)
                    .dayLabel(label)
                    .approved(approved)
                    .rejected(rejected)
                    .pending(pending)
                    .build());
        }

        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. ROOM CURRENT USAGE
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<RoomCurrentUsageDTO> getCurrentRoomUsage() {
        // Lấy tất cả phòng
        List<Room> allRooms = roomRepository.findAll();

        // Lấy tất cả phiếu đang BORROWED — JOIN FETCH để tránh N+1
        List<RentTicket> borrowedTickets = rentTicketRepository
                .findAllBorrowedWithRoomAndRequester();

        // Map: roomId → ticket đang BORROWED (1 phòng tại 1 thời điểm chỉ có 1 phiếu BORROWED)
        Map<UUID, RentTicket> borrowedByRoom = borrowedTickets.stream()
                .collect(Collectors.toMap(
                        t -> t.getFromRoom().getRoomId(),
                        t -> t,
                        // Nếu trùng roomId (edge case), lấy phiếu mượn gần nhất
                        (a, b) -> a.getBorrowDate().isAfter(b.getBorrowDate()) ? a : b
                ));

        return allRooms.stream()
                .map(room -> buildRoomUsageDTO(room, borrowedByRoom.get(room.getRoomId())))
                .sorted(Comparator.comparing(RoomCurrentUsageDTO::getStatus))
                .collect(Collectors.toList());
    }

    private RoomCurrentUsageDTO buildRoomUsageDTO(Room room, RentTicket activeTicket) {
        // Phòng bị khoá → maintenance
        if (Boolean.FALSE.equals(room.getIsActive())) {
            return RoomCurrentUsageDTO.builder()
                    .roomId(room.getRoomId())
                    .roomName(room.getRoomName())
                    .status("maintenance")
                    .build();
        }

        // Phòng đang có người mượn → occupied
        if (activeTicket != null) {
            String borrowerName = UserNameResolver.resolve(activeTicket.getRequester());
            return RoomCurrentUsageDTO.builder()
                    .roomId(room.getRoomId())
                    .roomName(room.getRoomName())
                    .status("occupied")
                    .activeTicketId(activeTicket.getTicketId())
                    .borrowerName(borrowerName)
                    .expectedReturnDate(activeTicket.getExpectedReturnDate())
                    .ticketType(activeTicket.getTicketType() != null
                            ? activeTicket.getTicketType().name() : null)
                    .build();
        }

        // Phòng trống
        return RoomCurrentUsageDTO.builder()
                .roomId(room.getRoomId())
                .roomName(room.getRoomName())
                .status("available")
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. ACTIVITY FEED
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ActivityFeedItemDTO> getActivityFeed(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50); // clamp 1-50

        List<AuditLog> logs = auditLogRepository.findAll(
                PageRequest.of(0, safeLimit, Sort.by("createdAt").descending())
        ).getContent();

        return logs.stream()
                .map(this::toFeedItem)
                .collect(Collectors.toList());
    }

    /**
     * Dịch AuditLog kỹ thuật → ActivityFeedItemDTO thân thiện tiếng Việt.
     */
    private ActivityFeedItemDTO toFeedItem(AuditLog log) {
        String category = resolveCategory(log.getEntityName());
        String description = buildDescription(log);
        String timeAgo = formatTimeAgo(log.getCreatedAt());

        return ActivityFeedItemDTO.builder()
                .logId(log.getLogId())
                .category(category)
                .description(description)
                .actorName(log.getActorUsername() != null ? log.getActorUsername() : "Hệ thống")
                .actorRole(log.getActorRole() != null ? log.getActorRole() : "SYSTEM")
                .createdAt(log.getCreatedAt())
                .timeAgo(timeAgo)
                .build();
    }

    /** Map entityName → icon category cho FE */
    private String resolveCategory(String entityName) {
        if (entityName == null) return "SYSTEM";
        return switch (entityName.toUpperCase()) {
            case "RENT_TICKET"           -> "TICKET";
            case "ROOM", "ROOM_STAFF"    -> "ROOM";
            case "CHEMICAL",
                 "CHEMICAL_IMPORT",
                 "ROOM_INVENTORY"        -> "CHEMICAL";
            case "USER"                  -> "USER";
            default                      -> "SYSTEM";
        };
    }

    /**
     * Tạo mô tả thân thiện.
     * Quy tắc: [actor] đã [hành động] [đối tượng]
     */
    private String buildDescription(AuditLog log) {
        String actor = (log.getActorUsername() != null && !log.getActorUsername().equals("System"))
                ? log.getActorUsername()
                : "Hệ thống";

        String entity = log.getEntityName() != null ? log.getEntityName().toUpperCase() : "";
        String action = log.getAction()     != null ? log.getAction().toUpperCase()     : "";

        return switch (entity) {
            case "RENT_TICKET" -> switch (action) {
                case "CREATE"          -> actor + " đã tạo phiếu mượn mới";
                case "CANCEL"          -> actor + " đã hủy phiếu mượn";
                case "TEACHER_APPROVE" -> actor + " (Teacher) đã xử lý phiếu mượn";
                case "ADMIN_APPROVE"   -> actor + " (Admin) đã duyệt/từ chối phiếu mượn";
                case "ACTIVATE"        -> actor + " đã bàn giao thiết bị cho người mượn";
                case "REQUEST_RETURN"  -> actor + " đã yêu cầu trả đồ";
                case "CONFIRM_RETURN"  -> actor + " đã xác nhận nhận lại đồ";
                case "RETURN_ISSUE"    -> actor + " ghi nhận vấn đề khi trả đồ";
                default                -> actor + " đã thao tác phiếu mượn";
            };
            case "ROOM" -> switch (action) {
                case "CREATE"     -> actor + " đã tạo phòng Lab mới";
                case "UPDATE"     -> actor + " đã cập nhật thông tin phòng";
                case "ACTIVATE"   -> actor + " đã mở lại phòng Lab";
                case "DEACTIVATE" -> actor + " đã tạm ngưng phòng Lab";
                default           -> actor + " đã thao tác phòng Lab";
            };
            case "ROOM_STAFF" -> switch (action) {
                case "ASSIGN_STAFF" -> actor + " đã phân công giáo viên quản lý phòng";
                case "REMOVE_STAFF" -> actor + " đã gỡ giáo viên khỏi phòng";
                default             -> actor + " đã thao tác phân công phòng";
            };
            case "CHEMICAL" -> switch (action) {
                case "CREATE"  -> actor + " đã thêm hóa chất mới";
                case "UPDATE"  -> actor + " đã cập nhật thông tin hóa chất";
                case "DELETE"  -> actor + " đã ẩn hóa chất khỏi hệ thống";
                case "RESTORE" -> actor + " đã khôi phục hóa chất";
                default        -> actor + " đã thao tác hóa chất";
            };
            case "CHEMICAL_IMPORT" -> actor + " đã import hóa chất từ file Excel";
            case "ROOM_INVENTORY"  -> switch (action) {
                case "ALLOCATE_INVENTORY" -> actor + " đã phân bổ vật tư vào phòng";
                case "REVOKE_INVENTORY"   -> actor + " đã thu hồi vật tư khỏi phòng";
                default                  -> actor + " đã thao tác kho vật tư";
            };
            case "USER" -> switch (action) {
                case "CREATE"          -> actor + " đã tạo tài khoản người dùng mới";
                case "UPDATE"          -> actor + " đã cập nhật thông tin người dùng";
                case "UPDATE_STATUS"   -> actor + " đã thay đổi trạng thái tài khoản";
                case "RESET_PASSWORD"  -> actor + " đã đặt lại mật khẩu người dùng";
                case "CHANGE_PASSWORD" -> actor + " đã đổi mật khẩu";
                case "UPDATE_MY_PROFILE" -> actor + " đã cập nhật hồ sơ cá nhân";
                default                -> actor + " đã thao tác tài khoản";
            };
            default -> actor + " đã thực hiện thao tác: " + log.getAction();
        };
    }

    /**
     * Định dạng thời gian tương đối, VD: "5 phút trước", "2 giờ trước".
     */
    private String formatTimeAgo(LocalDateTime time) {
        if (time == null) return "";
        LocalDateTime now = LocalDateTime.now();
        long seconds = ChronoUnit.SECONDS.between(time, now);

        if (seconds < 60)               return "Vừa xong";
        if (seconds < 3600)             return (seconds / 60)   + " phút trước";
        if (seconds < 86400)            return (seconds / 3600) + " giờ trước";
        if (seconds < 86400 * 7)        return (seconds / 86400) + " ngày trước";
        if (seconds < 86400 * 30)       return (seconds / (86400 * 7)) + " tuần trước";
        return (seconds / (86400 * 30)) + " tháng trước";
    }
}