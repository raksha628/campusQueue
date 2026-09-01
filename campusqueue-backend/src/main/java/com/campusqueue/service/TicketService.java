package com.campusqueue.service;

import com.campusqueue.dto.request.CreateTicketRequest;
import com.campusqueue.dto.response.QueueStatusResponse;
import com.campusqueue.dto.response.TicketResponse;
import com.campusqueue.entity.Counter;
import com.campusqueue.entity.Ticket;
import com.campusqueue.entity.TicketStatus;
import com.campusqueue.entity.User;
import com.campusqueue.exception.BadRequestException;
import com.campusqueue.exception.ConflictException;
import com.campusqueue.exception.ResourceNotFoundException;
import com.campusqueue.repository.CounterRepository;
import com.campusqueue.repository.TicketRepository;
import com.campusqueue.repository.UserRepository;
import com.campusqueue.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TicketService {

    private static final double DEFAULT_SERVICE_TIME_MINUTES = 5.0;

    private final TicketRepository ticketRepository;
    private final CounterRepository counterRepository;
    private final UserRepository userRepository;

    public TicketService(TicketRepository ticketRepository,
                         CounterRepository counterRepository,
                         UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.counterRepository = counterRepository;
        this.userRepository = userRepository;
    }

    /**
     * 1. Take a ticket:
     * - Acquires a pessimistic row-level lock on the target Counter to serialize token generation per counter.
     * - Generates the next sequential positive token number (scoped independently per counter: #1, #2, #3...).
     * - Enforces ownership: Students can only generate a token for themselves.
     */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request) {
        // Enforce ownership
        SecurityUtils.enforceUserOwnership(request.getUserId(), "create a ticket");

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + request.getUserId()));

        Counter counter = counterRepository.findByIdForUpdate(request.getCounterId())
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found with id: " + request.getCounterId()));

        if (!Boolean.TRUE.equals(counter.getIsActive())) {
            throw new BadRequestException("Counter '" + counter.getName() + "' is currently inactive");
        }

        // Prevent duplicate active tickets for the same user on the same counter
        boolean hasActiveTicket = ticketRepository.existsByUserIdAndCounterIdAndStatusIn(
                user.getId(),
                counter.getId(),
                List.of(TicketStatus.WAITING, TicketStatus.CALLED)
        );
        if (hasActiveTicket) {
            throw new ConflictException("You already hold an active token for " + counter.getName());
        }

        int maxToken = ticketRepository.findMaxTokenNumberByCounterId(counter.getId());
        int nextTokenNumber = maxToken + 1;

        Ticket ticket = new Ticket(nextTokenNumber, counter, user, TicketStatus.WAITING);
        Ticket savedTicket = ticketRepository.save(ticket);

        return mapToTicketResponse(savedTicket);
    }

    /**
     * 2. Get queue status:
     * - Returns real-time queue metrics for a counter.
     */
    @Transactional(readOnly = true)
    public QueueStatusResponse getQueueStatus(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found with id: " + counterId));

        Optional<Ticket> currentlyCalled = ticketRepository.findFirstByCounterIdAndStatusOrderByCalledAtDesc(
                counterId, TicketStatus.CALLED
        );

        List<Ticket> waitingList = ticketRepository.findByCounterIdAndStatusOrderByTokenNumberAsc(
                counterId, TicketStatus.WAITING
        );

        Double avgTime = ticketRepository.calculateAverageHandlingTimeMinutes(counterId);
        double safeAvg = (avgTime != null && avgTime > 0) ? Math.round(avgTime * 10.0) / 10.0 : DEFAULT_SERVICE_TIME_MINUTES;
        double estimatedWait = Math.round(waitingList.size() * safeAvg * 10.0) / 10.0;

        List<TicketResponse> waitingResponses = waitingList.stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());

        String servingToken = currentlyCalled.map(Ticket::getFormattedToken).orElse(null);
        Long servingTicketId = currentlyCalled.map(Ticket::getId).orElse(null);

        return new QueueStatusResponse(
                counter.getId(),
                counter.getName(),
                counter.getCode(),
                counter.getIsActive(),
                servingToken,
                servingTicketId,
                waitingList.size(),
                safeAvg,
                estimatedWait,
                waitingResponses
        );
    }

    /**
     * 3. Call a specific ticket:
     * - Transitions a specific ticket from WAITING -> CALLED.
     */
    @Transactional
    public TicketResponse callTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (ticket.getStatus() != TicketStatus.WAITING) {
            throw new ConflictException("Cannot call ticket: ticket is in " + ticket.getStatus() + " status (must be WAITING)");
        }

        // Auto-complete any currently CALLED ticket for this counter
        if (ticket.getCounter() != null) {
            ticketRepository.findFirstByCounterIdAndStatusOrderByCalledAtDesc(ticket.getCounter().getId(), TicketStatus.CALLED)
                    .ifPresent(currentCalled -> {
                        currentCalled.setStatus(TicketStatus.COMPLETED);
                        currentCalled.setCompletedAt(LocalDateTime.now());
                        ticketRepository.save(currentCalled);
                    });
        }

        ticket.setStatus(TicketStatus.CALLED);
        ticket.setCalledAt(LocalDateTime.now());
        Ticket savedTicket = ticketRepository.save(ticket);

        return mapToTicketResponse(savedTicket);
    }

    /**
     * 4. Call next student in queue for a counter:
     * - Auto-completes any existing CALLED ticket at this counter.
     * - Uses atomic PostgreSQL 'FOR UPDATE SKIP LOCKED' to pick the earliest WAITING ticket.
     * - Transitions WAITING -> CALLED.
     */
    @Transactional
    public TicketResponse callNextTicket(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found with id: " + counterId));

        // Auto-complete any currently CALLED ticket for this counter
        ticketRepository.findFirstByCounterIdAndStatusOrderByCalledAtDesc(counterId, TicketStatus.CALLED)
                .ifPresent(currentCalled -> {
                    currentCalled.setStatus(TicketStatus.COMPLETED);
                    currentCalled.setCompletedAt(LocalDateTime.now());
                    ticketRepository.save(currentCalled);
                });

        // Concurrency-safe fetch of next WAITING ticket
        Ticket nextTicket = ticketRepository.findNextWaitingTicketForUpdate(counterId)
                .orElseThrow(() -> new BadRequestException("No students currently waiting for " + counter.getName()));

        if (nextTicket.getStatus() != TicketStatus.WAITING) {
            throw new ConflictException("Invalid state transition: Cannot call ticket in status " + nextTicket.getStatus());
        }

        nextTicket.setStatus(TicketStatus.CALLED);
        nextTicket.setCalledAt(LocalDateTime.now());
        Ticket savedTicket = ticketRepository.save(nextTicket);

        return mapToTicketResponse(savedTicket);
    }

    /**
     * 5. Complete ticket:
     * - Enforces state machine transition: CALLED -> COMPLETED.
     */
    @Transactional
    public TicketResponse completeTicket(Long ticketId, String remarks) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (ticket.getStatus() != TicketStatus.CALLED) {
            throw new ConflictException("Cannot complete ticket: ticket is currently in "
                    + ticket.getStatus() + " status (must be CALLED)");
        }

        ticket.setStatus(TicketStatus.COMPLETED);
        ticket.setCompletedAt(LocalDateTime.now());
        if (remarks != null && !remarks.isBlank()) {
            ticket.setRemarks(remarks.trim());
        }
        Ticket savedTicket = ticketRepository.save(ticket);
        return mapToTicketResponse(savedTicket);
    }

    /**
     * 6. Skip ticket:
     * - Enforces state machine transitions: WAITING -> SKIPPED or CALLED -> SKIPPED.
     */
    @Transactional
    public TicketResponse skipTicket(Long ticketId, String remarks) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        if (ticket.getStatus() != TicketStatus.WAITING && ticket.getStatus() != TicketStatus.CALLED) {
            throw new ConflictException("Cannot skip ticket: ticket is already in "
                    + ticket.getStatus() + " status");
        }

        ticket.setStatus(TicketStatus.SKIPPED);
        ticket.setCompletedAt(LocalDateTime.now());
        if (remarks != null && !remarks.isBlank()) {
            ticket.setRemarks(remarks.trim());
        }
        Ticket savedTicket = ticketRepository.save(ticket);
        return mapToTicketResponse(savedTicket);
    }

    /**
     * 7. Cancel ticket:
     * - Enforces state machine transition: WAITING -> CANCELLED.
     * - Enforces ownership: Students can only cancel their own ticket.
     */
    @Transactional
    public TicketResponse cancelTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + ticketId));

        // Enforce ownership
        if (ticket.getUser() != null) {
            SecurityUtils.enforceUserOwnership(ticket.getUser().getId(), "cancel ticket");
        }

        if (ticket.getStatus() != TicketStatus.WAITING) {
            throw new ConflictException("Cannot cancel ticket: ticket is currently in "
                    + ticket.getStatus() + " status (only WAITING tickets can be cancelled)");
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setCompletedAt(LocalDateTime.now());
        Ticket savedTicket = ticketRepository.save(ticket);
        return mapToTicketResponse(savedTicket);
    }

    /**
     * Retrieves ticket by ID.
     * - Enforces ownership: Students can only view their own ticket.
     */
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with id: " + id));

        // Enforce ownership
        if (ticket.getUser() != null) {
            SecurityUtils.enforceUserOwnership(ticket.getUser().getId(), "view ticket details");
        }

        return mapToTicketResponse(ticket);
    }

    /**
     * Finds all tickets issued for a counter.
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByCounter(Long counterId) {
        if (!counterRepository.existsById(counterId)) {
            throw new ResourceNotFoundException("Counter not found with id: " + counterId);
        }
        return ticketRepository.findByCounterIdOrderByCreatedAtAsc(counterId).stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    /**
     * Finds all WAITING tickets for a counter.
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getWaitingTicketsByCounter(Long counterId) {
        if (!counterRepository.existsById(counterId)) {
            throw new ResourceNotFoundException("Counter not found with id: " + counterId);
        }
        return ticketRepository.findByCounterIdAndStatusOrderByTokenNumberAsc(counterId, TicketStatus.WAITING).stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    /**
     * Finds currently CALLED ticket for a counter.
     */
    @Transactional(readOnly = true)
    public Optional<TicketResponse> getCurrentlyCalledTicket(Long counterId) {
        if (!counterRepository.existsById(counterId)) {
            throw new ResourceNotFoundException("Counter not found with id: " + counterId);
        }
        return ticketRepository.findFirstByCounterIdAndStatusOrderByCalledAtDesc(counterId, TicketStatus.CALLED)
                .map(this::mapToTicketResponse);
    }

    /**
     * Finds all tickets for a user.
     * - Enforces ownership: Students can only view their own ticket history.
     */
    @Transactional(readOnly = true)
    public List<TicketResponse> getTicketsByUser(Long userId) {
        // Enforce ownership
        SecurityUtils.enforceUserOwnership(userId, "view ticket history");

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToTicketResponse)
                .collect(Collectors.toList());
    }

    // --- Helper Mapping Methods ---

    private TicketResponse mapToTicketResponse(Ticket ticket) {
        long peopleAhead = 0L;
        double estimatedWait = 0.0;

        if (ticket.getStatus() == TicketStatus.WAITING && ticket.getCounter() != null && ticket.getTokenNumber() != null) {
            peopleAhead = ticketRepository.countByCounterIdAndStatusAndTokenNumberLessThan(
                    ticket.getCounter().getId(),
                    TicketStatus.WAITING,
                    ticket.getTokenNumber()
            );
            Double avgMinutes = ticketRepository.calculateAverageHandlingTimeMinutes(ticket.getCounter().getId());
            double safeAvg = (avgMinutes != null && avgMinutes > 0) ? avgMinutes : DEFAULT_SERVICE_TIME_MINUTES;
            estimatedWait = Math.round((peopleAhead + 1) * safeAvg * 10.0) / 10.0;
        }

        return TicketResponse.fromEntity(ticket, peopleAhead, estimatedWait);
    }
}
