package com.campusqueue.controller;

import com.campusqueue.dto.response.AnalyticsOverviewResponse;
import com.campusqueue.dto.response.BusiestCounterResponse;
import com.campusqueue.dto.response.CounterPerformanceDto;
import com.campusqueue.dto.response.CounterStatsResponse;
import com.campusqueue.dto.response.DailyVolumeResponse;
import com.campusqueue.dto.response.PeakHourResponse;
import com.campusqueue.dto.response.TicketResponse;
import com.campusqueue.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * A. Returns all WAITING tickets for a given counter ordered by token number.
     */
    @GetMapping("/counters/{counterId}/queue")
    public ResponseEntity<List<TicketResponse>> getWaitingQueue(@PathVariable Long counterId) {
        return ResponseEntity.ok(analyticsService.getWaitingQueueOrderedByToken(counterId));
    }

    /**
     * B. Returns wait and service metrics for a specific counter.
     */
    @GetMapping("/counters/{counterId}/stats")
    public ResponseEntity<CounterStatsResponse> getCounterStats(@PathVariable Long counterId) {
        return ResponseEntity.ok(analyticsService.getCounterStats(counterId));
    }

    /**
     * C. Returns system-wide high level analytics overview.
     */
    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getOverview() {
        return ResponseEntity.ok(analyticsService.getOverview());
    }

    /**
     * D. Returns busiest counter by total completed tickets.
     */
    @GetMapping("/busiest-counter")
    public ResponseEntity<BusiestCounterResponse> getBusiestCounter() {
        return ResponseEntity.ok(analyticsService.getBusiestCounter());
    }

    /**
     * E. Returns daily ticket volume grouped by date.
     */
    @GetMapping("/daily-volume")
    public ResponseEntity<List<DailyVolumeResponse>> getDailyVolume() {
        return ResponseEntity.ok(analyticsService.getDailyTicketVolume());
    }

    /**
     * F. Returns the peak queue traffic hour.
     */
    @GetMapping("/peak-hour")
    public ResponseEntity<PeakHourResponse> getPeakHour() {
        return ResponseEntity.ok(analyticsService.getPeakQueueHour());
    }

    /**
     * G. Returns per-counter performance breakdown, optionally filtered by a specific date.
     */
    @GetMapping("/performance")
    public ResponseEntity<List<CounterPerformanceDto>> getCounterPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(analyticsService.getCounterPerformance(date));
    }
}
