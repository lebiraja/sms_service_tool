package com.smstool.gateway.data.db;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room entity for activity log entries.
 * Displayed in MainActivity for debugging and user visibility.
 */
@Entity(tableName = "event_log")
public class EventLogEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;  // epoch millis
    public String level;    // INFO, WARN, ERROR
    public String message;

    public EventLogEntity(long timestamp, String level, String message) {
        this.timestamp = timestamp;
        this.level = level;
        this.message = message;
    }
}
