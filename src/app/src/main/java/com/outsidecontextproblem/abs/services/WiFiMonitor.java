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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

import com.outsidecontextproblem.abs.MainActivity;
import com.outsidecontextproblem.abs.R;

public class WiFiMonitor extends Service {

    public static final int MESSAGE_REGISTER_CLIENT = 1;

    public static final String MESSAGE_KEY_WIFI_NAME = "WIFI";

    private static final String NOTIFICATION_CHANNEL_ID = "com.outsidecontextproblem.abs";

    private static final int POLL_MILLISECONDS = 2_000;

    private static final int NOTIFICATION_ID = 824954302;

    private Messenger _client = null;

    private int _pollCount = 0;

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
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationChannel notificationChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_ID,
                NotificationManager.IMPORTANCE_LOW
        );

        getSystemService((NotificationManager.class)).createNotificationChannel(notificationChannel);

        Notification.Builder notificationBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentText("Monitoring for WiFi calling designated access points.")
                .setSmallIcon(R.drawable.ic_launcher_background);

        startForeground(NOTIFICATION_ID, notificationBuilder.build());

        new CountDownTimer(Long.MAX_VALUE, POLL_MILLISECONDS) {
            @Override
            public void onTick(long l) {
                doPoll();
            }

            @Override
            public void onFinish() {
            }
        }.start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Messenger _messenger = new Messenger(new IncomingHandler(this));

        return _messenger.getBinder();
    }

    private void doPoll() {
        _pollCount++;

        Log.i(WiFiMonitor.class.getName(), String.format("Polling %d...", _pollCount));

        WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        String name = wifiInfo.getSSID();

        name = name.substring(1, name.length() - 1);

        Log.i(WiFiMonitor.class.getName(), name);

        Message message = Message.obtain(null, MainActivity.MESSAGE_WIFI_SSID);
        Bundle bundle = new Bundle();
        bundle.putString(MESSAGE_KEY_WIFI_NAME, name);
        message.setData(bundle);

        try {
            if (_client != null) {
                _client.send(message);
            }
        }
        catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }
}
