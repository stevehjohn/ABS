package com.outsidecontextproblem.abs.services;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WiFiMonitorBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (serviceIsRunning(context)) {
            return;
        }

        Intent serviceIntent = new Intent(context, WiFiMonitor.class);
        context.startForegroundService(serviceIntent);
    }

    private boolean serviceIsRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (WiFiMonitor.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
}
