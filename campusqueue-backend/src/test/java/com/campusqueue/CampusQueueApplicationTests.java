package com.campusqueue;

import com.campusqueue.dto.request.CreateCounterRequest;
import com.campusqueue.dto.request.CreateTicketRequest;
import com.campusqueue.dto.request.CreateUserRequest;
import com.campusqueue.dto.response.CounterResponse;
import com.campusqueue.dto.response.QueueStatusResponse;
import com.campusqueue.dto.response.TicketResponse;
import com.campusqueue.dto.response.UserResponse;
import com.campusqueue.entity.Counter;
import com.campusqueue.entity.Ticket;
import com.campusqueue.entity.TicketStatus;
import com.campusqueue.entity.User;
import com.campusqueue.entity.UserRole;
import com.campusqueue.exception.BadRequestException;
import com.campusqueue.repository.CounterRepository;
import com.campusqueue.repository.TicketRepository;
import com.campusqueue.repository.UserRepository;
import com.campusqueue.service.CounterService;
import com.campusqueue.service.TicketService;
import com.campusqueue.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
class CampusQueueApplicationTests {

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

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        counterRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1 & 2. Token Generation - First ticket receives #1, second receives #2 in sequence")
    void testTokenGenerationSequence() {
        User student1 = userRepository.save(new User("Student One", "s1@college.edu", UserRole.STUDENT));
        User student2 = userRepository.save(new User("Student Two", "s2@college.edu", UserRole.STUDENT));
        Counter counter = counterRepository.save(new Counter("Accounts Office", "ACC", "Fee desks", true));

        // 1. First ticket receives token #1
        TicketResponse ticket1 = ticketService.createTicket(new CreateTicketRequest(counter.getId(), student1.getId()));
        assertEquals(1, ticket1.getTokenNumber());
        assertEquals("ACC-001", ticket1.getFormattedToken());
        assertEquals(TicketStatus.WAITING, ticket1.getStatus());

        // 2. Second ticket receives token #2
        TicketResponse ticket2 = ticketService.createTicket(new CreateTicketRequest(counter.getId(), student2.getId()));
        assertEquals(2, ticket2.getTokenNumber());
        assertEquals("ACC-002", ticket2.getFormattedToken());
        assertEquals(TicketStatus.WAITING, ticket2.getStatus());
    }

    @Test
    @DisplayName("3. Queue Position - Position is calculated correctly based on waiting tickets ahead")
    void testQueuePositionCalculation() {
        User student1 = userRepository.save(new User("Student 1", "st1@college.edu", UserRole.STUDENT));
        User student2 = userRepository.save(new User("Student 2", "st2@college.edu", UserRole.STUDENT));
        User student3 = userRepository.save(new User("Student 3", "st3@college.edu", UserRole.STUDENT));
        Counter counter = counterRepository.save(new Counter("Placement Cell", "PLC", "Placements", true));

        TicketResponse t1 = ticketService.createTicket(new CreateTicketRequest(counter.getId(), student1.getId()));
        assertEquals(0, t1.getPeopleAhead()); // First student has 0 ahead

        TicketResponse t2 = ticketService.createTicket(new CreateTicketRequest(counter.getId(), student2.getId()));
        assertEquals(1, t2.getPeopleAhead()); // Second student has 1 ahead (t1)

        TicketResponse t3 = ticketService.createTicket(new CreateTicketRequest(counter.getId(), student3.getId()));
        assertEquals(2, t3.getPeopleAhead()); // Third student has 2 ahead (t1, t2)

        // When t1 is called, t2 now has 0 ahead and t3 has 1 ahead
        ticketService.callNextTicket(counter.getId());
        TicketResponse t2Updated = ticketService.getTicketById(t2.getId());
        TicketResponse t3Updated = ticketService.getTicketById(t3.getId());
        assertEquals(0, t2Updated.getPeopleAhead());
        assertEquals(1, t3Updated.getPeopleAhead());
    }

    @Test
    @DisplayName("4. State Machine - Invalid state transitions are rejected with BadRequestException")
    void testInvalidStateTransitions() {
        User user = userRepository.save(new User("Test Student", "invalid.trans@college.edu", UserRole.STUDENT));
        Counter counter = counterRepository.save(new Counter("Library", "LIB", "Books", true));

        TicketResponse ticket = ticketService.createTicket(new CreateTicketRequest(counter.getId(), user.getId()));

        // Cannot complete a WAITING ticket directly (must be CALLED first)
        assertThrows(BadRequestException.class, () -> ticketService.completeTicket(ticket.getId(), "Done"));

        // Call the ticket: WAITING -> CALLED
        TicketResponse calledTicket = ticketService.callNextTicket(counter.getId());
        assertEquals(TicketStatus.CALLED, calledTicket.getStatus());

        // Complete the ticket: CALLED -> COMPLETED
        TicketResponse completedTicket = ticketService.completeTicket(calledTicket.getId(), "All done");
        assertEquals(TicketStatus.COMPLETED, completedTicket.getStatus());

        // Cannot complete again
        assertThrows(BadRequestException.class, () -> ticketService.completeTicket(completedTicket.getId(), "Again"));

        // Cannot skip a COMPLETED ticket
        assertThrows(BadRequestException.class, () -> ticketService.skipTicket(completedTicket.getId(), "Skip"));

        // Cannot cancel a COMPLETED ticket
        assertThrows(BadRequestException.class, () -> ticketService.cancelTicket(completedTicket.getId()));
    }

    @Test
    @DisplayName("5. Complete Ticket - Transitions CALLED -> COMPLETED and records completedAt")
    void testCompleteTicket() {
        User user = userRepository.save(new User("Rajesh", "rajesh@college.edu", UserRole.STUDENT));
        Counter counter = counterRepository.save(new Counter("Accounts", "ACC", "Fees", true));

        ticketService.createTicket(new CreateTicketRequest(counter.getId(), user.getId()));
        TicketResponse called = ticketService.callNextTicket(counter.getId());

        TicketResponse completed = ticketService.completeTicket(called.getId(), "Fee receipt handed over");
        assertEquals(TicketStatus.COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt());
        assertEquals("Fee receipt handed over", completed.getRemarks());
    }

    @Test
    @DisplayName("6. Skip Ticket - Transitions WAITING/CALLED -> SKIPPED and records completedAt")
    void testSkipTicket() {
        User user = userRepository.save(new User("Ananya", "ananya@college.edu", UserRole.STUDENT));
        Counter counter = counterRepository.save(new Counter("Placement", "PLC", "Jobs", true));

        TicketResponse ticket = ticketService.createTicket(new CreateTicketRequest(counter.getId(), user.getId()));
        TicketResponse skipped = ticketService.skipTicket(ticket.getId(), "Student did not show up");

        assertEquals(TicketStatus.SKIPPED, skipped.getStatus());
        assertNotNull(skipped.getCompletedAt());
        assertEquals("Student did not show up", skipped.getRemarks());
    }

    @Test
    @DisplayName("7. Call Next Ticket - Selects the earliest WAITING ticket in strict FIFO order")
    void testCallNextTicketFIFO() {
        User u1 = userRepository.save(new User("First In Line", "first@college.edu", UserRole.STUDENT));
        User u2 = userRepository.save(new User("Second In Line", "second@college.edu", UserRole.STUDENT));
        Counter counter = counterRepository.save(new Counter("Admin", "ADM", "Certificates", true));

        ticketService.createTicket(new CreateTicketRequest(counter.getId(), u1.getId()));
        ticketService.createTicket(new CreateTicketRequest(counter.getId(), u2.getId()));

        TicketResponse calledFirst = ticketService.callNextTicket(counter.getId());
        assertEquals(1, calledFirst.getTokenNumber());
        assertEquals(u1.getId(), calledFirst.getUserId());
        assertEquals(TicketStatus.CALLED, calledFirst.getStatus());

        TicketResponse calledSecond = ticketService.callNextTicket(counter.getId());
        assertEquals(2, calledSecond.getTokenNumber());
        assertEquals(u2.getId(), calledSecond.getUserId());
        assertEquals(TicketStatus.CALLED, calledSecond.getStatus());
    }

    @Test
    @DisplayName("8. Call Next when Queue Empty - Rejects with clear BadRequestException")
    void testCallNextWhenQueueEmpty() {
        Counter counter = counterRepository.save(new Counter("Empty Counter", "EMP", "Empty desk", true));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> ticketService.callNextTicket(counter.getId()));
        assertTrue(ex.getMessage().contains("No students currently waiting"));
    }

    @Test
    @DisplayName("9. Token Scope - Token generation is independent for separate counters")
    void testIndependentCounterTokenGeneration() {
        User u1 = userRepository.save(new User("User 1", "u1@college.edu", UserRole.STUDENT));
        User u2 = userRepository.save(new User("User 2", "u2@college.edu", UserRole.STUDENT));
        Counter accounts = counterRepository.save(new Counter("Accounts", "ACC", "Fees", true));
        Counter placement = counterRepository.save(new Counter("Placement", "PLC", "Placements", true));

        // Accounts Office tokens
        TicketResponse acc1 = ticketService.createTicket(new CreateTicketRequest(accounts.getId(), u1.getId()));
        assertEquals(1, acc1.getTokenNumber());
        assertEquals("ACC-001", acc1.getFormattedToken());

        // Placement Cell tokens start at #1 independently
        TicketResponse plc1 = ticketService.createTicket(new CreateTicketRequest(placement.getId(), u1.getId()));
        assertEquals(1, plc1.getTokenNumber());
        assertEquals("PLC-001", plc1.getFormattedToken());

        // Subsequent tokens increment per counter
        TicketResponse acc2 = ticketService.createTicket(new CreateTicketRequest(accounts.getId(), u2.getId()));
        assertEquals(2, acc2.getTokenNumber());
        assertEquals("ACC-002", acc2.getFormattedToken());

        TicketResponse plc2 = ticketService.createTicket(new CreateTicketRequest(placement.getId(), u2.getId()));
        assertEquals(2, plc2.getTokenNumber());
        assertEquals("PLC-002", plc2.getFormattedToken());
    }

    @Test
    @DisplayName("10. Concurrent Token Generation - Protected against duplicate tokens under concurrency")
    void testConcurrentTokenGeneration() throws Exception {
        Counter counter = counterRepository.save(new Counter("High Traffic Desk", "HTD", "Busy Desk", true));
        int threadCount = 10;
        List<User> students = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            students.add(userRepository.save(new User("Student " + i, "concurrent" + i + "@college.edu", UserRole.STUDENT)));
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<TicketResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final Long userId = students.get(i).getId();
            futures.add(CompletableFuture.supplyAsync(() ->
                    ticketService.createTicket(new CreateTicketRequest(counter.getId(), userId)), executor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        List<Integer> generatedTokens = new ArrayList<>();
        for (CompletableFuture<TicketResponse> future : futures) {
            generatedTokens.add(future.get().getTokenNumber());
        }

        // Verify all 10 tokens were generated
        assertEquals(10, generatedTokens.size());

        // Verify all tokens are unique (no duplicates)
        Set<Integer> uniqueTokens = new HashSet<>(generatedTokens);
        assertEquals(10, uniqueTokens.size(), "All generated token numbers must be strictly unique");

        // Verify tokens span sequentially from 1 to 10
        Collections.sort(generatedTokens);
        for (int i = 0; i < threadCount; i++) {
            assertEquals(i + 1, generatedTokens.get(i));
        }
    }

    @Test
    @DisplayName("11. Queue Status - Returns complete live queue metrics")
    void testGetQueueStatus() {
        User u1 = userRepository.save(new User("Student One", "status1@college.edu", UserRole.STUDENT));
        User u2 = userRepository.save(new User("Student Two", "status2@college.edu", UserRole.STUDENT));
        Counter counter = counterRepository.save(new Counter("Accounts Office", "ACC", "Fees", true));

        ticketService.createTicket(new CreateTicketRequest(counter.getId(), u1.getId()));
        ticketService.createTicket(new CreateTicketRequest(counter.getId(), u2.getId()));
        ticketService.callNextTicket(counter.getId()); // u1 is now CALLED

        QueueStatusResponse status = ticketService.getQueueStatus(counter.getId());
        assertEquals("ACC-001", status.getCurrentlyServingToken());
        assertEquals(1, status.getTotalWaiting()); // u2 is WAITING
        assertEquals(1, status.getWaitingTickets().size());
        assertEquals("ACC-002", status.getWaitingTickets().get(0).getFormattedToken());
        assertTrue(status.getEstimatedWaitMinutes() > 0);
    }
}
