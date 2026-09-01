package com.campusqueue.dto.response;

public class CounterStatsResponse {

    private Long counterId;
    private String counterName;
    private String counterCode;
    private double averageWaitMinutes;
    private double averageServiceMinutes;
    private long completedTickets;
    private long waitingTickets;

    public CounterStatsResponse() {
    }

    public CounterStatsResponse(Long counterId, String counterName, String counterCode,
                                double averageWaitMinutes, double averageServiceMinutes,
                                long completedTickets, long waitingTickets) {
        this.counterId = counterId;
        this.counterName = counterName;
        this.counterCode = counterCode;
        this.averageWaitMinutes = averageWaitMinutes;
        this.averageServiceMinutes = averageServiceMinutes;
        this.completedTickets = completedTickets;
        this.waitingTickets = waitingTickets;
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

    public long getCompletedTickets() {
        return completedTickets;
    }

    public void setCompletedTickets(long completedTickets) {
        this.completedTickets = completedTickets;
    }

    public long getWaitingTickets() {
        return waitingTickets;
    }

    public void setWaitingTickets(long waitingTickets) {
        this.waitingTickets = waitingTickets;
    }
}
