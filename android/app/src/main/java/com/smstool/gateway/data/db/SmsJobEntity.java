package com.smstool.gateway.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity representing an SMS job in the local queue.
 * Persisted to survive app crashes and process death.
 */
@Entity(tableName = "sms_jobs")
public class SmsJobEntity {
    @PrimaryKey
    @NonNull
    public String jobId;

    public String toNumber;
    public String body;
    public String status;  // queued, sending, sent, delivered, failed_retrying, failed_permanent

    public int attempts;
    public int maxRetries;

    public long createdAt;  // epoch millis
    public long updatedAt;

    public Long sentAt;      // nullable
    public Long deliveredAt; // nullable

    public Integer errorCode;      // nullable
    public String errorMessage;    // nullable

    public Long nextRetryAt;  // epoch millis, when to retry next (nullable)
    public boolean pendingReport; // true if status update couldn't be sent to server

    // Constructor
    public SmsJobEntity(String jobId, String toNumber, String body, String status,
                        int attempts, int maxRetries, long createdAt, long updatedAt) {
        this.jobId = jobId;
        this.toNumber = toNumber;
        this.body = body;
        this.status = status;
        this.attempts = attempts;
        this.maxRetries = maxRetries;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.pendingReport = false;
    }
}
