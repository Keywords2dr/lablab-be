package com.keywords2dr.lablab.entity;

import com.keywords2dr.lablab.entity.enums.PurposeType;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_room_id", nullable = false)
    private Room fromRoom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketType ticketType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    @Enumerated(EnumType.STRING)
    private PurposeType purposeType;

    private String subjectName;
    private String lessonDetail;
    private String classCode;

    private String note;

    @Column(nullable = false)
    private LocalDateTime borrowDate;

    @Column(nullable = false)
    private LocalDateTime expectedReturnDate;

    private LocalDateTime actualReturnDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_approved_by")
    private User ownerApprovedBy;

    private LocalDateTime ownerApprovedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_approved_by")
    private User adminApprovedBy;

    private LocalDateTime adminApprovedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by")
    private User rejectedBy;

    private String rejectedReason;
    private LocalDateTime rejectedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentTicketDetail> ticketDetails;
}