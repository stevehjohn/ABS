package com.outsidecontextproblem.abs.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.outsidecontextproblem.abs.R;

public class WiFiMonitor extends Service {

    public static final int MESSAGE_REGISTER_CLIENT = 1;

    private Messenger _client = null;

    private static class IncomingHandler extends Handler {
        private final WiFiMonitor _wiFiMonitor;

        IncomingHandler(WiFiMonitor wiFiMonitor) {
            _wiFiMonitor = wiFiMonitor;
        }

        @Override
        public void handleMessage(Message message) {
            // noinspection SwitchStatementWithTooFewBranches - Will be adding more
            switch (message.what) {
                case MESSAGE_REGISTER_CLIENT:
                    _wiFiMonitor._client = message.replyTo;

                    break;
                default:
                    super.handleMessage(message);
            }
        }
    }

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

                    String currentWifi = String.format("WiFi: %s Poll Count: %d", name, c);

                    Message message = Message.obtain(null, 123);
                    Bundle bundle = new Bundle();
                    bundle.putString("WIFI", currentWifi);
                    message.setData(bundle);

                    try {
                        if (_client != null) {
                            _client.send(message);
                        }
                    }
                    catch (RemoteException exception) {
                        exception.printStackTrace();
                    }

                    try {
                        // TODO: Different mechanism, ScheduledExecutorService or such?
                        Thread.sleep(2000);
                        // Thread.sleep(60000);
                    }
                    catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        ).start();

        final String CHANNEL_ID = "com.outsidecontextproblem.abs";

        NotificationChannel notificationChannel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_ID,
                NotificationManager.IMPORTANCE_LOW
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
        Messenger _messenger = new Messenger(new IncomingHandler(this));

        return _messenger.getBinder();
    }
}
