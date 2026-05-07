package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.RoomStaffAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomStaffAssignmentRepository extends JpaRepository<RoomStaffAssignment, UUID> {

    List<RoomStaffAssignment> findAllByRoom_RoomId(UUID roomId);

    boolean existsByRoom_RoomIdAndUser_UserId(UUID roomId, UUID userId);

    Optional<RoomStaffAssignment> findByRoom_RoomIdAndUser_UserId(UUID roomId, UUID userId);
}