package com.campusqueue.repository;

import com.campusqueue.entity.Ticket;
import com.campusqueue.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // =========================================================================
    // 1. Derived JPA Query Methods
    // =========================================================================

    /**
     * Finds all tickets issued for a specific counter.
     */
    List<Ticket> findByCounterId(Long counterId);

    /**
     * Finds all tickets for a counter ordered chronologically.
     */
    List<Ticket> findByCounterIdOrderByCreatedAtAsc(Long counterId);

    /**
     * Finds all tickets currently in a given status (e.g. WAITING across all counters).
     */
    List<Ticket> findByStatus(TicketStatus status);

    /**
     * Finds all tickets for a specific counter in a specific status (e.g. WAITING queue).
     */
    List<Ticket> findByCounterIdAndStatus(Long counterId, TicketStatus status);

    /**
     * Finds all waiting tickets for a counter ordered by creation time (FIFO queue order).
     */
    List<Ticket> findByCounterIdAndStatusOrderByCreatedAtAsc(Long counterId, TicketStatus status);

    /**
     * Finds all tickets created by a specific user/student.
     */
    List<Ticket> findByUserId(Long userId);

    /**
     * Finds all tickets created by a user ordered with newest first.
     */
    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Finds active tickets (WAITING or CALLED) for a specific user.
     */
    List<Ticket> findByUserIdAndStatusIn(Long userId, Collection<TicketStatus> statuses);

    /**
     * Checks if a user already has an active ticket for a specific counter.
     */
    boolean existsByUserIdAndCounterIdAndStatusIn(Long userId, Long counterId, Collection<TicketStatus> statuses);

    /**
     * Finds the currently CALLED ticket for a counter.
     */
    Optional<Ticket> findFirstByCounterIdAndStatusOrderByCalledAtDesc(Long counterId, TicketStatus status);

    /**
     * Finds the latest ticket issued for a counter (to retrieve highest token number).
     */
    Optional<Ticket> findTopByCounterIdOrderByTokenNumberDesc(Long counterId);

    /**
     * Counts how many waiting tickets exist ahead of this ticket in the queue
     * based on sequential token number comparison.
     */
    long countByCounterIdAndStatusAndTokenNumberLessThan(Long counterId, TicketStatus status, Integer tokenNumber);

    // =========================================================================
    // 2. JPQL Query Methods
    // =========================================================================

    /**
     * Finds the maximum token number issued for a counter today, defaulting to 0 if none exist.
     */
    @Query("SELECT COALESCE(MAX(t.tokenNumber), 0) FROM Ticket t WHERE t.counter.id = :counterId")
    int findMaxTokenNumberByCounterId(@Param("counterId") Long counterId);

    // =========================================================================
    // 3. Native SQL Queries (for complex analytics & atomic operations)
    // =========================================================================

    /**
     * Computes the moving average handling time in minutes for completed tickets at this counter.
     */
    @Query(value = """
            SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (completed_at - called_at)) / 60.0), 5.0) 
            FROM tickets 
            WHERE counter_id = :counterId 
              AND status = 'COMPLETED' 
              AND called_at IS NOT NULL 
              AND completed_at IS NOT NULL
            """, nativeQuery = true)
    Double calculateAverageHandlingTimeMinutes(@Param("counterId") Long counterId);

    /**
     * Concurrency-safe retrieval of next waiting ticket using PostgreSQL row-level pessimistic locking.
     */
    @Query(value = """
            SELECT * FROM tickets 
            WHERE counter_id = :counterId 
              AND status = 'WAITING' 
            ORDER BY created_at ASC 
            LIMIT 1 
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<Ticket> findNextWaitingTicketForUpdate(@Param("counterId") Long counterId);
}
