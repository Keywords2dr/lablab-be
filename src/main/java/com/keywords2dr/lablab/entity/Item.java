package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "items")
@Inheritance(strategy = InheritanceType.JOINED)
@SQLRestriction("is_deleted = false")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "item_id", updatable = false, nullable = false)
    private UUID itemId;

    @Column(name = "item_code", unique = true, nullable = false)
    private String itemCode;

    @Column(nullable = false)
    private String name;

    @Column(name = "category_type", nullable = false)
    private String categoryType; // CHEMICAL, DEVICE, TOOL

    @Column(nullable = false)
    private String unit; // Đơn vị gốc (g, ml, cái...)

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false; // Mặc định là false (chưa xóa)
}