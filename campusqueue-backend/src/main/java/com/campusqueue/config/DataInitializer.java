package com.campusqueue.config;

import com.campusqueue.entity.Counter;
import com.campusqueue.entity.Ticket;
import com.campusqueue.entity.TicketStatus;
import com.campusqueue.entity.User;
import com.campusqueue.entity.UserRole;
import com.campusqueue.repository.CounterRepository;
import com.campusqueue.repository.TicketRepository;
import com.campusqueue.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Automatically seeds initial demo data (users with BCrypt hashed passwords, counters, tickets)
 * on application startup in non-test profiles and ensures valid password hashes.
 */
@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CounterRepository counterRepository;
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           CounterRepository counterRepository,
                           TicketRepository ticketRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.counterRepository = counterRepository;
        this.ticketRepository = ticketRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        String studentHash = passwordEncoder.encode("student123");
        String staffHash = passwordEncoder.encode("staff123");
        String adminHash = passwordEncoder.encode("admin123");

        if (counterRepository.count() > 0) {
            log.info("Database already contains data. Updating password hashes for demo accounts...");
            userRepository.findAll().forEach(user -> {
                if (user.getRole() == UserRole.STAFF) {
                    user.setPasswordHash(staffHash);
                } else if (user.getRole() == UserRole.ADMIN) {
                    user.setPasswordHash(adminHash);
                } else {
                    user.setPasswordHash(studentHash);
                }
                userRepository.save(user);
            });
            log.info("Updated password hashes for existing users.");
            return;
        }

        log.info("Seeding initial development data for CampusQueue with secure demo credentials...");

        // 1. Seed Users with BCrypt Passwords
        User rahul = new User("Rahul Sharma", "rahul.sharma@college.edu", UserRole.STUDENT, studentHash);
        User priya = new User("Priya Patel", "priya.patel@college.edu", UserRole.STUDENT, studentHash);
        User amit = new User("Amit Kumar", "amit.kumar@college.edu", UserRole.STUDENT, studentHash);
        User sneha = new User("Sneha Reddy", "sneha.reddy@college.edu", UserRole.STUDENT, studentHash);
        User rohan = new User("Rohan Gupta", "rohan.gupta@college.edu", UserRole.STUDENT, studentHash);
        User staffAccounts = new User("Dr. Sunita Rao", "sunita.rao@college.edu", UserRole.STAFF, staffHash);
        User adminDesk = new User("System Admin", "admin@college.edu", UserRole.ADMIN, adminHash);

        userRepository.saveAll(List.of(rahul, priya, amit, sneha, rohan, staffAccounts, adminDesk));
        log.info("Seeded 7 initial users with BCrypt-hashed credentials.");

        // 2. Seed Counters
        Counter accounts = new Counter("Accounts Office", "ACC", "Student fee payments, fine clearances, receipts", true);
        Counter placement = new Counter("Placement Cell", "PLC", "Internship approvals, campus drive registrations", true);
        Counter admin = new Counter("Administration Office", "ADM", "Bonafide certificates, ID card re-issues, transcripts", true);
        Counter library = new Counter("Library Help Desk", "LIB", "Book issues, returns, library memberships", true);
        Counter studentServices = new Counter("Student Services", "STU", "General inquiries, hostel & scholarship forms", true);

        counterRepository.saveAll(List.of(accounts, placement, admin, library, studentServices));
        log.info("Seeded 5 service counters.");

        // 3. Seed Tickets
        // Accounts Office: 1 CALLED, 2 WAITING
        Ticket t1 = new Ticket(1, accounts, rahul, TicketStatus.CALLED);
        t1.setCalledAt(LocalDateTime.now().minusMinutes(4));

        Ticket t2 = new Ticket(2, accounts, priya, TicketStatus.WAITING);
        Ticket t3 = new Ticket(3, accounts, amit, TicketStatus.WAITING);

        // Placement Cell: 1 CALLED, 1 WAITING
        Ticket t4 = new Ticket(1, placement, sneha, TicketStatus.CALLED);
        t4.setCalledAt(LocalDateTime.now().minusMinutes(2));

        Ticket t5 = new Ticket(2, placement, rohan, TicketStatus.WAITING);

        ticketRepository.saveAll(List.of(t1, t2, t3, t4, t5));
        log.info("Seeded 5 sample queue tickets with WAITING and CALLED states.");
    }
}
