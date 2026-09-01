package com.campusqueue.dto.response;

import java.util.List;

public class QueueStatusResponse {

    private Long counterId;
    private String counterName;
    private String counterCode;
    private Boolean isActive;
    private String currentlyServingToken;
    private Long currentlyServingTicketId;
    private long totalWaiting;
    private double averageServiceMinutes;
    private double estimatedWaitMinutes;
    private List<TicketResponse> waitingTickets;

    public QueueStatusResponse() {
    }

    public QueueStatusResponse(Long counterId, String counterName, String counterCode, Boolean isActive,
                               String currentlyServingToken, Long currentlyServingTicketId,
                               long totalWaiting, double averageServiceMinutes, double estimatedWaitMinutes,
                               List<TicketResponse> waitingTickets) {
        this.counterId = counterId;
        this.counterName = counterName;
        this.counterCode = counterCode;
        this.isActive = isActive;
        this.currentlyServingToken = currentlyServingToken;
        this.currentlyServingTicketId = currentlyServingTicketId;
        this.totalWaiting = totalWaiting;
        this.averageServiceMinutes = averageServiceMinutes;
        this.estimatedWaitMinutes = estimatedWaitMinutes;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getCurrentlyServingToken() {
        return currentlyServingToken;
    }

    public void setCurrentlyServingToken(String currentlyServingToken) {
        this.currentlyServingToken = currentlyServingToken;
    }

    public Long getCurrentlyServingTicketId() {
        return currentlyServingTicketId;
    }

    public void setCurrentlyServingTicketId(Long currentlyServingTicketId) {
        this.currentlyServingTicketId = currentlyServingTicketId;
    }

    public long getTotalWaiting() {
        return totalWaiting;
    }

    public void setTotalWaiting(long totalWaiting) {
        this.totalWaiting = totalWaiting;
    }

    public double getAverageServiceMinutes() {
        return averageServiceMinutes;
    }

    public void setAverageServiceMinutes(double averageServiceMinutes) {
        this.averageServiceMinutes = averageServiceMinutes;
    }

    public double getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(double estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public List<TicketResponse> getWaitingTickets() {
        return waitingTickets;
    }

    public void setWaitingTickets(List<TicketResponse> waitingTickets) {
        this.waitingTickets = waitingTickets;
    }
}
