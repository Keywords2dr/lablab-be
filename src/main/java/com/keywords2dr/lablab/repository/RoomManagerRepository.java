package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.RoomManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomManagerRepository extends JpaRepository<RoomManager, UUID> {

    List<RoomManager> findAllByRoom_RoomId(UUID roomId);

    boolean existsByRoom_RoomIdAndUser_UserId(UUID roomId, UUID userId);

    Optional<RoomManager> findByRoom_RoomIdAndUser_UserId(UUID roomId, UUID userId);
}