package com.aerosentinel.citizen;

public class InvalidImageException extends RuntimeException {
    private final String errorCode;

    public InvalidImageException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}