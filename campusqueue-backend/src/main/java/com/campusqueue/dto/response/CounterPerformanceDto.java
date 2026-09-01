package com.campusqueue.dto.response;

public class CounterPerformanceDto {

    private Long counterId;
    private String counterName;
    private String counterCode;
    private long totalTickets;
    private long completedTickets;
    private long skippedTickets;
    private long cancelledTickets;
    private double averageWaitMinutes;
    private double averageServiceMinutes;

    public CounterPerformanceDto() {
    }

    public CounterPerformanceDto(Long counterId, String counterName, String counterCode,
                                 long totalTickets, long completedTickets, long skippedTickets,
                                 long cancelledTickets, double averageWaitMinutes, double averageServiceMinutes) {
        this.counterId = counterId;
        this.counterName = counterName;
        this.counterCode = counterCode;
        this.totalTickets = totalTickets;
        this.completedTickets = completedTickets;
        this.skippedTickets = skippedTickets;
        this.cancelledTickets = cancelledTickets;
        this.averageWaitMinutes = averageWaitMinutes;
        this.averageServiceMinutes = averageServiceMinutes;
    }

    public Long getCounterId() {
        return counterId;
    }

    public void setCounterId(Long counterId) {
        this.counterId = counterId;
    }

    public String getCounterName() {
        return counterName;
    }

    public void setCounterName(String counterName) {
        this.counterName = counterName;
    }

    public String getCounterCode() {
        return counterCode;
    }

    public void setCounterCode(String counterCode) {
        this.counterCode = counterCode;
    }

    public long getTotalTickets() {
        return totalTickets;
    }

    public void setTotalTickets(long totalTickets) {
        this.totalTickets = totalTickets;
    }

    public long getCompletedTickets() {
        return completedTickets;
    }

    public void setCompletedTickets(long completedTickets) {
        this.completedTickets = completedTickets;
    }

    public long getSkippedTickets() {
        return skippedTickets;
    }

    public void setSkippedTickets(long skippedTickets) {
        this.skippedTickets = skippedTickets;
    }

    public long getCancelledTickets() {
        return cancelledTickets;
    }

    public void setCancelledTickets(long cancelledTickets) {
        this.cancelledTickets = cancelledTickets;
    }

    public double getAverageWaitMinutes() {
        return averageWaitMinutes;
    }

    public void setAverageWaitMinutes(double averageWaitMinutes) {
        this.averageWaitMinutes = averageWaitMinutes;
    }

    public double getAverageServiceMinutes() {
        return averageServiceMinutes;
    }

    public void setAverageServiceMinutes(double averageServiceMinutes) {
        this.averageServiceMinutes = averageServiceMinutes;
    }
}
