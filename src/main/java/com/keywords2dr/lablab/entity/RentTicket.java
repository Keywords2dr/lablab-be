package com.keywords2dr.lablab.entity;

import com.keywords2dr.lablab.entity.enums.PurposeType;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import com.keywords2dr.lablab.entity.enums.TicketType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@NamedEntityGraphs({
        @NamedEntityGraph(
                name = "RentTicket.summary",
                attributeNodes = {
                        @NamedAttributeNode(value = "requester", subgraph = "requester-profile"),
                        @NamedAttributeNode("fromRoom")
                },
                subgraphs = {
                        @NamedSubgraph(
                                name = "requester-profile",
                                attributeNodes = @NamedAttributeNode("profile")
                        )
                }
        ),

        @NamedEntityGraph(
                name = "RentTicket.full",
                attributeNodes = {
                        @NamedAttributeNode(value = "requester", subgraph = "requester-profile"),
                        @NamedAttributeNode("fromRoom"),
                        @NamedAttributeNode(value = "ticketDetails", subgraph = "detail-item")
                },
                subgraphs = {
                        @NamedSubgraph(
                                name = "requester-profile",
                                attributeNodes = @NamedAttributeNode("profile")
                        ),
                        @NamedSubgraph(
                                name = "detail-item",
                                attributeNodes = @NamedAttributeNode("item")
                        )
                }
        )
})
@Entity
@Table(name = "rent_tickets", indexes = {
        @Index(name = "idx_ticket_requester",      columnList = "requester_id"),
        @Index(name = "idx_ticket_status",         columnList = "status"),
        @Index(name = "idx_ticket_room",           columnList = "from_room_id"),
        @Index(name = "idx_ticket_created",        columnList = "created_at"),
        @Index(name = "idx_ticket_expected_return",columnList = "expected_return_date")
})
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
    @Builder.Default
    private Set<RentTicketDetail> ticketDetails = new LinkedHashSet<>();

    /**
     * Helper trả về ticketDetails dưới dạng List (tiện dùng trong service/mapper).
     */
    public List<RentTicketDetail> getTicketDetailsList() {
        if (ticketDetails == null) return new ArrayList<>();
        return new ArrayList<>(ticketDetails);
    }
}