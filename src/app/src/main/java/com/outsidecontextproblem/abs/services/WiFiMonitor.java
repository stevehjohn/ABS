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
import android.provider.Settings;
import android.support.v4.os.IResultReceiver;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.Nullable;

import com.outsidecontextproblem.abs.MainActivity;
import com.outsidecontextproblem.abs.R;

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

public class WiFiMonitor extends Service {

    public static final int MESSAGE_REGISTER_CLIENT = 1;
    public static final int MESSAGE_ADD_WIFI_ONLY_HOTSPOT = 2;
    public static final int MESSAGE_REMOVE_WIFI_ONLY_HOTSPOT = 3;
    public static final int MESSAGE_GET_WIFI_ONLY_HOTSPOTS = 4;
    public static final int MESSAGE_GET_WIFI_ONLY_STATE = 5;
    public static final int MESSAGE_SET_WIFI_ONLY_STATE = 6;

    public static final String MESSAGE_KEY_WIFI_NAME = "WIFI";
    public static final String MESSAGE_KEY_WIFI_HOTSPOTS = "WIFI_HOTSPOTS";
    public static final String MESSAGE_KEY_WIFI_ONLY_STATE = "WIFI_ONLY_STATE";

    private static final String NOTIFICATION_CHANNEL_ID = "com.outsidecontextproblem.abs";

    private static final int POLL_MILLISECONDS = 2_000;

    private static final int NOTIFICATION_ID = 824954302;

    private NotificationManager _notificationManager;

    private Notification.Builder _notificationBuilder;

    private Messenger _client = null;

    private int _pollCount = 0;

    private final ArrayList<String> _wiFiSSIDs = new ArrayList<>();

    private boolean _wifiCalling = false;

    private static class IncomingHandler extends Handler {
        private final WiFiMonitor _wiFiMonitor;

        IncomingHandler(WiFiMonitor wiFiMonitor) {
            _wiFiMonitor = wiFiMonitor;
        }

        @Override
        public void handleMessage(Message message) {
            String ssid;
            boolean state;

            switch (message.what) {
                case MESSAGE_REGISTER_CLIENT:
                    _wiFiMonitor._client = message.replyTo;

                    _wiFiMonitor.doPoll();

                    break;
                case MESSAGE_ADD_WIFI_ONLY_HOTSPOT:
                    ssid = message.getData().getString(MESSAGE_KEY_WIFI_NAME);

                    if (! _wiFiMonitor._wiFiSSIDs.contains(ssid)) {
                        _wiFiMonitor._wiFiSSIDs.add(ssid);

                        // TODO: Save wiFiSSIDs
                    }

                    break;
                case MESSAGE_REMOVE_WIFI_ONLY_HOTSPOT:
                    ssid = message.getData().getString(MESSAGE_KEY_WIFI_NAME);

                    if (_wiFiMonitor._wiFiSSIDs.contains(ssid)) {
                        _wiFiMonitor._wiFiSSIDs.remove(ssid);

                        // TODO: Save wiFiSSIDs
                    }

                    break;
                case MESSAGE_GET_WIFI_ONLY_HOTSPOTS:
                    _wiFiMonitor.returnHotspots();

                    break;
                case MESSAGE_GET_WIFI_ONLY_STATE:
                    // TODO: Do

                    break;
                case MESSAGE_SET_WIFI_ONLY_STATE:
                    state = message.getData().getBoolean(MESSAGE_KEY_WIFI_ONLY_STATE);

                    _wiFiMonitor.setWiFiOnlyState(state);

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

        _notificationManager = getSystemService((NotificationManager.class));

        _notificationManager.createNotificationChannel(notificationChannel);

        _notificationBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentText(getResources().getString(R.string.notification_monitoring))
                .setSmallIcon(R.drawable.ic_launcher_background);

        startForeground(NOTIFICATION_ID, _notificationBuilder.build());

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

    private void setWiFiOnlyState(boolean state) {
        Log.i(WiFiMonitor.class.getName(), String.format("Request to change WiFi state: %b", state));

        if (state) {
            switchToWiFiCalling();
        } else {
            restoreDefaultRadios();
        }
    }

    private void returnHotspots() {
        Message message = Message.obtain(null, MainActivity.MESSAGE_WIFI_HOTSPOTS);
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(MESSAGE_KEY_WIFI_HOTSPOTS, _wiFiSSIDs);
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

    private void doPoll() {
        _pollCount++;

        Log.i(WiFiMonitor.class.getName(), String.format("Polling %d...", _pollCount));

        WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        String name = wifiInfo.getSSID();

        if (name != null) {
            Log.i(WiFiMonitor.class.getName(), name);

            if (name.startsWith("<")) {
                name = null;
            } else if (name.startsWith("\"")) {
                name = name.substring(1, name.length() - 1);
            }
        } else {
            Log.i(WiFiMonitor.class.getName(), "null");
        }

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

        if (! _wifiCalling) {
            if (_wiFiSSIDs.contains(name)) {
                switchToWiFiCalling();
            }
        } else {
            if (! _wiFiSSIDs.contains(name)) {
                restoreDefaultRadios();
            }
        }
    }

    // Here, we fail :(
    // Cannot disable cellular from a non-system app.
    private void switchToWiFiCalling() {
        _notificationBuilder.setContentText(getResources().getString(R.string.notification_wifi_only));

        _notificationManager.notify(NOTIFICATION_ID, _notificationBuilder.build());

        _wifiCalling = true;
    }

    private void restoreDefaultRadios() {
        _notificationBuilder.setContentText(getResources().getString(R.string.notification_monitoring));

        _notificationManager.notify(NOTIFICATION_ID, _notificationBuilder.build());

        _wifiCalling = false;
    }
}
