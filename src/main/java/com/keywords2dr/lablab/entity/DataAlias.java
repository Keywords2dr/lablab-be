package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "data_aliases")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DataAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Phân loại: "PACKAGING" (Đóng gói), "UNIT" (Đơn vị tính)
    @Column(name = "alias_type", nullable = false)
    private String aliasType;

    // Từ gõ sai
    @Column(name = "wrong_term", nullable = false, unique = true)
    private String wrongTerm;

    // Từ chuẩn hóa (Ví dụ: "Thủy tinh", "Nhựa", "L")
    @Column(name = "standard_term", nullable = false)
    private String standardTerm;
}