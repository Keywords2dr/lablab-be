package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID>, JpaSpecificationExecutor<Room> {

    boolean existsByRoomNameIgnoreCase(String roomName);

    long countByIsActive(Boolean isActive);

    long count();

    @Query("""
            SELECT COUNT(r) FROM Room r
            WHERE r.isActive = true
              AND NOT EXISTS (
                SELECT 1 FROM RoomStaffAssignment a WHERE a.room = r
              )
            """)
    long countRoomsWithoutStaff();

    @Query("SELECT COUNT(a) FROM RoomStaffAssignment a WHERE a.room.roomId = :roomId")
    int countStaffByRoomId(@Param("roomId") UUID roomId);

    @Query("""
            SELECT DISTINCT r FROM Room r
            LEFT JOIN FETCH r.staffAssignments sa
            LEFT JOIN FETCH sa.user
            WHERE r.roomId IN :ids
            """)
    List<Room> findAllByIdWithStaff(@Param("ids") Set<UUID> ids);

    Optional<Room> findByRoomNameIgnoreCase(String roomName);
}