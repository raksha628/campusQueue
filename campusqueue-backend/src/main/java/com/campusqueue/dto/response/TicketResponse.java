package com.campusqueue.dto.response;

import com.campusqueue.entity.Ticket;
import com.campusqueue.entity.TicketStatus;

import java.time.LocalDateTime;

public class TicketResponse {

    private Long id;
    private String tokenNumber;
    private Long counterId;
    private String counterName;
    private String counterCode;
    private Long userId;
    private String userName;
    private TicketStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;
    private String remarks;
    private Long peopleAhead;
    private Double estimatedWaitMinutes;

    public TicketResponse() {
    }

    public static TicketResponse fromEntity(Ticket ticket, Long peopleAhead, Double estimatedWaitMinutes) {
        if (ticket == null) return null;

        TicketResponse response = new TicketResponse();
        response.setId(ticket.getId());
        response.setTokenNumber(ticket.getTokenNumber());
        if (ticket.getCounter() != null) {
            response.setCounterId(ticket.getCounter().getId());
            response.setCounterName(ticket.getCounter().getName());
            response.setCounterCode(ticket.getCounter().getCode());
        }
        if (ticket.getUser() != null) {
            response.setUserId(ticket.getUser().getId());
            response.setUserName(ticket.getUser().getName());
        }
        response.setStatus(ticket.getStatus());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setCalledAt(ticket.getCalledAt());
        response.setCompletedAt(ticket.getCompletedAt());
        response.setRemarks(ticket.getRemarks());
        response.setPeopleAhead(peopleAhead != null ? peopleAhead : 0L);
        response.setEstimatedWaitMinutes(estimatedWaitMinutes != null ? estimatedWaitMinutes : 0.0);

        return response;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public void setStatus(TicketStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCalledAt() {
        return calledAt;
    }

    public void setCalledAt(LocalDateTime calledAt) {
        this.calledAt = calledAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Long getPeopleAhead() {
        return peopleAhead;
    }

    public void setPeopleAhead(Long peopleAhead) {
        this.peopleAhead = peopleAhead;
    }

    public Double getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(Double estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }
}
