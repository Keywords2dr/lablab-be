package com.keywords2dr.lablab.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keywords2dr.lablab.dto.ticket.*;
import com.keywords2dr.lablab.service.RentTicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RentTicketController.class)
class RentTicketControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private RentTicketService ticketService;

    // ================== SECURITY & VALIDATION TESTS ==================

    @Test
    @WithMockUser(roles = "STUDENT") // Student vào link của Teacher
    void teacherPending_Returns403_WhenStudentRole() throws Exception {
        mockMvc.perform(get("/api/tickets/teacher/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER") // Teacher vào link của Admin
    void adminAll_Returns403_WhenTeacherRole() throws Exception {
        mockMvc.perform(get("/api/tickets/admin/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser // User chưa đăng nhập
    void anyEndpoint_Returns401_WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/tickets/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void createTicket_Returns400_WhenMissingRequiredFields() throws Exception {
        RentTicketCreateRequest request = new RentTicketCreateRequest();
        // Không set roomId, borrowDate... để trigger @Valid lỗi

        mockMvc.perform(post("/api/tickets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}