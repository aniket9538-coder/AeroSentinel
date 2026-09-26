package com.aerosentinel.integration.provider;

import java.util.ArrayList;
import java.util.List;

public class IngestionSummary {

    private String provider;
    private ProviderStatus status = ProviderStatus.SUCCESS;
    private int fetched = 0;
    private int mapped = 0;
    private int inserted = 0;
    private int duplicates = 0;
    private int rejected = 0;
    private List<String> rejectionReasons = new ArrayList<>();
    private String message;

    public IngestionSummary() {}

    public IngestionSummary(String provider) {
        this.provider = provider;
    }

    public void incrementFetched() {
        this.fetched++;
    }

    public void incrementMapped() {
        this.mapped++;
    }

    public void incrementInserted() {
        this.inserted++;
    }

    public void incrementDuplicates() {
        this.duplicates++;
    }

    public void incrementRejected(String reason) {
        this.rejected++;
        if (reason != null && !rejectionReasons.contains(reason)) {
            this.rejectionReasons.add(reason);
        }
    }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public ProviderStatus getStatus() { return status; }
    public void setStatus(ProviderStatus status) { this.status = status; }

    public int getFetched() { return fetched; }
    public void setFetched(int fetched) { this.fetched = fetched; }

    public int getMapped() { return mapped; }
    public void setMapped(int mapped) { this.mapped = mapped; }

    public int getInserted() { return inserted; }
    public void setInserted(int inserted) { this.inserted = inserted; }

    public int getDuplicates() { return duplicates; }
    public void setDuplicates(int duplicates) { this.duplicates = duplicates; }

    public int getRejected() { return rejected; }
    public void setRejected(int rejected) { this.rejected = rejected; }

    public List<String> getRejectionReasons() { return rejectionReasons; }
    public void setRejectionReasons(List<String> rejectionReasons) { this.rejectionReasons = rejectionReasons; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    @Override
    public String toString() {
        return "IngestionSummary{" +
                "provider='" + provider + '\'' +
                ", status=" + status +
                ", fetched=" + fetched +
                ", mapped=" + mapped +
                ", inserted=" + inserted +
                ", duplicates=" + duplicates +
                ", rejected=" + rejected +
                ", rejectionReasons=" + rejectionReasons +
                ", message='" + message + '\'' +
                '}';
    }
}
