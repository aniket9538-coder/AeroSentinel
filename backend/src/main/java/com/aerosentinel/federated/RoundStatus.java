package com.aerosentinel.federated;

public enum RoundStatus {
    CREATED,
    MODEL_DISTRIBUTED,
    TRAINING,
    UPDATES_COLLECTING,
    AGGREGATING,
    COMPLETED,
    FAILED
}
