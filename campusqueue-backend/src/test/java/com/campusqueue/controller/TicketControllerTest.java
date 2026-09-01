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
import com.campusqueue.service.TicketService;
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

    @Autowired
    private TicketService ticketService;

    private UserResponse testUser;
    private UserResponse testUser2;
    private CounterResponse testCounter;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userService.createUser(new CreateUserRequest("Test Student", "test.student@college.edu", UserRole.STUDENT));
        testUser2 = userService.createUser(new CreateUserRequest("Second Student", "second.student@college.edu", UserRole.STUDENT));
        testCounter = counterService.createCounter(new CreateCounterRequest("Accounts Desk", "ACC", "Fees"));
    }

    @Test
    @DisplayName("10. POST /api/tickets - Issues a new ticket and returns 201 CREATED")
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
    @DisplayName("11. GET /api/tickets/{id} - Retrieves ticket by ID with 200 OK")
    void shouldGetTicketById() throws Exception {
        var ticket = ticketService.createTicket(new CreateTicketRequest(testCounter.getId(), testUser.getId()));

        mockMvc.perform(get("/api/tickets/" + ticket.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.formattedToken").value("ACC-001"))
                .andExpect(jsonPath("$.status").value("WAITING"));
    }

    @Test
    @DisplayName("12. POST /api/tickets/{id}/call - Transitions specific ticket to CALLED")
    void shouldCallTicketById() throws Exception {
        var ticket = ticketService.createTicket(new CreateTicketRequest(testCounter.getId(), testUser.getId()));

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/call")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticket.getId()))
                .andExpect(jsonPath("$.status").value("CALLED"))
                .andExpect(jsonPath("$.calledAt").exists());
    }

    @Test
    @DisplayName("13. POST /api/tickets/{id}/complete - Transitions CALLED ticket to COMPLETED")
    void shouldCompleteTicket() throws Exception {
        var ticket = ticketService.createTicket(new CreateTicketRequest(testCounter.getId(), testUser.getId()));
        ticketService.callTicket(ticket.getId());

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/complete")
                        .param("remarks", "Processed successfully"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.remarks").value("Processed successfully"))
                .andExpect(jsonPath("$.completedAt").exists());
    }

    @Test
    @DisplayName("14. POST /api/tickets/{id}/skip - Transitions WAITING ticket to SKIPPED")
    void shouldSkipTicket() throws Exception {
        var ticket = ticketService.createTicket(new CreateTicketRequest(testCounter.getId(), testUser.getId()));

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/skip")
                        .param("remarks", "Did not respond"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"))
                .andExpect(jsonPath("$.remarks").value("Did not respond"));
    }

    @Test
    @DisplayName("15. POST /api/tickets/{id}/cancel - Transitions WAITING ticket to CANCELLED")
    void shouldCancelTicket() throws Exception {
        var ticket = ticketService.createTicket(new CreateTicketRequest(testCounter.getId(), testUser.getId()));

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("16. POST /api/tickets/{id}/complete - Invalid state transition returns 409 CONFLICT")
    void shouldRejectInvalidStateTransition() throws Exception {
        // Ticket is still in WAITING status, cannot complete directly
        var ticket = ticketService.createTicket(new CreateTicketRequest(testCounter.getId(), testUser.getId()));

        mockMvc.perform(post("/api/tickets/" + ticket.getId() + "/complete"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test
    @DisplayName("17. GET /api/tickets/{id} - Missing ticket returns 404 NOT FOUND")
    void shouldReturn404ForMissingTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/tickets/99999"));
    }
}
