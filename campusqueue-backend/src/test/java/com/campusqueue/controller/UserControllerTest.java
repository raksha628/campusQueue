package com.campusqueue.controller;

import com.campusqueue.dto.request.CreateUserRequest;
import com.campusqueue.entity.UserRole;
import com.campusqueue.repository.CounterRepository;
import com.campusqueue.repository.TicketRepository;
import com.campusqueue.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.security.test.context.support.WithMockUser;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. POST /api/users - Creates a new user and returns 201 CREATED")
    void testCreateUserSuccess() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Pooja Hegde", "pooja@college.edu", UserRole.STUDENT);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Pooja Hegde"))
                .andExpect(jsonPath("$.email").value("pooja@college.edu"))
                .andExpect(jsonPath("$.role").value("STUDENT"));
    }

    @Test
    @DisplayName("2. GET /api/users/{id} - Retrieves user by ID with 200 OK")
    void testGetUserSuccess() throws Exception {
        var user = userService.createUser(new CreateUserRequest("Vikas", "vikas@college.edu", UserRole.STUDENT));

        mockMvc.perform(get("/api/users/" + user.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.name").value("Vikas"))
                .andExpect(jsonPath("$.email").value("vikas@college.edu"));
    }

    @Test
    @DisplayName("3. POST /api/users - Invalid request body returns 400 BAD REQUEST with details")
    void testCreateUserInvalid() throws Exception {
        CreateUserRequest invalidRequest = new CreateUserRequest("", "not-an-email", null);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    @DisplayName("4. POST /api/users - Duplicate email returns 409 CONFLICT")
    void testCreateUserDuplicateEmail() throws Exception {
        CreateUserRequest request = new CreateUserRequest("Suresh", "suresh@college.edu", UserRole.STAFF);
        userService.createUser(request);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("A user with email 'suresh@college.edu' already exists"));
    }

    @Test
    @DisplayName("5. GET /api/users/{id} - Non-existent ID returns 404 NOT FOUND")
    void testGetMissingUser() throws Exception {
        mockMvc.perform(get("/api/users/99999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/users/99999"));
    }
}
