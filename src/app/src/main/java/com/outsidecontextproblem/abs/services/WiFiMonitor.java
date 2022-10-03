package com.outsidecontextproblem.abs.services;

import static android.Manifest.permission_group.LOCATION;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

public class WiFiMonitor extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        new Thread(
            () -> {
                int c = 1;

                while (true) {

                    Log.e("Service", String.format("Polling %d...", c++));

                    WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
                    SupplicantState supplicantState = wifiInfo.getSupplicantState();
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

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
