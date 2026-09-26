package com.aerosentinel.exception;

import java.time.Instant;

public class ApiError {
    private String timestamp;
    private int status;
    private String error;
    private String message;

    public ApiError() {
        this.timestamp = Instant.now().toString();
    }

    public ApiError(int status, String error, String message) {
        this.timestamp = Instant.now().toString();
        this.status = status;
        this.error = error;
        this.message = message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
