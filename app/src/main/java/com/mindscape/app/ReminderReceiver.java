package com.mindscape.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public final class ReminderReceiver extends BroadcastReceiver {
    public static final String ACTION_REMINDER = "com.mindscape.app.ACTION_REMINDER";
    public static final String EXTRA_NOTE_PATH = "note_path";
    public static final String EXTRA_NOTE_TITLE = "note_title";
    public static final String EXTRA_NOTE_CONTENT = "note_content";
    public static final String CHANNEL_ID = "mindscape_note_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION_REMINDER.equals(intent.getAction())) return;

        String notePath = intent.getStringExtra(EXTRA_NOTE_PATH);
        String title = intent.getStringExtra(EXTRA_NOTE_TITLE);
        String content = intent.getStringExtra(EXTRA_NOTE_CONTENT);

        if (title == null || title.trim().isEmpty()) title = context.getString(R.string.str_note_reminder);
        if (content == null || content.trim().isEmpty()) content = context.getString(R.string.str_open_note);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        ensureChannel(context, manager);

        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.putExtra(EXTRA_NOTE_PATH, notePath);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
                context,
                notificationId(notePath),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content))
                .setContentIntent(openPending)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .build();

        manager.notify(notificationId(notePath), notification);
    }

    public static int notificationId(String notePath) {
        return notePath == null ? 1001 : (notePath.hashCode() & 0x7fffffff);
    }

    private static void ensureChannel(Context context, NotificationManager manager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.str_reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.str_reminder_channel_description));
        manager.createNotificationChannel(channel);
    }
}
