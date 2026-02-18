package com.smstool.gateway.data.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object for event log entries.
 */
@Dao
public interface EventLogDao {
    @Insert
    long insertEvent(EventLogEntity event);

    /**
     * Get the most recent N log entries (for displaying in MainActivity).
     */
    @Query("SELECT * FROM event_log ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<EventLogEntity>> getRecentEventsLive(int limit);

    /**
     * Get recent events synchronously (for testing).
     */
    @Query("SELECT * FROM event_log ORDER BY timestamp DESC LIMIT :limit")
    List<EventLogEntity> getRecentEvents(int limit);

    /**
     * Clear old log entries (keep last 500).
     */
    @Query("DELETE FROM event_log WHERE id NOT IN (SELECT id FROM event_log ORDER BY timestamp DESC LIMIT 500)")
    void cleanupOldEvents();

    /**
     * Delete all events (for testing).
     */
    @Query("DELETE FROM event_log")
    void deleteAll();
}
