package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "room_inventory")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "package_count")
    private Integer packageCount;

    @Column(name = "total_quantity", precision = 38, scale = 2)
    private BigDecimal totalQuantity;

    @Column(name = "locked_quantity", precision = 38, scale = 2)
    private BigDecimal lockedQuantity;

    @Column(length = 255)
    private String note;
}