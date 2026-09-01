package com.campusqueue.repository;

import com.campusqueue.entity.Ticket;
import com.campusqueue.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // --- JPA Derived Methods ---

    List<Ticket> findByUserIdAndStatusIn(Long userId, Collection<TicketStatus> statuses);

    List<Ticket> findByCounterIdAndStatusOrderByCreatedAtAsc(Long counterId, TicketStatus status);

    Optional<Ticket> findFirstByCounterIdAndStatusOrderByCalledAtDesc(Long counterId, TicketStatus status);

    boolean existsByUserIdAndCounterIdAndStatusIn(Long userId, Long counterId, Collection<TicketStatus> statuses);

    // --- Explicit SQL / Native Queries ---

    /**
     * Counts the number of waiting tickets created before this ticket for the same counter (people ahead).
     */
    @Query(value = """
            SELECT COUNT(*) FROM tickets 
            WHERE counter_id = :counterId 
              AND status = 'WAITING' 
              AND created_at < :createdAt
            """, nativeQuery = true)
    long countPeopleAhead(@Param("counterId") Long counterId, @Param("createdAt") LocalDateTime createdAt);

    /**
     * Counts how many tickets were generated today for a given counter to build the daily sequence number.
     */
    @Query(value = """
            SELECT COUNT(*) FROM tickets 
            WHERE counter_id = :counterId 
              AND CAST(created_at AS DATE) = CURRENT_DATE
            """, nativeQuery = true)
    long countTodayTicketsByCounterId(@Param("counterId") Long counterId);

    /**
     * Calculates the moving average handling time (in minutes) for completed tickets today at this counter.
     * Defaults to 5.0 minutes if no tickets have been completed yet today.
     */
    @Query(value = """
            SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (completed_at - called_at)) / 60.0), 5.0) 
            FROM tickets 
            WHERE counter_id = :counterId 
              AND status = 'COMPLETED' 
              AND called_at IS NOT NULL 
              AND completed_at IS NOT NULL 
              AND CAST(completed_at AS DATE) = CURRENT_DATE
            """, nativeQuery = true)
    Double calculateAverageHandlingTimeMinutes(@Param("counterId") Long counterId);

    /**
     * Concurrency-safe fetch of the next WAITING ticket using PostgreSQL row-level pessimistic locking.
     * SKIP LOCKED ensures multiple staff desks calling simultaneously do not encounter lock contention or race conditions.
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
