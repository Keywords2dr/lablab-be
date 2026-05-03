package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rent_tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RentTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID ticketId;

    @ManyToOne
    @JoinColumn(name = "requester_id")
    private User requester;

    @ManyToOne
    @JoinColumn(name = "from_room_id")
    private Room fromRoom;

    private String purposeType;
    private String subjectName;
    private String lessonDetail;
    private String classCode;

    private String status;

    @ManyToOne
    @JoinColumn(name = "owner_approved_by")
    private User ownerApprovedBy;

    @ManyToOne
    @JoinColumn(name = "admin_approved_by")
    private User adminApprovedBy;

    private LocalDateTime borrowDate;
    private LocalDateTime expectedReturnDate;
    private LocalDateTime createdDate;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL)
    private List<RentTicketDetail> ticketDetails;
}