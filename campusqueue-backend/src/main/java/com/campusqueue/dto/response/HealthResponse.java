package com.campusqueue.dto.response;

import java.time.LocalDateTime;

public class HealthResponse {

    private String status;
    private String service;
    private LocalDateTime timestamp;

    public HealthResponse() {
    }

    public HealthResponse(String status, String service, LocalDateTime timestamp) {
        this.status = status;
        this.service = service;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
