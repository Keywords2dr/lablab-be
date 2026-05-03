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

    private String roomName;
    private String description;
    private Boolean isActive;

    @OneToMany(mappedBy = "room")
    private List<RoomInventory> inventories;
}