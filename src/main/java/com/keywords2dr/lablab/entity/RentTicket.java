package com.keywords2dr.lablab.entity;

import com.keywords2dr.lablab.entity.enums.TicketStatus;
import com.keywords2dr.lablab.entity.enums.TicketType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rent_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID ticketId;

    // Người tạo phiếu (Teacher hoặc Student)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // Phòng mượn (1 phiếu = 1 phòng)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_room_id", nullable = false)
    private Room fromRoom;

    // Loại phiếu
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketType ticketType;

    // Trạng thái phiếu
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    // Thông tin mục đích mượn
    private String purposeType;     // TEACHING | RESEARCH | PERSONAL | OTHER
    private String subjectName;
    private String lessonDetail;
    private String classCode;

    // Thời gian mượn/trả — theo giờ
    @Column(nullable = false)
    private LocalDateTime borrowDate;

    @Column(nullable = false)
    private LocalDateTime expectedReturnDate;

    private LocalDateTime actualReturnDate;

    // Duyệt bởi Teacher (chủ phòng) — bước 1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_approved_by")
    private User ownerApprovedBy;

    private LocalDateTime ownerApprovedAt;

    // Duyệt bởi Admin — bước 2 (chỉ khi có hóa chất)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_approved_by")
    private User adminApprovedBy;

    private LocalDateTime adminApprovedAt;

    // Từ chối
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    private String rejectedReason;
    private LocalDateTime rejectedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // Chi tiết hóa chất mượn (null / rỗng nếu ROOM_ONLY)
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentTicketDetail> ticketDetails;
}