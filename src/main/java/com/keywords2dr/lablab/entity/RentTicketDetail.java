package com.keywords2dr.lablab.entity;

import com.keywords2dr.lablab.entity.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "rent_ticket_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentTicketDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID detailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private RentTicket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // Số lượng mượn
    @Column(nullable = false)
    private BigDecimal quantityBorrowed;

    // Số lượng thực tế trả (điền khi trả)
    private BigDecimal quantityReturned;

    // Trạng thái trả từng dòng
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReturnStatus returnStatus = ReturnStatus.NOT_RETURNED;

    // Ghi chú khi trả (mô tả hỏng/mất/thiếu)
    private String returnNote;
}