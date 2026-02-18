package com.smstool.gateway.data.model;

/**
 * Enum representing SMS job status.
 * Mirrors backend SmsJobStatus enum.
 */
public enum SmsJobStatus {
    QUEUED("queued"),
    SENDING("sending"),
    SENT("sent"),
    DELIVERED("delivered"),
    FAILED_RETRYING("failed_retrying"),
    FAILED_PERMANENT("failed_permanent");

    private final String value;

    SmsJobStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /**
     * Convert string to enum.
     */
    public static SmsJobStatus fromString(String value) {
        for (SmsJobStatus status : SmsJobStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }

    /**
     * Check if job has finished (no more retries will happen).
     */
    public boolean isTerminal() {
        return this == SENT || this == DELIVERED || this == FAILED_PERMANENT;
    }

    /**
     * Check if job failed and can be retried.
     */
    public boolean isRetryable() {
        return this == FAILED_RETRYING;
    }
}
