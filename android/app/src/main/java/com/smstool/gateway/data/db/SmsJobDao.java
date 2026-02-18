package com.smstool.gateway.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

/**
 * Data Access Object for SMS jobs.
 */
@Dao
public interface SmsJobDao {
    @Insert
    long insertJob(SmsJobEntity job);

    @Update
    void updateJob(SmsJobEntity job);

    @Delete
    void deleteJob(SmsJobEntity job);

    @Query("SELECT * FROM sms_jobs WHERE jobId = :jobId")
    SmsJobEntity getJobById(String jobId);

    /**
     * Get all jobs with a specific status.
     */
    @Query("SELECT * FROM sms_jobs WHERE status = :status ORDER BY updatedAt DESC")
    List<SmsJobEntity> getJobsByStatus(String status);

    /**
     * Get jobs that need to be retried (failed_retrying status and nextRetryAt <= now).
     */
    @Query("SELECT * FROM sms_jobs WHERE status = 'failed_retrying' AND nextRetryAt <= :nowMillis ORDER BY nextRetryAt ASC")
    List<SmsJobEntity> getJobsReadyForRetry(long nowMillis);

    /**
     * Get jobs with pending status reports (status update couldn't be sent to server).
     */
    @Query("SELECT * FROM sms_jobs WHERE pendingReport = 1 ORDER BY updatedAt ASC")
    List<SmsJobEntity> getJobsWithPendingReports();

    /**
     * Get all jobs ordered by creation time (newest first).
     * Observed as LiveData for UI updates.
     */
    @Query("SELECT * FROM sms_jobs ORDER BY createdAt DESC")
    LiveData<List<SmsJobEntity>> getAllJobsLive();

    /**
     * Count jobs with a specific status.
     */
    @Query("SELECT COUNT(*) FROM sms_jobs WHERE status = :status")
    int countJobsByStatus(String status);

    /**
     * Delete jobs older than a specified timestamp (for cleanup).
     */
    @Query("DELETE FROM sms_jobs WHERE createdAt < :olderThanMillis")
    int deleteJobsOlderThan(long olderThanMillis);

    /**
     * Clear all jobs (for testing).
     */
    @Query("DELETE FROM sms_jobs")
    void deleteAll();
}
