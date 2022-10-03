package com.outsidecontextproblem.abs.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.outsidecontextproblem.abs.R;

public class WiFiMonitor extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new Thread(
            () -> {
                int c = 1;

                while (true) {

                    Log.e("Service", String.format("Polling %d...", c++));

                    WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                    String name = wifiInfo.getSSID();

                    Log.e("Service", name);

                    try {
                        Thread.sleep(2000);
                        // Thread.sleep(60000);
                    }
                    catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        ).start();

        final String CHANNEL_ID = "Foreground Service Id";

        NotificationChannel notificationChannel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
        );

        getSystemService((NotificationManager.class)).createNotificationChannel(notificationChannel);

        Notification.Builder notificationBuilder = new Notification.Builder(this, CHANNEL_ID)
                .setContentText("Blah")
                .setContentTitle("Blah")
                .setSmallIcon(R.drawable.ic_launcher_background);

        startForeground(1001, notificationBuilder.build());

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
