package com.smstool.gateway.data.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database for SMSTool.
 * Stores SMS jobs and activity log.
 */
@Database(entities = {SmsJobEntity.class, EventLogEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract SmsJobDao smsJobDao();

    public abstract EventLogDao eventLogDao();

    /**
     * Get singleton instance of the database.
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "smstool.db"
                    ).build();
                }
            }
        }
        return instance;
    }

    /**
     * Close the database (for testing).
     */
    public static void closeDatabase() {
        if (instance != null) {
            instance.close();
            instance = null;
        }
    }
}
