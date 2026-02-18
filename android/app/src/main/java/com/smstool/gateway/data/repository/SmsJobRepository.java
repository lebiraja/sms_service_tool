package com.smstool.gateway.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.smstool.gateway.data.db.AppDatabase;
import com.smstool.gateway.data.db.EventLogDao;
import com.smstool.gateway.data.db.EventLogEntity;
import com.smstool.gateway.data.db.SmsJobDao;
import com.smstool.gateway.data.db.SmsJobEntity;
import com.smstool.gateway.data.model.SmsJobStatus;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository layer providing high-level access to SMS job data and business logic.
 * All database operations run on a background thread.
 */
public class SmsJobRepository {
    private static final String TAG = "SmsJobRepository";
    private static final Executor DATABASE_EXECUTOR = Executors.newSingleThreadExecutor();

    private final SmsJobDao jobDao;
    private final EventLogDao eventLogDao;

    public SmsJobRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.jobDao = db.smsJobDao();
        this.eventLogDao = db.eventLogDao();
    }

    /**
     * Create a new SMS job in the local queue.
     */
    public void createJob(String toNumber, String body, int maxRetries, Runnable onComplete) {
        DATABASE_EXECUTOR.execute(() -> {
            String jobId = UUID.randomUUID().toString();
            long now = System.currentTimeMillis();

            SmsJobEntity job = new SmsJobEntity(
                    jobId,
                    toNumber,
                    body,
                    SmsJobStatus.QUEUED.getValue(),
                    0,  // attempts
                    maxRetries,
                    now,
                    now
            );

            jobDao.insertJob(job);
            logEvent("INFO", "Created job " + jobId + " to " + toNumber);

            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    /**
     * Get a job by ID.
     */
    public void getJob(String jobId, JobCallback callback) {
        DATABASE_EXECUTOR.execute(() -> {
            SmsJobEntity job = jobDao.getJobById(jobId);
            callback.onJobRetrieved(job);
        });
    }

    /**
     * Update a job's status.
     */
    public void updateJobStatus(String jobId, SmsJobStatus newStatus, Integer errorCode,
                                String errorMessage, Runnable onComplete) {
        DATABASE_EXECUTOR.execute(() -> {
            SmsJobEntity job = jobDao.getJobById(jobId);
            if (job != null) {
                job.status = newStatus.getValue();
                job.updatedAt = System.currentTimeMillis();
                job.errorCode = errorCode;
                job.errorMessage = errorMessage;

                if (newStatus == SmsJobStatus.SENT) {
                    job.sentAt = System.currentTimeMillis();
                } else if (newStatus == SmsJobStatus.DELIVERED) {
                    job.deliveredAt = System.currentTimeMillis();
                }

                jobDao.updateJob(job);
                logEvent("INFO", "Job " + jobId + " status -> " + newStatus.getValue());

                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    /**
     * Mark a job for retry.
     */
    public void scheduleRetry(String jobId, int attemptNumber, long delayMillis, Runnable onComplete) {
        DATABASE_EXECUTOR.execute(() -> {
            SmsJobEntity job = jobDao.getJobById(jobId);
            if (job != null) {
                job.status = SmsJobStatus.FAILED_RETRYING.getValue();
                job.attempts = attemptNumber;
                job.nextRetryAt = System.currentTimeMillis() + delayMillis;
                job.updatedAt = System.currentTimeMillis();

                jobDao.updateJob(job);
                logEvent("INFO", "Job " + jobId + " scheduled for retry in " + (delayMillis / 1000) + "s");

                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    /**
     * Get all jobs ready for retry (nextRetryAt <= now).
     */
    public void getJobsReadyForRetry(JobListCallback callback) {
        DATABASE_EXECUTOR.execute(() -> {
            List<SmsJobEntity> jobs = jobDao.getJobsReadyForRetry(System.currentTimeMillis());
            callback.onJobsRetrieved(jobs);
        });
    }

    /**
     * Get jobs with pending status reports (to flush to server on reconnect).
     */
    public void getJobsWithPendingReports(JobListCallback callback) {
        DATABASE_EXECUTOR.execute(() -> {
            List<SmsJobEntity> jobs = jobDao.getJobsWithPendingReports();
            callback.onJobsRetrieved(jobs);
        });
    }

    /**
     * Mark a job as having a pending report (status update couldn't be sent).
     */
    public void setPendingReport(String jobId, boolean pending, Runnable onComplete) {
        DATABASE_EXECUTOR.execute(() -> {
            SmsJobEntity job = jobDao.getJobById(jobId);
            if (job != null) {
                job.pendingReport = pending;
                jobDao.updateJob(job);

                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }

    /**
     * Get all jobs as LiveData (for MainActivity UI).
     */
    public LiveData<List<SmsJobEntity>> getAllJobsLive() {
        return jobDao.getAllJobsLive();
    }

    /**
     * Get recent activity log entries.
     */
    public LiveData<List<EventLogEntity>> getRecentEventsLive(int limit) {
        return eventLogDao.getRecentEventsLive(limit);
    }

    /**
     * Log an event for debugging and UI display.
     */
    public void logEvent(String level, String message) {
        DATABASE_EXECUTOR.execute(() -> {
            EventLogEntity event = new EventLogEntity(
                    System.currentTimeMillis(),
                    level,
                    message
            );
            eventLogDao.insertEvent(event);
            eventLogDao.cleanupOldEvents();
            Log.d(TAG, level + ": " + message);
        });
    }

    /**
     * Clear all data (for testing/reset).
     */
    public void clearAll() {
        DATABASE_EXECUTOR.execute(() -> {
            jobDao.deleteAll();
            eventLogDao.deleteAll();
            Log.i(TAG, "All data cleared");
        });
    }

    // Callbacks for async operations
    public interface JobCallback {
        void onJobRetrieved(SmsJobEntity job);
    }

    public interface JobListCallback {
        void onJobsRetrieved(List<SmsJobEntity> jobs);
    }
}
