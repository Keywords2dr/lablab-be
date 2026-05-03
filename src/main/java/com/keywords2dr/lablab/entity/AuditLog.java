package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID logId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user; // Ai là người thực hiện hành động

    private String action; // VD: "MƯỢN_HÓA_CHẤT", "CẬP_NHẬT_PHÒNG"
    private String tableName; // Tên bảng bị tác động
    private UUID recordId; // ID của dòng dữ liệu bị tác động

    // Dùng jsonb của PostgreSQL để lưu cấu trúc linh hoạt
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String newData;

    @Column(updatable = false)
    private LocalDateTime createdAt;
}