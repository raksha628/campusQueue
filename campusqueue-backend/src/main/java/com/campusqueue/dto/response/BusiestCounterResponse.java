package com.campusqueue.dto.response;

public class BusiestCounterResponse {

    private Long counterId;
    private String counterName;
    private String counterCode;
    private long handledTickets;

    public BusiestCounterResponse() {
    }

    public BusiestCounterResponse(Long counterId, String counterName, String counterCode, long handledTickets) {
        this.counterId = counterId;
        this.counterName = counterName;
        this.counterCode = counterCode;
        this.handledTickets = handledTickets;
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

    public long getHandledTickets() {
        return handledTickets;
    }

    public void setHandledTickets(long handledTickets) {
        this.handledTickets = handledTickets;
    }
}
