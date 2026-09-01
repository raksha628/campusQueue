package com.campusqueue.controller;

import com.campusqueue.entity.Counter;
import com.campusqueue.entity.Ticket;
import com.campusqueue.entity.TicketStatus;
import com.campusqueue.entity.User;
import com.campusqueue.entity.UserRole;
import com.campusqueue.repository.CounterRepository;
import com.campusqueue.repository.TicketRepository;
import com.campusqueue.repository.UserRepository;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private TicketRepository ticketRepository;

    private Counter testCounter;
    private User testStudent;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        userRepository.deleteAll();

        testStudent = userRepository.save(new User("Test Student", "analytics.student@college.edu", UserRole.STUDENT));
        testCounter = counterRepository.save(new Counter("Accounts Desk", "ACC", "Fees Desk", true));
    }

    @Test
    @DisplayName("GET /api/analytics/counters/{id}/queue - Returns waiting queue in token order")
    void testGetWaitingQueue() throws Exception {
        Ticket t1 = new Ticket(1, testCounter, testStudent, TicketStatus.WAITING);
        Ticket t2 = new Ticket(2, testCounter, testStudent, TicketStatus.WAITING);
        ticketRepository.saveAll(List.of(t1, t2));

        mockMvc.perform(get("/api/analytics/counters/" + testCounter.getId() + "/queue")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tokenNumber").value(1))
                .andExpect(jsonPath("$[1].tokenNumber").value(2));
    }

    @Test
    @DisplayName("GET /api/analytics/counters/{id}/stats - Returns stats for counter")
    void testGetCounterStats() throws Exception {
        Ticket t1 = new Ticket(1, testCounter, testStudent, TicketStatus.COMPLETED);
        t1.setCalledAt(LocalDateTime.now().minusMinutes(10));
        t1.setCompletedAt(LocalDateTime.now().minusMinutes(5));
        ticketRepository.save(t1);

        mockMvc.perform(get("/api/analytics/counters/" + testCounter.getId() + "/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterId").value(testCounter.getId()))
                .andExpect(jsonPath("$.completedTickets").value(1))
                .andExpect(jsonPath("$.averageWaitMinutes").exists())
                .andExpect(jsonPath("$.averageServiceMinutes").exists());
    }

    @Test
    @DisplayName("GET /api/analytics/overview - Returns high level overview")
    void testGetOverview() throws Exception {
        mockMvc.perform(get("/api/analytics/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTicketsOverall").exists())
                .andExpect(jsonPath("$.counterPerformance").isArray());
    }

    @Test
    @DisplayName("GET /api/analytics/busiest-counter - Returns busiest counter")
    void testGetBusiestCounter() throws Exception {
        Ticket t1 = new Ticket(1, testCounter, testStudent, TicketStatus.COMPLETED);
        t1.setCalledAt(LocalDateTime.now().minusMinutes(10));
        t1.setCompletedAt(LocalDateTime.now().minusMinutes(5));
        ticketRepository.save(t1);

        mockMvc.perform(get("/api/analytics/busiest-counter")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.counterId").value(testCounter.getId()))
                .andExpect(jsonPath("$.handledTickets").value(1));
    }

    @Test
    @DisplayName("GET /api/analytics/daily-volume - Returns daily volume list")
    void testGetDailyVolume() throws Exception {
        Ticket t1 = new Ticket(1, testCounter, testStudent, TicketStatus.WAITING);
        ticketRepository.save(t1);

        mockMvc.perform(get("/api/analytics/daily-volume")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketCount").value(1));
    }

    @Test
    @DisplayName("GET /api/analytics/peak-hour - Returns peak hour")
    void testGetPeakHour() throws Exception {
        Ticket t1 = new Ticket(1, testCounter, testStudent, TicketStatus.WAITING);
        ticketRepository.save(t1);

        mockMvc.perform(get("/api/analytics/peak-hour")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hour").exists())
                .andExpect(jsonPath("$.ticketCount").value(1));
    }

    @Test
    @DisplayName("GET /api/analytics/performance - Returns per counter performance")
    void testGetPerformance() throws Exception {
        mockMvc.perform(get("/api/analytics/performance")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].counterName").value("Accounts Desk"));
    }
}
