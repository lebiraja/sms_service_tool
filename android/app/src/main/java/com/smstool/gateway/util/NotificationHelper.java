package com.smstool.gateway.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.smstool.gateway.MainActivity;
import com.smstool.gateway.R;

/**
 * Helper for creating and managing notifications.
 */
public class NotificationHelper {
    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "sms_gateway_channel";
    private static final int NOTIFICATION_ID = 1001;

    /**
     * Create and show the persistent notification for the foreground service.
     */
    public static Notification createForegroundNotification(Context context, String statusText) {
        createNotificationChannel(context);

        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("SMS Gateway")
                .setContentText(statusText)
                .setSmallIcon(android.R.drawable.ic_notification_overlay)  // Replace with app icon
                .setContentIntent(pendingIntent)
                .setOngoing(true)  // Persistent
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_STATUS);

        return builder.build();
    }

    /**
     * Update the notification text.
     */
    public static void updateNotification(Context context, String statusText) {
        NotificationManager manager = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE
        );
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, createForegroundNotification(context, statusText));
        }
    }

    /**
     * Create the notification channel (required for Android 8+).
     */
    private static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS Gateway",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Gateway connection status");
            NotificationManager manager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE
            );
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
