package com.aerosentinel.alert;

public enum AlertStatus {
    OPEN,
    ACKNOWLEDGED,
    CLOSED;

    public boolean canTransitionTo(AlertStatus next) {
        return switch (this) {
            case OPEN -> next == ACKNOWLEDGED || next == CLOSED;
            case ACKNOWLEDGED -> next == CLOSED;
            case CLOSED -> false;
        };
    }
}