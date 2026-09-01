package com.campusqueue.repository;

import com.campusqueue.dto.projection.BusiestCounterProjection;
import com.campusqueue.dto.projection.CounterPerformanceProjection;
import com.campusqueue.dto.projection.DailyVolumeProjection;
import com.campusqueue.dto.projection.PeakHourProjection;
import com.campusqueue.entity.Ticket;
import com.campusqueue.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // =========================================================================
    // 1. Derived JPA Query Methods
    // =========================================================================

    List<Ticket> findByCounterId(Long counterId);

    List<Ticket> findByCounterIdOrderByCreatedAtAsc(Long counterId);

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByCounterIdAndStatus(Long counterId, TicketStatus status);

    List<Ticket> findByCounterIdAndStatusOrderByCreatedAtAsc(Long counterId, TicketStatus status);

    List<Ticket> findByCounterIdAndStatusOrderByTokenNumberAsc(Long counterId, TicketStatus status);

    List<Ticket> findByUserId(Long userId);

    List<Ticket> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Ticket> findByUserIdAndStatusIn(Long userId, Collection<TicketStatus> statuses);

    boolean existsByUserIdAndCounterIdAndStatusIn(Long userId, Long counterId, Collection<TicketStatus> statuses);

    Optional<Ticket> findFirstByCounterIdAndStatusOrderByCalledAtDesc(Long counterId, TicketStatus status);

    Optional<Ticket> findTopByCounterIdOrderByTokenNumberDesc(Long counterId);

    long countByCounterIdAndStatusAndTokenNumberLessThan(Long counterId, TicketStatus status, Integer tokenNumber);

    long countByStatus(TicketStatus status);

    // =========================================================================
    // 2. JPQL Query Methods
    // =========================================================================

    @Query("SELECT COALESCE(MAX(t.tokenNumber), 0) FROM Ticket t WHERE t.counter.id = :counterId")
    int findMaxTokenNumberByCounterId(@Param("counterId") Long counterId);

    // =========================================================================
    // 3. PostgreSQL Native SQL Analytics Queries
    // =========================================================================

    /**
     * A. Average Waiting Time (called_at - created_at) for a specific counter in minutes.
     */
    @Query(value = """
            SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (called_at - created_at)) / 60.0), 0.0) 
            FROM tickets 
            WHERE counter_id = :counterId 
              AND status = 'COMPLETED' 
              AND called_at IS NOT NULL 
              AND created_at IS NOT NULL
            """, nativeQuery = true)
    Double calculateAverageWaitingTimeMinutes(@Param("counterId") Long counterId);

    /**
     * Overall Average Waiting Time across all completed tickets.
     */
    @Query(value = """
            SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (called_at - created_at)) / 60.0), 0.0) 
            FROM tickets 
            WHERE status = 'COMPLETED' 
              AND called_at IS NOT NULL 
              AND created_at IS NOT NULL
            """, nativeQuery = true)
    Double calculateOverallAverageWaitingTimeMinutes();

    /**
     * B. Average Service Time (completed_at - called_at) for a specific counter in minutes.
     */
    @Query(value = """
            SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (completed_at - called_at)) / 60.0), 0.0) 
            FROM tickets 
            WHERE counter_id = :counterId 
              AND status = 'COMPLETED' 
              AND called_at IS NOT NULL 
              AND completed_at IS NOT NULL
            """, nativeQuery = true)
    Double calculateAverageHandlingTimeMinutes(@Param("counterId") Long counterId);

    /**
     * Overall Average Service Time across all completed tickets.
     */
    @Query(value = """
            SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (completed_at - called_at)) / 60.0), 0.0) 
            FROM tickets 
            WHERE status = 'COMPLETED' 
              AND called_at IS NOT NULL 
              AND completed_at IS NOT NULL
            """, nativeQuery = true)
    Double calculateOverallAverageServiceTimeMinutes();

    /**
     * C. Busiest Counter: Counter that handled the highest number of COMPLETED tickets.
     */
    @Query(value = """
            SELECT c.id AS counterId, c.name AS counterName, c.code AS counterCode, 
                   COUNT(t.id) AS handledTickets 
            FROM counters c 
            JOIN tickets t ON c.id = t.counter_id 
            WHERE t.status = 'COMPLETED' 
            GROUP BY c.id, c.name, c.code 
            ORDER BY handledTickets DESC, c.id ASC 
            LIMIT 1
            """, nativeQuery = true)
    Optional<BusiestCounterProjection> findBusiestCounter();

    /**
     * D. Daily Ticket Volume: Groups created tickets by calendar date.
     */
    @Query(value = """
            SELECT CAST(t.created_at AS DATE) AS date, 
                   COUNT(t.id) AS ticketCount 
            FROM tickets t 
            GROUP BY CAST(t.created_at AS DATE) 
            ORDER BY date ASC
            """, nativeQuery = true)
    List<DailyVolumeProjection> findDailyTicketVolume();

    /**
     * E. Peak Queue Hour: Hour of the day (0-23) when most tickets were generated.
     */
    @Query(value = """
            SELECT CAST(EXTRACT(HOUR FROM t.created_at) AS INTEGER) AS queueHour, 
                   COUNT(t.id) AS ticketCount 
            FROM tickets t 
            GROUP BY CAST(EXTRACT(HOUR FROM t.created_at) AS INTEGER) 
            ORDER BY ticketCount DESC, queueHour ASC 
            LIMIT 1
            """, nativeQuery = true)
    Optional<PeakHourProjection> findPeakQueueHour();

    /**
     * F. Counter Performance Aggregation: Calculates volume, breakdown by status,
     * and average wait/service metrics per counter in a single query.
     */
    @Query(value = """
            SELECT 
                c.id AS counterId, 
                c.name AS counterName, 
                c.code AS counterCode, 
                COUNT(t.id) AS totalTickets, 
                COUNT(CASE WHEN t.status = 'COMPLETED' THEN 1 END) AS completedTickets, 
                COUNT(CASE WHEN t.status = 'SKIPPED' THEN 1 END) AS skippedTickets, 
                COUNT(CASE WHEN t.status = 'CANCELLED' THEN 1 END) AS cancelledTickets, 
                COALESCE(AVG(CASE WHEN t.status = 'COMPLETED' AND t.called_at IS NOT NULL AND t.created_at IS NOT NULL 
                                  THEN EXTRACT(EPOCH FROM (t.called_at - t.created_at)) / 60.0 END), 0.0) AS averageWaitMinutes, 
                COALESCE(AVG(CASE WHEN t.status = 'COMPLETED' AND t.completed_at IS NOT NULL AND t.called_at IS NOT NULL 
                                  THEN EXTRACT(EPOCH FROM (t.completed_at - t.called_at)) / 60.0 END), 0.0) AS averageServiceMinutes 
            FROM counters c 
            LEFT JOIN tickets t ON c.id = t.counter_id 
            GROUP BY c.id, c.name, c.code 
            ORDER BY c.id ASC
            """, nativeQuery = true)
    List<CounterPerformanceProjection> findCounterPerformanceSummary();

    /**
     * G. Daily Counter Performance for a selected date.
     */
    @Query(value = """
            SELECT 
                c.id AS counterId, 
                c.name AS counterName, 
                c.code AS counterCode, 
                COUNT(t.id) AS totalTickets, 
                COUNT(CASE WHEN t.status = 'COMPLETED' THEN 1 END) AS completedTickets, 
                COUNT(CASE WHEN t.status = 'SKIPPED' THEN 1 END) AS skippedTickets, 
                COUNT(CASE WHEN t.status = 'CANCELLED' THEN 1 END) AS cancelledTickets, 
                COALESCE(AVG(CASE WHEN t.status = 'COMPLETED' AND t.called_at IS NOT NULL AND t.created_at IS NOT NULL 
                                  THEN EXTRACT(EPOCH FROM (t.called_at - t.created_at)) / 60.0 END), 0.0) AS averageWaitMinutes, 
                COALESCE(AVG(CASE WHEN t.status = 'COMPLETED' AND t.completed_at IS NOT NULL AND t.called_at IS NOT NULL 
                                  THEN EXTRACT(EPOCH FROM (t.completed_at - t.called_at)) / 60.0 END), 0.0) AS averageServiceMinutes 
            FROM counters c 
            LEFT JOIN tickets t ON c.id = t.counter_id AND CAST(t.created_at AS DATE) = :targetDate 
            GROUP BY c.id, c.name, c.code 
            ORDER BY c.id ASC
            """, nativeQuery = true)
    List<CounterPerformanceProjection> findDailyCounterPerformanceSummary(@Param("targetDate") LocalDate targetDate);

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
