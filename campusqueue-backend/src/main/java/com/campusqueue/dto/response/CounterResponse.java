package com.campusqueue.dto.response;

import com.campusqueue.entity.Counter;

import java.time.LocalDateTime;

public class CounterResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public CounterResponse() {
    }

    public CounterResponse(Long id, String name, String code, String description, Boolean isActive, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.code = code;
        this.description = description;
        this.isActive = isActive;
        this.createdAt = createdAt;
    }

    public static CounterResponse fromEntity(Counter counter) {
        if (counter == null) return null;
        return new CounterResponse(
                counter.getId(),
                counter.getName(),
                counter.getCode(),
                counter.getDescription(),
                counter.getIsActive(),
                counter.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
