package com.campusqueue.dto.request;

import jakarta.validation.constraints.NotNull;

public class CreateTicketRequest {

    @NotNull(message = "Counter ID is required")
    private Long counterId;

    @NotNull(message = "User ID is required")
    private Long userId;

    public CreateTicketRequest() {
    }

    public CreateTicketRequest(Long counterId, Long userId) {
        this.counterId = counterId;
        this.userId = userId;
    }

    public Long getCounterId() {
        return counterId;
    }

    public void setCounterId(Long counterId) {
        this.counterId = counterId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
