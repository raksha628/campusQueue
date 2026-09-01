package com.campusqueue.dto.projection;

public interface CounterPerformanceProjection {
    Long getCounterId();
    String getCounterName();
    String getCounterCode();
    Long getTotalTickets();
    Long getCompletedTickets();
    Long getSkippedTickets();
    Long getCancelledTickets();
    Double getAverageWaitMinutes();
    Double getAverageServiceMinutes();
}
