package com.keywords2dr.lablab.service;

import com.keywords2dr.lablab.entity.RentTicket;
import com.keywords2dr.lablab.entity.RoomInventory;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import com.keywords2dr.lablab.repository.RentTicketRepository;
import com.keywords2dr.lablab.repository.RoomInventoryRepository;
import com.keywords2dr.lablab.repository.RoomStaffAssignmentRepository;
import com.keywords2dr.lablab.util.UserNameResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatContextService {

    private final RentTicketRepository rentTicketRepository;
    private final RoomInventoryRepository roomInventoryRepository;
    private final RoomStaffAssignmentRepository roomStaffAssignmentRepository;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Context chung (dùng cho mọi role) ────────────────────────────────────

    @Transactional(readOnly = true)
    public String buildContext() {
        return buildRoomCurrentStatus()
                + buildUpcomingSchedule()
                + buildChemicalInventory();
    }

    // ── Context bổ sung riêng cho TEACHER ─────────────────────────────────────

    /**
     * Tóm tắt trạng thái các phiếu mượn thuộc phòng mà teacher đang quản lý.
     * Bao gồm số lượng theo từng trạng thái và danh sách chi tiết cần xử lý.
     */
    @Transactional(readOnly = true)
    public String buildTeacherTicketSummary(UUID teacherId) {
        // Lấy danh sách các phòng teacher đang quản lý
        var assignments = roomStaffAssignmentRepository.findAllByUser_UserId(teacherId);

        if (assignments.isEmpty()) {
            return "\n### THỐNG KÊ PHIẾU MƯỢN (dành cho Giảng viên)\n"
                    + "- Bạn chưa được phân công quản lý phòng nào.\n";
        }

        List<UUID> managedRoomIds = assignments.stream()
                .map(a -> a.getRoom().getRoomId())
                .collect(Collectors.toList());

        String managedRoomNames = assignments.stream()
                .map(a -> a.getRoom().getRoomName())
                .collect(Collectors.joining(", "));

        // Lấy tất cả phiếu theo từng trạng thái cần quan tâm
        List<RentTicket> pendingOwner   = getTicketsByRoomsAndStatus(managedRoomIds, TicketStatus.PENDING_OWNER);
        List<RentTicket> pendingReturn  = getTicketsByRoomsAndStatus(managedRoomIds, TicketStatus.PENDING_RETURN);
        List<RentTicket> borrowed       = getTicketsByRoomsAndStatus(managedRoomIds, TicketStatus.BORROWED);
        List<RentTicket> approved       = getTicketsByRoomsAndStatus(managedRoomIds, TicketStatus.APPROVED);
        List<RentTicket> pendingAdmin   = getTicketsByRoomsAndStatus(managedRoomIds, TicketStatus.PENDING_ADMIN);

        StringBuilder sb = new StringBuilder();
        sb.append("\n### THỐNG KÊ PHIẾU MƯỢN (dành cho Giảng viên)\n");
        sb.append(String.format("Phòng bạn quản lý: [%s]\n\n", managedRoomNames));

        // Tổng hợp số lượng
        sb.append("**Tổng quan:**\n");
        sb.append(String.format("- Chờ bạn duyệt (PENDING_OWNER): %d phiếu\n", pendingOwner.size()));
        sb.append(String.format("- Đang yêu cầu trả (PENDING_RETURN): %d phiếu\n", pendingReturn.size()));
        sb.append(String.format("- Đang được mượn (BORROWED): %d phiếu\n", borrowed.size()));
        sb.append(String.format("- Đã duyệt, chờ bàn giao (APPROVED): %d phiếu\n", approved.size()));
        sb.append(String.format("- Chờ Admin duyệt thêm (PENDING_ADMIN): %d phiếu\n", pendingAdmin.size()));

        // Chi tiết các phiếu cần xử lý ngay
        appendTicketDetail(sb, "Phiếu chờ bạn duyệt", pendingOwner);
        appendTicketDetail(sb, "Phiếu đang yêu cầu trả (cần xác nhận)", pendingReturn);
        appendTicketDetail(sb, "Phiếu đã duyệt, chờ bàn giao cho người mượn", approved);

        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<RentTicket> getTicketsByRoomsAndStatus(List<UUID> roomIds, TicketStatus status) {
        return rentTicketRepository.findAllByStatusFetched(status).stream()
                .filter(t -> roomIds.contains(t.getFromRoom().getRoomId()))
                .collect(Collectors.toList());
    }

    private void appendTicketDetail(StringBuilder sb, String title, List<RentTicket> tickets) {
        if (tickets.isEmpty()) return;

        sb.append(String.format("\n**%s (%d phiếu):**\n", title, tickets.size()));
        for (RentTicket t : tickets) {
            sb.append(String.format(
                    "  - Phòng [%s] | Người mượn: %s | Loại: %s | Từ: %s → %s\n",
                    t.getFromRoom().getRoomName(),
                    UserNameResolver.resolve(t.getRequester()),
                    t.getTicketType().name(),
                    t.getBorrowDate() != null ? t.getBorrowDate().format(DT_FMT) : "?",
                    t.getExpectedReturnDate() != null ? t.getExpectedReturnDate().format(DT_FMT) : "?"
            ));
        }
    }

    // ── 1. Phòng đang được sử dụng (BORROWED) ────────────────────────────────

    private String buildRoomCurrentStatus() {
        List<RentTicket> borrowed = rentTicketRepository
                .findAllBorrowedWithRoomAndRequester();

        StringBuilder sb = new StringBuilder();
        sb.append("\n### PHÒNG ĐANG ĐƯỢC SỬ DỤNG (trạng thái BORROWED)\n");

        if (borrowed.isEmpty()) {
            sb.append("- Hiện tại không có phòng nào đang được mượn.\n");
        } else {
            for (RentTicket t : borrowed) {
                sb.append(String.format(
                        "- Phòng [%s]: người mượn=[%s], loại=[%s], hạn trả=[%s]\n",
                        t.getFromRoom().getRoomName(),
                        UserNameResolver.resolve(t.getRequester()),
                        t.getTicketType().name(),
                        t.getExpectedReturnDate().format(DT_FMT)
                ));
            }
        }
        return sb.toString();
    }

    // ── 2. Lịch mượn sắp tới (7 ngày tiếp theo) ──────────────────────────────

    private String buildUpcomingSchedule() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in7Days = now.plusDays(7);

        List<TicketStatus> activeStatuses = List.of(
                TicketStatus.APPROVED,
                TicketStatus.PENDING_OWNER,
                TicketStatus.PENDING_ADMIN,
                TicketStatus.BORROWED
        );

        List<RentTicket> upcoming = activeStatuses.stream()
                .flatMap(s -> rentTicketRepository.findAllByStatusFetched(s).stream())
                .filter(t -> t.getBorrowDate() != null
                        && t.getBorrowDate().isAfter(now)
                        && t.getBorrowDate().isBefore(in7Days))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("\n### LỊCH MƯỢN PHÒNG 7 NGÀY TỚI (APPROVED / PENDING)\n");

        if (upcoming.isEmpty()) {
            sb.append("- Không có lịch mượn nào trong 7 ngày tới.\n");
        } else {
            for (RentTicket t : upcoming) {
                sb.append(String.format(
                        "- Phòng [%s]: người mượn=[%s], từ=[%s] đến=[%s], trạng thái=[%s]\n",
                        t.getFromRoom().getRoomName(),
                        UserNameResolver.resolve(t.getRequester()),
                        t.getBorrowDate().format(DT_FMT),
                        t.getExpectedReturnDate().format(DT_FMT),
                        t.getStatus().name()
                ));
            }
        }
        return sb.toString();
    }

    // ── 3. Tồn kho hóa chất (> 0) ─────────────────────────────────────────────

    private String buildChemicalInventory() {
        List<RoomInventory> stocks = roomInventoryRepository
                .findAllPositiveStockWithItemAndRoom();

        StringBuilder sb = new StringBuilder();
        sb.append("\n### TỒN KHO HÓA CHẤT (chỉ các hóa chất đang có hàng)\n");

        if (stocks.isEmpty()) {
            sb.append("- Hiện tại không có hóa chất nào trong kho.\n");
        } else {
            var grouped = stocks.stream().collect(
                    Collectors.groupingBy(ri -> ri.getItem().getName()));

            for (var entry : grouped.entrySet()) {
                String chemName = entry.getKey();
                List<RoomInventory> rooms = entry.getValue();

                String roomDetails = rooms.stream()
                        .map(ri -> String.format("phòng %s: %.2f %s (khóa: %.2f)",
                                ri.getRoom().getRoomName(),
                                ri.getTotalQuantity(),
                                ri.getItem().getUnit(),
                                ri.getLockedQuantity()))
                        .collect(Collectors.joining(" | "));

                sb.append(String.format("- [%s] (%s): %s\n",
                        chemName,
                        rooms.getFirst().getItem().getItemCode(),
                        roomDetails));
            }
        }
        return sb.toString();
    }
}