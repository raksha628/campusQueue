package com.campusqueue;

import com.campusqueue.dto.request.CreateCounterRequest;
import com.campusqueue.dto.request.CreateTicketRequest;
import com.campusqueue.dto.request.CreateUserRequest;
import com.campusqueue.entity.Counter;
import com.campusqueue.entity.Ticket;
import com.campusqueue.entity.TicketStatus;
import com.campusqueue.entity.User;
import com.campusqueue.entity.UserRole;
import com.campusqueue.repository.CounterRepository;
import com.campusqueue.repository.TicketRepository;
import com.campusqueue.repository.UserRepository;
import com.campusqueue.security.CustomUserDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class SecurityAuthorizationTest {

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
    private PasswordEncoder passwordEncoder;

    private User student1;
    private User student2;
    private User staffUser;
    private User adminUser;
    private Counter testCounter;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        userRepository.deleteAll();

        String pass = passwordEncoder.encode("password123");
        student1 = userRepository.save(new User("Student One", "s1@college.edu", UserRole.STUDENT, pass));
        student2 = userRepository.save(new User("Student Two", "s2@college.edu", UserRole.STUDENT, pass));
        staffUser = userRepository.save(new User("Staff Member", "staff@college.edu", UserRole.STAFF, pass));
        adminUser = userRepository.save(new User("Admin Member", "admin@college.edu", UserRole.ADMIN, pass));

        testCounter = counterRepository.save(new Counter("Accounts Office", "ACC", "Fee payments", true));
    }

    @Test
    @DisplayName("1. Unauthenticated request to protected endpoint returns 401 UNAUTHORIZED")
    void testUnauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/counters"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("2. Public health check endpoint works without authentication")
    void testPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("3. STUDENT cannot call staff operations (403 FORBIDDEN)")
    @WithMockUser(username = "s1@college.edu", roles = {"STUDENT"})
    void testStudentCannotCallNext() throws Exception {
        mockMvc.perform(post("/api/tickets/counter/" + testCounter.getId() + "/call-next"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("4. STAFF can call staff operations")
    @WithMockUser(username = "staff@college.edu", roles = {"STAFF"})
    void testStaffCanCallNext() throws Exception {
        Ticket ticket = new Ticket(1, testCounter, student1, TicketStatus.WAITING);
        ticketRepository.save(ticket);

        mockMvc.perform(post("/api/tickets/counter/" + testCounter.getId() + "/call-next"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CALLED"))
                .andExpect(jsonPath("$.tokenNumber").value(1));
    }

    @Test
    @DisplayName("5. STAFF cannot create new service counters (403 FORBIDDEN)")
    @WithMockUser(username = "staff@college.edu", roles = {"STAFF"})
    void testStaffCannotCreateCounter() throws Exception {
        CreateCounterRequest req = new CreateCounterRequest("Admin Cell", "ADM", "Certificates");

        mockMvc.perform(post("/api/counters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("6. STAFF cannot toggle counter active status (403 FORBIDDEN)")
    @WithMockUser(username = "staff@college.edu", roles = {"STAFF"})
    void testStaffCannotToggleCounterStatus() throws Exception {
        mockMvc.perform(patch("/api/counters/" + testCounter.getId() + "/toggle-status"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("7. ADMIN can create new service counters")
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    void testAdminCanCreateCounter() throws Exception {
        CreateCounterRequest req = new CreateCounterRequest("Scholarship Cell", "SCH", "Scholarships");

        mockMvc.perform(post("/api/counters")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("SCH"));
    }

    @Test
    @DisplayName("8. STUDENT cannot access another student's ticket history (Ownership 403)")
    void testStudentCannotAccessOtherStudentTickets() throws Exception {
        CustomUserDetails s1Details = CustomUserDetails.fromEntity(student1);

        mockMvc.perform(get("/api/tickets/user/" + student2.getId())
                        .with(user(s1Details)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: You can only view ticket history for your own account."));
    }

    @Test
    @DisplayName("9. STUDENT can access their own ticket history")
    void testStudentCanAccessOwnTickets() throws Exception {
        CustomUserDetails s1Details = CustomUserDetails.fromEntity(student1);
        Ticket t1 = new Ticket(1, testCounter, student1, TicketStatus.WAITING);
        ticketRepository.save(t1);

        mockMvc.perform(get("/api/tickets/user/" + student1.getId())
                        .with(user(s1Details)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tokenNumber").value(1));
    }

    @Test
    @DisplayName("10. STUDENT cannot take a ticket on behalf of another user (Ownership 403)")
    void testStudentCannotTakeTicketForOtherUser() throws Exception {
        CustomUserDetails s1Details = CustomUserDetails.fromEntity(student1);
        CreateTicketRequest req = new CreateTicketRequest(testCounter.getId(), student2.getId());

        mockMvc.perform(post("/api/tickets")
                        .with(user(s1Details))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: You can only create a ticket for your own account."));
    }

    @Test
    @DisplayName("11. STUDENT cannot cancel another student's waiting ticket (Ownership 403)")
    void testStudentCannotCancelOtherStudentTicket() throws Exception {
        CustomUserDetails s1Details = CustomUserDetails.fromEntity(student1);
        Ticket s2Ticket = ticketRepository.save(new Ticket(1, testCounter, student2, TicketStatus.WAITING));

        mockMvc.perform(post("/api/tickets/" + s2Ticket.getId() + "/cancel")
                        .with(user(s1Details)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: You can only cancel ticket for your own account."));
    }

    @Test
    @DisplayName("12. STUDENT cannot access analytics overview (403 FORBIDDEN)")
    @WithMockUser(username = "s1@college.edu", roles = {"STUDENT"})
    void testStudentCannotAccessAnalytics() throws Exception {
        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("13. STAFF can access analytics overview (200 OK)")
    @WithMockUser(username = "staff@college.edu", roles = {"STAFF"})
    void testStaffCanAccessAnalytics() throws Exception {
        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTicketsOverall").exists());
    }

    @Test
    @DisplayName("14. STUDENT cannot complete or skip a ticket (403 FORBIDDEN)")
    @WithMockUser(username = "s1@college.edu", roles = {"STUDENT"})
    void testStudentCannotCompleteOrSkipTicket() throws Exception {
        Ticket t1 = ticketRepository.save(new Ticket(1, testCounter, student1, TicketStatus.CALLED));

        mockMvc.perform(post("/api/tickets/" + t1.getId() + "/complete"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(post("/api/tickets/" + t1.getId() + "/skip"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("15. STAFF can complete and skip a ticket (200 OK)")
    @WithMockUser(username = "staff@college.edu", roles = {"STAFF"})
    void testStaffCanCompleteAndSkipTicket() throws Exception {
        Ticket t1 = new Ticket(1, testCounter, student1, TicketStatus.CALLED);
        t1.setCalledAt(LocalDateTime.now().minusMinutes(2));
        ticketRepository.save(t1);

        mockMvc.perform(post("/api/tickets/" + t1.getId() + "/complete")
                        .param("remarks", "Fee cleared"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.remarks").value("Fee cleared"));

        Ticket t2 = ticketRepository.save(new Ticket(2, testCounter, student2, TicketStatus.WAITING));
        mockMvc.perform(post("/api/tickets/" + t2.getId() + "/skip")
                        .param("remarks", "No show"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"))
                .andExpect(jsonPath("$.remarks").value("No show"));
    }

    @Test
    @DisplayName("16. STUDENT cannot list all users (403 FORBIDDEN)")
    @WithMockUser(username = "s1@college.edu", roles = {"STUDENT"})
    void testStudentCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("17. STUDENT cannot create users (403 FORBIDDEN)")
    @WithMockUser(username = "s1@college.edu", roles = {"STUDENT"})
    void testStudentCannotCreateUser() throws Exception {
        CreateUserRequest req = new CreateUserRequest("New User", "new@college.edu", UserRole.STUDENT, "pass123");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("18. ADMIN can create users (201 CREATED)")
    @WithMockUser(username = "admin@college.edu", roles = {"ADMIN"})
    void testAdminCanCreateUser() throws Exception {
        CreateUserRequest req = new CreateUserRequest("New Student", "new.student@college.edu", UserRole.STUDENT, "student123");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.student@college.edu"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("19. STUDENT cannot view another student's profile (Ownership 403)")
    void testStudentCannotViewOtherUserProfile() throws Exception {
        CustomUserDetails s1Details = CustomUserDetails.fromEntity(student1);

        mockMvc.perform(get("/api/users/" + student2.getId())
                        .with(user(s1Details)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied: You can only view profile for your own account."));
    }

    @Test
    @DisplayName("20. STUDENT can view their own profile (200 OK)")
    void testStudentCanViewOwnProfile() throws Exception {
        CustomUserDetails s1Details = CustomUserDetails.fromEntity(student1);

        mockMvc.perform(get("/api/users/" + student1.getId())
                        .with(user(s1Details)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("s1@college.edu"));
    }
}
