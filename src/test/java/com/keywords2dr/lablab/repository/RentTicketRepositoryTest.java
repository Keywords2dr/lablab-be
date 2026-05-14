package com.keywords2dr.lablab.repository;

import com.keywords2dr.lablab.entity.*;
import com.keywords2dr.lablab.entity.enums.TicketStatus;
import com.keywords2dr.lablab.entity.enums.TicketType;
import com.keywords2dr.lablab.repository.specification.RentTicketSpecification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RentTicketRepositoryTest {

    @Autowired private RentTicketRepository rentTicketRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void findPendingTicketsByTeacher_ReturnsList() {
        User teacher = userRepository.save(new User());
        Room room = new Room(); room.setRoomName("Lab 1"); room.setIsActive(true);
        room = roomRepository.save(room);

        // Gán teacher vào phòng
        RoomStaffAssignment sa = new RoomStaffAssignment();
        sa.setUser(teacher); sa.setRoom(room);
        room.setStaffAssignments(List.of(sa));
        roomRepository.save(room);

        // Tạo 1 phiếu pending owner
        RentTicket ticket = new RentTicket();
        ticket.setFromRoom(room);
        ticket.setRequester(teacher);
        ticket.setTicketType(TicketType.ROOM_ONLY);
        ticket.setStatus(TicketStatus.PENDING_OWNER);
        ticket.setBorrowDate(LocalDateTime.now());
        ticket.setExpectedReturnDate(LocalDateTime.now().plusHours(1));
        rentTicketRepository.save(ticket);

        List<RentTicket> results = rentTicketRepository.findPendingTicketsByTeacher(teacher.getUserId());
        assertFalse(results.isEmpty());
        assertEquals(TicketStatus.PENDING_OWNER, results.get(0).getStatus());
    }

    @Test
    void findPendingTicketsByTeacher_ReturnsEmpty_WhenNoTickets() {
        List<RentTicket> results = rentTicketRepository.findPendingTicketsByTeacher(UUID.randomUUID());
        assertTrue(results.isEmpty());
    }

    @Test
    void findAll_WithSpecification_FiltersCorrectly() {
        Room room1 = new Room();
        room1.setRoomName("Room 1");
        room1.setIsActive(true);
        room1 = roomRepository.save(room1);

        Room room2 = new Room();
        room2.setRoomName("Room 2");
        room2.setIsActive(true);
        room2 = roomRepository.save(room2);

        User user = userRepository.save(new User());

        RentTicket ticket = new RentTicket();
        ticket.setFromRoom(room1);
        ticket.setRequester(user);
        ticket.setTicketType(TicketType.ROOM_ONLY);
        ticket.setStatus(TicketStatus.APPROVED);
        ticket.setBorrowDate(LocalDateTime.now());
        ticket.setExpectedReturnDate(LocalDateTime.now().plusHours(1));
        rentTicketRepository.save(ticket);

        // Test filter đúng roomId và status -> Phải ra 1
        Specification<RentTicket> specMatch = RentTicketSpecification.filter(room1.getRoomId(), TicketStatus.APPROVED, null, null);
        List<RentTicket> matchResult = rentTicketRepository.findAll(specMatch);
        assertEquals(1, matchResult.size());

        // Test filter sai roomId -> Phải ra 0
        Specification<RentTicket> specNotMatch = RentTicketSpecification.filter(room2.getRoomId(), TicketStatus.APPROVED, null, null);
        List<RentTicket> notMatchResult = rentTicketRepository.findAll(specNotMatch);
        assertTrue(notMatchResult.isEmpty());
    }
}