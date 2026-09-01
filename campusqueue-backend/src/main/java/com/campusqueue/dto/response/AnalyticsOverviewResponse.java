package com.campusqueue.dto.response;

import java.util.List;

public class AnalyticsOverviewResponse {

    private long totalTicketsOverall;
    private long totalCompletedOverall;
    private long totalWaitingOverall;
    private double averageWaitMinutesOverall;
    private double averageServiceMinutesOverall;
    private BusiestCounterResponse busiestCounter;
    private PeakHourResponse peakHour;
    private List<CounterPerformanceDto> counterPerformance;

    public AnalyticsOverviewResponse() {
    }

    public AnalyticsOverviewResponse(long totalTicketsOverall, long totalCompletedOverall,
                                     long totalWaitingOverall, double averageWaitMinutesOverall,
                                     double averageServiceMinutesOverall, BusiestCounterResponse busiestCounter,
                                     PeakHourResponse peakHour, List<CounterPerformanceDto> counterPerformance) {
        this.totalTicketsOverall = totalTicketsOverall;
        this.totalCompletedOverall = totalCompletedOverall;
        this.totalWaitingOverall = totalWaitingOverall;
        this.averageWaitMinutesOverall = averageWaitMinutesOverall;
        this.averageServiceMinutesOverall = averageServiceMinutesOverall;
        this.busiestCounter = busiestCounter;
        this.peakHour = peakHour;
        this.counterPerformance = counterPerformance;
    }

    public long getTotalTicketsOverall() {
        return totalTicketsOverall;
    }

    public void setTotalTicketsOverall(long totalTicketsOverall) {
        this.totalTicketsOverall = totalTicketsOverall;
    }

    public long getTotalCompletedOverall() {
        return totalCompletedOverall;
    }

    public void setTotalCompletedOverall(long totalCompletedOverall) {
        this.totalCompletedOverall = totalCompletedOverall;
    }

    public long getTotalWaitingOverall() {
        return totalWaitingOverall;
    }

    public void setTotalWaitingOverall(long totalWaitingOverall) {
        this.totalWaitingOverall = totalWaitingOverall;
    }

    public double getAverageWaitMinutesOverall() {
        return averageWaitMinutesOverall;
    }

    public void setAverageWaitMinutesOverall(double averageWaitMinutesOverall) {
        this.averageWaitMinutesOverall = averageWaitMinutesOverall;
    }

    public double getAverageServiceMinutesOverall() {
        return averageServiceMinutesOverall;
    }

    public void setAverageServiceMinutesOverall(double averageServiceMinutesOverall) {
        this.averageServiceMinutesOverall = averageServiceMinutesOverall;
    }

    public BusiestCounterResponse getBusiestCounter() {
        return busiestCounter;
    }

    public void setBusiestCounter(BusiestCounterResponse busiestCounter) {
        this.busiestCounter = busiestCounter;
    }

    public PeakHourResponse getPeakHour() {
        return peakHour;
    }

    public void setPeakHour(PeakHourResponse peakHour) {
        this.peakHour = peakHour;
    }

    public List<CounterPerformanceDto> getCounterPerformance() {
        return counterPerformance;
    }

    public void setCounterPerformance(List<CounterPerformanceDto> counterPerformance) {
        this.counterPerformance = counterPerformance;
    }
}
