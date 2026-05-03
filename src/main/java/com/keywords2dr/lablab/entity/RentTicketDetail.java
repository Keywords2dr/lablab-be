package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "rent_ticket_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RentTicketDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID detailId;

    @ManyToOne
    @JoinColumn(name = "ticket_id")
    private RentTicket ticket;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    private BigDecimal quantityBorrowed;
    private String returnStatus;
}