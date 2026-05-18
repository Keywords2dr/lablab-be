package com.keywords2dr.lablab.entity;

import com.keywords2dr.lablab.entity.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@NamedEntityGraph(
        name = "ReportTicket.withRelations",
        attributeNodes = {
                @NamedAttributeNode("reporter"),
                @NamedAttributeNode("room"),
                @NamedAttributeNode("item")
        }
)
@Entity
@Table(name = "report_tickets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = true)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false, length = 1000)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void validateBeforePersist() {
        if (reportType == null) return; // Bean Validation sẽ bắt lỗi này trước

        switch (reportType) {
            case ROOM -> {
                if (room == null)
                    throw new IllegalStateException("Báo cáo phòng bắt buộc phải có room");
                if (item != null)
                    throw new IllegalStateException("Báo cáo phòng không được có item");
            }
            case CHEMICAL -> {
                if (room == null)
                    throw new IllegalStateException("Báo cáo hóa chất bắt buộc phải có room");
                if (item == null)
                    throw new IllegalStateException("Báo cáo hóa chất bắt buộc phải có item");
            }
            default -> throw new IllegalStateException("ReportType không hợp lệ: " + reportType);
        }
    }
}