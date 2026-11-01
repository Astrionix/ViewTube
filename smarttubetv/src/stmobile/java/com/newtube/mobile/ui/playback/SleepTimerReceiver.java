package com.newtube.mobile.ui.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BroadcastReceiver triggered by AlarmManager when the sleep timer duration expires.
 */
public class SleepTimerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SleepTimer.instance().onTimerExpired(context);
    }
}
