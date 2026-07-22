package com.mindscape.app.reminders;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.mindscape.app.ReminderReceiver;

/**
 * Планирование/отмена напоминаний заметок через {@link AlarmManager}.
 * Все методы статические, не зависят от Activity.
 * Источник: вынесено из MainActivity.java (scheduleNoteReminder, cancelNoteReminder,
 * cancelReminderNotification, rescheduleActiveReminders).
 */
public final class ReminderScheduler {

    private ReminderScheduler() {}

    /** Планирует напоминание для заметки (или отменяет, если оно в прошлом). */
    public static void schedule(Context ctx, String notePath, String noteTitle, String noteContent, long triggerAt) {
        cancel(ctx, notePath);
        if (triggerAt <= System.currentTimeMillis()) return;

        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_REMINDER);
        intent.putExtra(ReminderReceiver.EXTRA_NOTE_PATH, notePath);
        intent.putExtra(ReminderReceiver.EXTRA_NOTE_TITLE, noteTitle);
        intent.putExtra(ReminderReceiver.EXTRA_NOTE_CONTENT, noteContent);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                ctx,
                ReminderReceiver.notificationId(notePath),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        } catch (SecurityException denied) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
        }
    }

    /** Отменяет напоминание по пути заметки и убирает активное уведомление. */
    public static void cancel(Context ctx, String notePath) {
        Intent intent = new Intent(ctx, ReminderReceiver.class);
        intent.setAction(ReminderReceiver.ACTION_REMINDER);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                ctx,
                ReminderReceiver.notificationId(notePath),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager alarmManager = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        pendingIntent.cancel();
        cancelNotification(ctx, notePath);
    }

    /** Снимает активное уведомление напоминания (без отмены alarm). */
    public static void cancelNotification(Context ctx, String notePath) {
        NotificationManager manager = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.cancel(ReminderReceiver.notificationId(notePath));
        }
    }
}