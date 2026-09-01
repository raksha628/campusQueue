package com.campusqueue.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCounterRequest {

    @NotBlank(message = "Counter name is required")
    @Size(max = 100, message = "Counter name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Counter code is required")
    @Size(max = 10, message = "Counter code must not exceed 10 characters")
    private String code;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    public CreateCounterRequest() {
    }

    public CreateCounterRequest(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
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
}
