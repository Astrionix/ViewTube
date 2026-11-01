package com.newtube.mobile.ui.playback;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton sleep timer manager for NewTube mobile playback.
 *
 * <p>Supports countdown durations (10m, 15m, 20m, 30m, 45m, 60m, custom) and "End of video" mode.
 * Realtime tracking uses {@link SystemClock#elapsedRealtime()} so sleep states and deep sleep
 * are accurately measured. Works in foreground, mini player, and background playback via
 * {@link MobilePlaybackService}. When the timer expires, playback is paused and a gentle
 * confirmation is shown.</p>
 */
public final class SleepTimer {

    public static final int MODE_OFF = 0;
    public static final int MODE_DURATION = 1;
    public static final int MODE_END_OF_VIDEO = 2;

    private static final int ALARM_REQUEST_CODE = 41339;

    public interface PauseCallback {
        void pause();
    }

    public interface Listener {
        void onSleepTimerChanged(SleepTimer timer);
    }

    private static final SleepTimer sInstance = new SleepTimer();

    public static SleepTimer instance() {
        return sInstance;
    }

    private int mMode = MODE_OFF;
    private int mInitialMinutes = 0;
    private long mTargetRealtimeMs = 0;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final List<Listener> mListeners = new ArrayList<>();
    @Nullable private PauseCallback mPauseCallback;
    @Nullable private Context mAppContext;

    private final Runnable mExpireRunnable = this::onHandlerExpired;

    private SleepTimer() {
    }

    public void init(@NonNull Context context) {
        mAppContext = context.getApplicationContext();
    }

    public synchronized void setPauseCallback(@Nullable PauseCallback callback) {
        mPauseCallback = callback;
    }

    public synchronized void addListener(@NonNull Listener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public synchronized void removeListener(@NonNull Listener listener) {
        mListeners.remove(listener);
    }

    public synchronized int getMode() {
        if (mMode == MODE_DURATION && SystemClock.elapsedRealtime() >= mTargetRealtimeMs) {
            mMode = MODE_OFF;
        }
        return mMode;
    }

    public synchronized boolean isActive() {
        return getMode() != MODE_OFF;
    }

    public synchronized boolean isEndOfVideo() {
        return getMode() == MODE_END_OF_VIDEO;
    }

    public synchronized int getInitialMinutes() {
        return mInitialMinutes;
    }

    public synchronized long getRemainingMs() {
        if (getMode() != MODE_DURATION) {
            return 0;
        }
        return Math.max(0, mTargetRealtimeMs - SystemClock.elapsedRealtime());
    }

    public synchronized int getRemainingMinutes() {
        long ms = getRemainingMs();
        return (int) Math.ceil(ms / 60000.0);
    }

    @MainThread
    public synchronized void start(int minutes) {
        if (minutes <= 0) {
            stop();
            return;
        }
        mMode = MODE_DURATION;
        mInitialMinutes = minutes;
        mTargetRealtimeMs = SystemClock.elapsedRealtime() + (minutes * 60_000L);

        scheduleAlarm();
        scheduleHandler();
        notifyChanged();
    }

    @MainThread
    public synchronized void startEndOfVideo() {
        cancelAlarm();
        cancelHandler();
        mMode = MODE_END_OF_VIDEO;
        mInitialMinutes = 0;
        mTargetRealtimeMs = 0;
        notifyChanged();
    }

    @MainThread
    public synchronized void addMinutes(int minutes) {
        if (mMode != MODE_DURATION || !isActive()) {
            start(minutes);
            return;
        }
        mInitialMinutes += minutes;
        mTargetRealtimeMs += (minutes * 60_000L);

        scheduleAlarm();
        scheduleHandler();
        notifyChanged();
    }

    @MainThread
    public synchronized void stop() {
        cancelAlarm();
        cancelHandler();
        mMode = MODE_OFF;
        mInitialMinutes = 0;
        mTargetRealtimeMs = 0;
        notifyChanged();
    }

    /**
     * Triggered when the current video ends. If "End of video" mode was active,
     * stops the timer, pauses playback, and displays confirmation.
     */
    @MainThread
    public synchronized void onVideoEnded() {
        if (mMode != MODE_END_OF_VIDEO) {
            return;
        }
        stop();
        triggerPause();
        showFeedback(mAppContext, R.string.mobile_sleep_timer_paused);
    }

    /**
     * Triggered by {@link SleepTimerReceiver} or the internal Handler when duration expires.
     */
    @MainThread
    public synchronized void onTimerExpired(@Nullable Context context) {
        if (mMode != MODE_DURATION) {
            return;
        }
        stop();
        triggerPause();
        showFeedback(context != null ? context : mAppContext, R.string.mobile_sleep_timer_paused);
    }

    private void onHandlerExpired() {
        onTimerExpired(mAppContext);
    }

    private void triggerPause() {
        if (mPauseCallback != null) {
            mPauseCallback.pause();
        }
    }

    private void showFeedback(@Nullable Context context, int stringResId) {
        if (context == null) {
            return;
        }
        mHandler.post(() -> {
            try {
                Toast.makeText(context, stringResId, Toast.LENGTH_SHORT).show();
            } catch (Exception ignored) {
            }
        });
    }

    private void scheduleHandler() {
        cancelHandler();
        long delayMs = getRemainingMs();
        if (delayMs > 0) {
            mHandler.postDelayed(mExpireRunnable, delayMs);
        }
    }

    private void cancelHandler() {
        mHandler.removeCallbacks(mExpireRunnable);
    }

    private void scheduleAlarm() {
        if (mAppContext == null) {
            return;
        }
        AlarmManager am = (AlarmManager) mAppContext.getSystemService(Context.ALARM_SERVICE);
        if (am == null) {
            return;
        }
        PendingIntent pi = getAlarmPendingIntent(mAppContext);
        long triggerRealtimeMs = mTargetRealtimeMs;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerRealtimeMs, pi);
                } else {
                    am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerRealtimeMs, pi);
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerRealtimeMs, pi);
            } else {
                am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerRealtimeMs, pi);
            }
        } catch (SecurityException ignored) {
            try {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerRealtimeMs, pi);
            } catch (Exception fallbackIgnored) {
            }
        }
    }

    private void cancelAlarm() {
        if (mAppContext == null) {
            return;
        }
        AlarmManager am = (AlarmManager) mAppContext.getSystemService(Context.ALARM_SERVICE);
        if (am != null) {
            am.cancel(getAlarmPendingIntent(mAppContext));
        }
    }

    private PendingIntent getAlarmPendingIntent(@NonNull Context context) {
        Intent intent = new Intent(context, SleepTimerReceiver.class);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent, flags);
    }

    private void notifyChanged() {
        List<Listener> copy;
        synchronized (this) {
            copy = new ArrayList<>(mListeners);
        }
        for (Listener l : copy) {
            l.onSleepTimerChanged(this);
        }
    }

    /**
     * Formatted string for the gear sheet row (e.g. "25m left", "End of video", or null if off).
     */
    @Nullable
    public synchronized String getFormattedRemaining(@NonNull Context context) {
        if (!isActive()) {
            return null;
        }
        if (isEndOfVideo()) {
            return context.getString(R.string.mobile_sleep_timer_end_of_video);
        }
        int minutes = getRemainingMinutes();
        if (minutes < 60) {
            return context.getString(R.string.mobile_sleep_timer_remaining_min, Math.max(1, minutes));
        }
        int hours = minutes / 60;
        int remMin = minutes % 60;
        return context.getString(R.string.mobile_sleep_timer_remaining_hr_min, hours, remMin);
    }

    /**
     * Formatted status string for the bottom sheet header banner.
     */
    @NonNull
    public synchronized String getFormattedStatus(@NonNull Context context) {
        if (isEndOfVideo()) {
            return context.getString(R.string.mobile_sleep_timer_status_end_of_video);
        }
        int minutes = getRemainingMinutes();
        String timeStr;
        if (minutes < 60) {
            timeStr = context.getResources().getQuantityString(
                    R.plurals.mobile_settings_minutes, Math.max(1, minutes), Math.max(1, minutes));
        } else if (minutes % 60 == 0) {
            int hours = minutes / 60;
            timeStr = context.getResources().getQuantityString(
                    R.plurals.mobile_settings_hours, hours, hours);
        } else {
            timeStr = context.getString(R.string.mobile_settings_hours_minutes, minutes / 60, minutes % 60);
        }
        return context.getString(R.string.mobile_sleep_timer_status_running, timeStr);
    }
}
