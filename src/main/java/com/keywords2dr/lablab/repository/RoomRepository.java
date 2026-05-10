package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID>, JpaSpecificationExecutor<Room> {

    boolean existsByRoomNameIgnoreCase(String roomName);

    long countByIsActive(Boolean isActive);

    @Query("""
            SELECT COUNT(r) FROM Room r
            WHERE r.isActive = true
              AND NOT EXISTS (
                SELECT 1 FROM RoomStaffAssignment a WHERE a.room = r
              )
            """)
    long countRoomsWithoutStaff();
}