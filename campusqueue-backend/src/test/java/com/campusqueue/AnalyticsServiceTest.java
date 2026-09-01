package com.campusqueue;

import com.campusqueue.dto.response.AnalyticsOverviewResponse;
import com.campusqueue.dto.response.BusiestCounterResponse;
import com.campusqueue.dto.response.CounterPerformanceDto;
import com.campusqueue.dto.response.CounterStatsResponse;
import com.campusqueue.dto.response.DailyVolumeResponse;
import com.campusqueue.dto.response.PeakHourResponse;
import com.campusqueue.dto.response.TicketResponse;
import com.campusqueue.entity.Counter;
import com.campusqueue.entity.Ticket;
import com.campusqueue.entity.TicketStatus;
import com.campusqueue.entity.User;
import com.campusqueue.entity.UserRole;
import com.campusqueue.repository.CounterRepository;
import com.campusqueue.repository.TicketRepository;
import com.campusqueue.repository.UserRepository;
import com.campusqueue.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class AnalyticsServiceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private AnalyticsService analyticsService;

    private User student1;
    private User student2;
    private User student3;
    private Counter accountsCounter;
    private Counter placementCounter;
    private Counter emptyCounter;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        userRepository.deleteAll();

        student1 = userRepository.save(new User("Alice", "alice@college.edu", UserRole.STUDENT));
        student2 = userRepository.save(new User("Bob", "bob@college.edu", UserRole.STUDENT));
        student3 = userRepository.save(new User("Charlie", "charlie@college.edu", UserRole.STUDENT));

        accountsCounter = counterRepository.save(new Counter("Accounts Office", "ACC", "Fee payments", true));
        placementCounter = counterRepository.save(new Counter("Placement Cell", "PLC", "Internships", true));
        emptyCounter = counterRepository.save(new Counter("Empty Help Desk", "EMP", "No tickets", true));
    }

    @Test
    @DisplayName("1. Current Queue - Returns only WAITING tickets ordered strictly by token_number")
    void testCurrentQueueOrderedByToken() {
        Ticket t1 = new Ticket(1, accountsCounter, student1, TicketStatus.CALLED);
        t1.setCalledAt(LocalDateTime.now().minusMinutes(10));
        ticketRepository.save(t1);

        Ticket t3 = new Ticket(3, accountsCounter, student3, TicketStatus.WAITING);
        ticketRepository.save(t3);

        Ticket t2 = new Ticket(2, accountsCounter, student2, TicketStatus.WAITING);
        ticketRepository.save(t2);

        List<TicketResponse> queue = analyticsService.getWaitingQueueOrderedByToken(accountsCounter.getId());
        assertEquals(2, queue.size());
        assertEquals(2, queue.get(0).getTokenNumber());
        assertEquals("ACC-002", queue.get(0).getFormattedToken());
        assertEquals(3, queue.get(1).getTokenNumber());
        assertEquals("ACC-003", queue.get(1).getFormattedToken());
    }

    @Test
    @DisplayName("2. Average Waiting Time - SQL computes (called_at - created_at) accurately in minutes")
    void testAverageWaitingTimeCalculation() {
        LocalDateTime baseTime = LocalDateTime.now().minusHours(1);

        // Ticket 1: waited 10 minutes (created: base, called: base + 10m, completed: base + 15m)
        Ticket t1 = new Ticket(1, accountsCounter, student1, TicketStatus.COMPLETED);
        t1.setCreatedAt(baseTime);
        t1.setCalledAt(baseTime.plusMinutes(10));
        t1.setCompletedAt(baseTime.plusMinutes(15));
        ticketRepository.save(t1);

        // Ticket 2: waited 20 minutes (created: base, called: base + 20m, completed: base + 28m)
        Ticket t2 = new Ticket(2, accountsCounter, student2, TicketStatus.COMPLETED);
        t2.setCreatedAt(baseTime);
        t2.setCalledAt(baseTime.plusMinutes(20));
        t2.setCompletedAt(baseTime.plusMinutes(28));
        ticketRepository.save(t2);

        // Expected average wait = (10 + 20) / 2 = 15.0 minutes
        Double avgWait = ticketRepository.calculateAverageWaitingTimeMinutes(accountsCounter.getId());
        assertNotNull(avgWait);
        assertEquals(15.0, avgWait, 0.1);
    }

    @Test
    @DisplayName("3. Average Service Time - SQL computes (completed_at - called_at) accurately in minutes")
    void testAverageServiceTimeCalculation() {
        LocalDateTime baseTime = LocalDateTime.now().minusHours(1);

        // Ticket 1: service duration = 5 minutes (called: base + 10m, completed: base + 15m)
        Ticket t1 = new Ticket(1, accountsCounter, student1, TicketStatus.COMPLETED);
        t1.setCreatedAt(baseTime);
        t1.setCalledAt(baseTime.plusMinutes(10));
        t1.setCompletedAt(baseTime.plusMinutes(15));
        ticketRepository.save(t1);

        // Ticket 2: service duration = 9 minutes (called: base + 20m, completed: base + 29m)
        Ticket t2 = new Ticket(2, accountsCounter, student2, TicketStatus.COMPLETED);
        t2.setCreatedAt(baseTime);
        t2.setCalledAt(baseTime.plusMinutes(20));
        t2.setCompletedAt(baseTime.plusMinutes(29));
        ticketRepository.save(t2);

        // Expected average service = (5 + 9) / 2 = 7.0 minutes
        Double avgService = ticketRepository.calculateAverageHandlingTimeMinutes(accountsCounter.getId());
        assertNotNull(avgService);
        assertEquals(7.0, avgService, 0.1);
    }

    @Test
    @DisplayName("4. Busiest Counter - Identifies counter with most COMPLETED tickets")
    void testBusiestCounter() {
        LocalDateTime now = LocalDateTime.now();

        // Accounts has 1 completed ticket
        Ticket acc1 = new Ticket(1, accountsCounter, student1, TicketStatus.COMPLETED);
        acc1.setCreatedAt(now.minusMinutes(20));
        acc1.setCalledAt(now.minusMinutes(10));
        acc1.setCompletedAt(now.minusMinutes(5));
        ticketRepository.save(acc1);

        // Placement has 2 completed tickets
        Ticket plc1 = new Ticket(1, placementCounter, student2, TicketStatus.COMPLETED);
        plc1.setCreatedAt(now.minusMinutes(15));
        plc1.setCalledAt(now.minusMinutes(8));
        plc1.setCompletedAt(now.minusMinutes(4));

        Ticket plc2 = new Ticket(2, placementCounter, student3, TicketStatus.COMPLETED);
        plc2.setCreatedAt(now.minusMinutes(10));
        plc2.setCalledAt(now.minusMinutes(4));
        plc2.setCompletedAt(now.minusMinutes(1));
        ticketRepository.saveAll(List.of(plc1, plc2));

        BusiestCounterResponse busiest = analyticsService.getBusiestCounter();
        assertEquals(placementCounter.getId(), busiest.getCounterId());
        assertEquals("Placement Cell", busiest.getCounterName());
        assertEquals(2, busiest.getHandledTickets());
    }

    @Test
    @DisplayName("5. Daily Ticket Volume - Groups volume by calendar date")
    void testDailyTicketVolume() {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime yesterday = today.minusDays(1);

        Ticket t1 = new Ticket(1, accountsCounter, student1, TicketStatus.WAITING);
        t1.setCreatedAt(yesterday);
        Ticket t2 = new Ticket(2, accountsCounter, student2, TicketStatus.WAITING);
        t2.setCreatedAt(yesterday);
        Ticket t3 = new Ticket(1, placementCounter, student3, TicketStatus.WAITING);
        t3.setCreatedAt(today);

        ticketRepository.saveAll(List.of(t1, t2, t3));

        List<DailyVolumeResponse> volumeList = analyticsService.getDailyTicketVolume();
        assertEquals(2, volumeList.size());
        assertEquals(yesterday.toLocalDate(), volumeList.get(0).getDate());
        assertEquals(2, volumeList.get(0).getTicketCount());
        assertEquals(today.toLocalDate(), volumeList.get(1).getDate());
        assertEquals(1, volumeList.get(1).getTicketCount());
    }

    @Test
    @DisplayName("6. Peak Queue Hour - Accurately finds the hour of day with highest ticket creation")
    void testPeakQueueHour() {
        LocalDateTime hour10 = LocalDateTime.of(2026, 9, 1, 10, 15);
        LocalDateTime hour14 = LocalDateTime.of(2026, 9, 1, 14, 30);

        Ticket t1 = new Ticket(1, accountsCounter, student1, TicketStatus.WAITING);
        t1.setCreatedAt(hour10);
        Ticket t2 = new Ticket(2, accountsCounter, student2, TicketStatus.WAITING);
        t2.setCreatedAt(hour14);
        Ticket t3 = new Ticket(1, placementCounter, student3, TicketStatus.WAITING);
        t3.setCreatedAt(hour14);

        ticketRepository.saveAll(List.of(t1, t2, t3));

        PeakHourResponse peak = analyticsService.getPeakQueueHour();
        assertEquals(14, peak.getHour());
        assertEquals("14:00 - 15:00", peak.getFormattedHour());
        assertEquals(2, peak.getTicketCount());
    }

    @Test
    @DisplayName("7. Counter Performance Aggregation - Computes status breakdown in single SQL query")
    void testCounterPerformanceAggregation() {
        LocalDateTime now = LocalDateTime.now();

        // Accounts: 1 completed, 1 skipped, 1 cancelled
        // acc1: created 20m ago, called 10m ago (wait = 10m), completed 5m ago (service = 5m)
        Ticket acc1 = new Ticket(1, accountsCounter, student1, TicketStatus.COMPLETED);
        acc1.setCreatedAt(now.minusMinutes(20));
        acc1.setCalledAt(now.minusMinutes(10));
        acc1.setCompletedAt(now.minusMinutes(5));

        Ticket acc2 = new Ticket(2, accountsCounter, student2, TicketStatus.SKIPPED);
        acc2.setCreatedAt(now.minusMinutes(15));
        acc2.setCompletedAt(now.minusMinutes(2));

        Ticket acc3 = new Ticket(3, accountsCounter, student3, TicketStatus.CANCELLED);
        acc3.setCreatedAt(now.minusMinutes(10));
        acc3.setCompletedAt(now.minusMinutes(1));

        ticketRepository.saveAll(List.of(acc1, acc2, acc3));

        List<CounterPerformanceDto> performance = analyticsService.getCounterPerformance(null);
        assertFalse(performance.isEmpty());

        CounterPerformanceDto accPerf = performance.stream()
                .filter(p -> p.getCounterId().equals(accountsCounter.getId()))
                .findFirst()
                .orElseThrow();

        assertEquals(3, accPerf.getTotalTickets());
        assertEquals(1, accPerf.getCompletedTickets());
        assertEquals(1, accPerf.getSkippedTickets());
        assertEquals(1, accPerf.getCancelledTickets());
        assertEquals(10.0, accPerf.getAverageWaitMinutes(), 0.1);
        assertEquals(5.0, accPerf.getAverageServiceMinutes(), 0.1);
    }

    @Test
    @DisplayName("8. Empty Counter Graceful Behavior - Zero tickets produce safe 0.0 metrics without errors")
    void testEmptyCounterGracefulBehavior() {
        CounterStatsResponse stats = analyticsService.getCounterStats(emptyCounter.getId());
        assertEquals(emptyCounter.getId(), stats.getCounterId());
        assertEquals(0.0, stats.getAverageWaitMinutes());
        assertEquals(0.0, stats.getAverageServiceMinutes());
        assertEquals(0, stats.getCompletedTickets());
        assertEquals(0, stats.getWaitingTickets());
    }

    @Test
    @DisplayName("9. High-Level Analytics Overview - Returns full dashboard payload")
    void testAnalyticsOverview() {
        LocalDateTime now = LocalDateTime.now();

        Ticket t1 = new Ticket(1, accountsCounter, student1, TicketStatus.COMPLETED);
        t1.setCreatedAt(now.minusMinutes(20));
        t1.setCalledAt(now.minusMinutes(10));
        t1.setCompletedAt(now.minusMinutes(5));
        ticketRepository.save(t1);

        Ticket t2 = new Ticket(2, accountsCounter, student2, TicketStatus.WAITING);
        ticketRepository.save(t2);

        AnalyticsOverviewResponse overview = analyticsService.getOverview();
        assertEquals(2, overview.getTotalTicketsOverall());
        assertEquals(1, overview.getTotalCompletedOverall());
        assertEquals(1, overview.getTotalWaitingOverall());
        assertNotNull(overview.getBusiestCounter());
        assertNotNull(overview.getPeakHour());
        assertNotNull(overview.getCounterPerformance());
    }
}
