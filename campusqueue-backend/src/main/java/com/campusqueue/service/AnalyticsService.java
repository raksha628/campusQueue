package com.campusqueue.service;

import com.campusqueue.dto.projection.BusiestCounterProjection;
import com.campusqueue.dto.projection.CounterPerformanceProjection;
import com.campusqueue.dto.projection.DailyVolumeProjection;
import com.campusqueue.dto.projection.PeakHourProjection;
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
import com.campusqueue.exception.ResourceNotFoundException;
import com.campusqueue.repository.CounterRepository;
import com.campusqueue.repository.TicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TicketRepository ticketRepository;
    private final CounterRepository counterRepository;

    public AnalyticsService(TicketRepository ticketRepository, CounterRepository counterRepository) {
        this.ticketRepository = ticketRepository;
        this.counterRepository = counterRepository;
    }

    /**
     * A. Returns all currently WAITING tickets for a given counter ordered by positive token number (#1, #2, #3...).
     */
    public List<TicketResponse> getWaitingQueueOrderedByToken(Long counterId) {
        if (!counterRepository.existsById(counterId)) {
            throw new ResourceNotFoundException("Counter not found with id: " + counterId);
        }
        return ticketRepository.findByCounterIdAndStatusOrderByTokenNumberAsc(counterId, TicketStatus.WAITING).stream()
                .map(this::mapToSimpleTicketResponse)
                .collect(Collectors.toList());
    }

    /**
     * B. Returns specific average wait and service statistics for a counter.
     */
    public CounterStatsResponse getCounterStats(Long counterId) {
        Counter counter = counterRepository.findById(counterId)
                .orElseThrow(() -> new ResourceNotFoundException("Counter not found with id: " + counterId));

        Double avgWait = ticketRepository.calculateAverageWaitingTimeMinutes(counterId);
        Double avgService = ticketRepository.calculateAverageHandlingTimeMinutes(counterId);

        double safeAvgWait = avgWait != null ? Math.round(avgWait * 10.0) / 10.0 : 0.0;
        double safeAvgService = avgService != null ? Math.round(avgService * 10.0) / 10.0 : 0.0;

        long waitingCount = ticketRepository.findByCounterIdAndStatus(counterId, TicketStatus.WAITING).size();
        long completedCount = ticketRepository.findByCounterIdAndStatus(counterId, TicketStatus.COMPLETED).size();

        return new CounterStatsResponse(
                counter.getId(),
                counter.getName(),
                counter.getCode(),
                safeAvgWait,
                safeAvgService,
                completedCount,
                waitingCount
        );
    }

    /**
     * C. Returns the busiest counter based on total COMPLETED tickets.
     */
    public BusiestCounterResponse getBusiestCounter() {
        return ticketRepository.findBusiestCounter()
                .map(p -> new BusiestCounterResponse(p.getCounterId(), p.getCounterName(), p.getCounterCode(), p.getHandledTickets()))
                .orElse(new BusiestCounterResponse(null, "None", "N/A", 0L));
    }

    /**
     * D. Returns daily ticket generation volume grouped by calendar date.
     */
    public List<DailyVolumeResponse> getDailyTicketVolume() {
        return ticketRepository.findDailyTicketVolume().stream()
                .map(p -> new DailyVolumeResponse(p.getDate(), p.getTicketCount()))
                .collect(Collectors.toList());
    }

    /**
     * E. Returns the peak queue hour of the day.
     */
    public PeakHourResponse getPeakQueueHour() {
        return ticketRepository.findPeakQueueHour()
                .map(p -> {
                    int hour = p.getQueueHour() != null ? p.getQueueHour() : 0;
                    String formatted = String.format("%02d:00 - %02d:00", hour, (hour + 1) % 24);
                    return new PeakHourResponse(hour, formatted, p.getTicketCount());
                })
                .orElse(new PeakHourResponse(0, "00:00 - 01:00", 0L));
    }

    /**
     * F & G. Returns per-counter performance breakdown (all time or filtered by single date).
     */
    public List<CounterPerformanceDto> getCounterPerformance(LocalDate date) {
        List<CounterPerformanceProjection> projections = (date != null)
                ? ticketRepository.findDailyCounterPerformanceSummary(date)
                : ticketRepository.findCounterPerformanceSummary();

        return projections.stream()
                .map(p -> new CounterPerformanceDto(
                        p.getCounterId(),
                        p.getCounterName(),
                        p.getCounterCode(),
                        p.getTotalTickets() != null ? p.getTotalTickets() : 0L,
                        p.getCompletedTickets() != null ? p.getCompletedTickets() : 0L,
                        p.getSkippedTickets() != null ? p.getSkippedTickets() : 0L,
                        p.getCancelledTickets() != null ? p.getCancelledTickets() : 0L,
                        p.getAverageWaitMinutes() != null ? Math.round(p.getAverageWaitMinutes() * 10.0) / 10.0 : 0.0,
                        p.getAverageServiceMinutes() != null ? Math.round(p.getAverageServiceMinutes() * 10.0) / 10.0 : 0.0
                ))
                .collect(Collectors.toList());
    }

    /**
     * High-level Analytics Overview dashboard payload.
     */
    public AnalyticsOverviewResponse getOverview() {
        long totalTickets = ticketRepository.count();
        long totalCompleted = ticketRepository.countByStatus(TicketStatus.COMPLETED);
        long totalWaiting = ticketRepository.countByStatus(TicketStatus.WAITING);

        Double avgWait = ticketRepository.calculateOverallAverageWaitingTimeMinutes();
        Double avgService = ticketRepository.calculateOverallAverageServiceTimeMinutes();

        double safeAvgWait = avgWait != null ? Math.round(avgWait * 10.0) / 10.0 : 0.0;
        double safeAvgService = avgService != null ? Math.round(avgService * 10.0) / 10.0 : 0.0;

        BusiestCounterResponse busiest = getBusiestCounter();
        PeakHourResponse peakHour = getPeakQueueHour();
        List<CounterPerformanceDto> performance = getCounterPerformance(null);

        return new AnalyticsOverviewResponse(
                totalTickets,
                totalCompleted,
                totalWaiting,
                safeAvgWait,
                safeAvgService,
                busiest,
                peakHour,
                performance
        );
    }

    private TicketResponse mapToSimpleTicketResponse(Ticket ticket) {
        long peopleAhead = ticketRepository.countByCounterIdAndStatusAndTokenNumberLessThan(
                ticket.getCounter().getId(),
                TicketStatus.WAITING,
                ticket.getTokenNumber()
        );
        return TicketResponse.fromEntity(ticket, peopleAhead, 0.0);
    }
}
