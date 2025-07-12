package com.eternalcoders.pointedge.exception;

public class EntityNotFoundException extends RuntimeException {
    // Constructor that accepts a message
    public EntityNotFoundException(String message) {
        super(message);
    }
}