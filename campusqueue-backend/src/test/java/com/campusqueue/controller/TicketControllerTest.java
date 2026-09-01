package com.campusqueue.controller;

import com.campusqueue.dto.request.CreateCounterRequest;
import com.campusqueue.dto.request.CreateTicketRequest;
import com.campusqueue.dto.request.CreateUserRequest;
import com.campusqueue.dto.response.CounterResponse;
import com.campusqueue.dto.response.UserResponse;
import com.campusqueue.entity.UserRole;
import com.campusqueue.repository.CounterRepository;
import com.campusqueue.repository.TicketRepository;
import com.campusqueue.repository.UserRepository;
import com.campusqueue.service.CounterService;
import com.campusqueue.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CounterService counterService;

    private UserResponse testUser;
    private CounterResponse testCounter;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userService.createUser(new CreateUserRequest("Test Student", "test.student@college.edu", UserRole.STUDENT));
        testCounter = counterService.createCounter(new CreateCounterRequest("Accounts Desk", "ACC", "Fees"));
    }

    @Test
    @DisplayName("POST /api/tickets - Issues a new ticket and returns 201 CREATED")
    void shouldCreateTicket() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(testCounter.getId(), testUser.getId());

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tokenNumber").value(1))
                .andExpect(jsonPath("$.formattedToken").value("ACC-001"))
                .andExpect(jsonPath("$.status").value("WAITING"))
                .andExpect(jsonPath("$.counterName").value("Accounts Desk"))
                .andExpect(jsonPath("$.userName").value("Test Student"));
    }

    @Test
    @DisplayName("GET /api/tickets/counter/{id}/status - Returns complete queue status")
    void shouldGetQueueStatus() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(testCounter.getId(), testUser.getId());
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/tickets/counter/" + testCounter.getId() + "/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterName").value("Accounts Desk"))
                .andExpect(jsonPath("$.totalWaiting").value(1))
                .andExpect(jsonPath("$.waitingTickets[0].formattedToken").value("ACC-001"));
    }

    @Test
    @DisplayName("POST /api/tickets/counter/{id}/call-next - Transitions ticket to CALLED")
    void shouldCallNextTicket() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(testCounter.getId(), testUser.getId());
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tickets/counter/" + testCounter.getId() + "/call-next")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"))
                .andExpect(jsonPath("$.calledAt").exists());
    }

    @Test
    @DisplayName("PATCH /api/tickets/{id}/complete - Transitions CALLED ticket to COMPLETED")
    void shouldCompleteTicket() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest(testCounter.getId(), testUser.getId());
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/tickets/counter/" + testCounter.getId() + "/call-next")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Long ticketId = ticketRepository.findAll().get(0).getId();

        mockMvc.perform(patch("/api/tickets/" + ticketId + "/complete")
                        .param("remarks", "Processed successfully"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.remarks").value("Processed successfully"))
                .andExpect(jsonPath("$.completedAt").exists());
    }
}
