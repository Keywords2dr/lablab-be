package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_thresholds",
        uniqueConstraints = @UniqueConstraint(columnNames = "item_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockThreshold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "min_quantity", nullable = false, precision = 38, scale = 2)
    private BigDecimal minQuantity;

    @Column(length = 255)
    private String note;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}