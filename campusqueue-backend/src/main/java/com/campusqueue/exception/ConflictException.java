package com.campusqueue.exception;

public class ConflictException extends BadRequestException {

    public ConflictException(String message) {
        super(message);
    }
}
