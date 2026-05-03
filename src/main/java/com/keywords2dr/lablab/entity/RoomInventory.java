package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "room_inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomInventory {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    private UUID inventoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "package_count")
    private Integer packageCount; // Số lượng chai/lọ

    @Column(name = "total_quantity", precision = 38, scale = 2)
    private BigDecimal totalQuantity; // Tổng khối lượng thực tế

    @Column(name = "locked_quantity", precision = 38, scale = 2)
    private BigDecimal lockedQuantity; // Lượng đang chờ duyệt mượn

    @Column(length = 255)
    private String note;
}