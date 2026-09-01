package com.campusqueue;

import com.campusqueue.entity.Counter;
import com.campusqueue.entity.Ticket;
import com.campusqueue.entity.TicketStatus;
import com.campusqueue.entity.User;
import com.campusqueue.entity.UserRole;
import com.campusqueue.repository.CounterRepository;
import com.campusqueue.repository.TicketRepository;
import com.campusqueue.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
class CampusQueueApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CounterRepository counterRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("Spring Application Context and JPA Repositories load successfully")
    void contextLoads() {
        assertNotNull(userRepository);
        assertNotNull(counterRepository);
        assertNotNull(ticketRepository);
    }

    @Test
    @DisplayName("JPA Entity mappings and Repository CRUD operations work correctly")
    void testEntityLifecycleAndRelationships() {
        // 1. Create and persist User
        User student = new User("Rahul Sharma", "rahul.sharma@college.edu", UserRole.STUDENT);
        User savedStudent = userRepository.save(student);
        assertNotNull(savedStudent.getId());
        assertEquals("Rahul Sharma", savedStudent.getName());

        // 2. Create and persist Counter
        Counter accountsCounter = new Counter("Accounts Office", "ACC", "Student fee payments and receipts", true);
        Counter savedCounter = counterRepository.save(accountsCounter);
        assertNotNull(savedCounter.getId());
        assertEquals("ACC", savedCounter.getCode());

        // 3. Create and persist Ticket
        Ticket ticket = new Ticket("ACC-001", savedCounter, savedStudent, TicketStatus.WAITING);
        Ticket savedTicket = ticketRepository.save(ticket);
        assertNotNull(savedTicket.getId());
        assertEquals("ACC-001", savedTicket.getTokenNumber());
        assertEquals(TicketStatus.WAITING, savedTicket.getStatus());
        assertNotNull(savedTicket.getCreatedAt());

        // 4. Query relationships
        List<Ticket> studentTickets = ticketRepository.findByUserIdAndStatusIn(
                savedStudent.getId(),
                List.of(TicketStatus.WAITING, TicketStatus.SERVING)
        );
        assertEquals(1, studentTickets.size());
        assertEquals(savedCounter.getId(), studentTickets.get(0).getCounter().getId());

        // 5. Test native query countPeopleAhead
        long peopleAhead = ticketRepository.countPeopleAhead(savedCounter.getId(), savedTicket.getCreatedAt().plusSeconds(10));
        assertEquals(1, peopleAhead);
    }
}
