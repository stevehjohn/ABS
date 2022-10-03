package com.outsidecontextproblem.abs.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class WiFiMonitorBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: Don't start if already running.
        Intent serviceIntent = new Intent(context, WiFiMonitor.class);
        context.startForegroundService(serviceIntent);
    }
}
