package com.keywords2dr.lablab.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID roomId;

    @Column(name = "room_name", nullable = false)
    private String roomName;

    private String description;

    @Column(name = "is_active")
    private Boolean isActive;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomInventory> inventories;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomStaffAssignment> staffAssignments;
}