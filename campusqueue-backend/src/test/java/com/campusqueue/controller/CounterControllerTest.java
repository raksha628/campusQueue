package com.campusqueue.controller;

import com.campusqueue.dto.request.CreateCounterRequest;
import com.campusqueue.repository.CounterRepository;
import com.campusqueue.repository.TicketRepository;
import com.campusqueue.repository.UserRepository;
import com.campusqueue.service.CounterService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
class CounterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private CounterService counterService;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("6. POST /api/counters - Creates a new counter with 201 CREATED")
    void testCreateCounterSuccess() throws Exception {
        CreateCounterRequest request = new CreateCounterRequest("Library Desk", "LIB", "Book issues");

        mockMvc.perform(post("/api/counters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Library Desk"))
                .andExpect(jsonPath("$.code").value("LIB"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @DisplayName("7. GET /api/counters - Lists all counters with 200 OK")
    void testGetAllCounters() throws Exception {
        counterService.createCounter(new CreateCounterRequest("Accounts Desk", "ACC", "Fees"));
        counterService.createCounter(new CreateCounterRequest("Placement Cell", "PLC", "Jobs"));

        mockMvc.perform(get("/api/counters")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("8. GET /api/counters/{id} - Retrieves counter by ID with 200 OK")
    void testGetCounterById() throws Exception {
        var counter = counterService.createCounter(new CreateCounterRequest("Admin Desk", "ADM", "Certificates"));

        mockMvc.perform(get("/api/counters/" + counter.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(counter.getId()))
                .andExpect(jsonPath("$.name").value("Admin Desk"))
                .andExpect(jsonPath("$.code").value("ADM"));
    }

    @Test
    @DisplayName("9. GET /api/counters/{id} - Missing counter returns 404 NOT FOUND")
    void testGetMissingCounter() throws Exception {
        mockMvc.perform(get("/api/counters/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/counters/99999"));
    }

    @Test
    @DisplayName("POST /api/counters - Duplicate code returns 409 CONFLICT")
    void testDuplicateCounterCode() throws Exception {
        CreateCounterRequest req1 = new CreateCounterRequest("Desk One", "DSK", "First");
        counterService.createCounter(req1);

        CreateCounterRequest req2 = new CreateCounterRequest("Desk Two", "DSK", "Second");
        mockMvc.perform(post("/api/counters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"));
    }
}
