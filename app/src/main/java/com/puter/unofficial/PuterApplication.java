package com.puter.unofficial;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

/**
 * Global Application class for Puter Unofficial.
 * Initializes necessary notification channels for background voice/chat services.
 */
public class PuterApplication extends Application {

    // Unique ID for the AI voice and interaction notification channel
    public static final String CHANNEL_ID = "puter_voice_chat_channel";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize the notification channel required for Foreground Services
        createPuterNotificationChannel();
    }

    /**
     * Creates a Notification Channel for Puter AI interactions.
     * Required for Android 8.0 (API 26) and above.
     */
    private void createPuterNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // User-visible name for the channel (shown in system settings)
            CharSequence name = "Puter AI Agent";
            
            // Description of what this channel does
            String description = "Ensures Puter AI voice interactions remain active.";
            
            /* 
             * IMPORTANCE_LOW: The notification is shown in the tray 
             * but does not make an intrusive sound or pop up.
             */
            int importance = NotificationManager.IMPORTANCE_LOW;
            
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            // Register the channel with the Android system
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}