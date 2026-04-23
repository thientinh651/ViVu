package com.tinh.vivu.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.tinh.vivu.utils.AlarmScheduler;

public class AlarmRescheduleReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmRescheduleRcvr";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent != null ? intent.getAction() : "unknown";
        Log.d(TAG, "Rescheduling alarms after: " + action);
        AlarmScheduler.rescheduleActiveAlarms(context);
    }
}
