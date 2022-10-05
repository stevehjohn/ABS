package com.outsidecontextproblem.abs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.TextView;

import com.outsidecontextproblem.abs.services.WiFiMonitor;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final int FINE_LOCATION_REQUEST = 1;
    private final int BACKGROUND_LOCATION_REQUEST = 2;

    public static final int MESSAGE_WIFI_SSID = 1;
    public static final int MESSAGE_WIFI_HOTSPOTS = 2;

    private Messenger _serviceMessenger;
    private final Messenger _incomingMessenger = new Messenger(new IncomingHandler(this));
    private boolean _bound;

    private static class IncomingHandler extends Handler {
        private final MainActivity _mainActivity;

        public IncomingHandler(MainActivity mainActivity) {
            _mainActivity = mainActivity;
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_WIFI_SSID:
                    _mainActivity.updateConnectedWiFi(message.getData().getString(WiFiMonitor.MESSAGE_KEY_WIFI_NAME));

                    break;
                case MESSAGE_WIFI_HOTSPOTS:
                    ArrayList<String> hotspots = message.getData().getStringArrayList(WiFiMonitor.MESSAGE_KEY_WIFI_HOTSPOTS);

                    _mainActivity.showHotspots(hotspots);

                    break;
                default:
                    super.handleMessage(message);
            }
        }
    }

    private final ServiceConnection _connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            _serviceMessenger = new Messenger(service);
            _bound = true;

            Message message = Message.obtain(null, WiFiMonitor.MESSAGE_REGISTER_CLIENT);
            message.replyTo = _incomingMessenger;

            try {
                _serviceMessenger.send(message);
            }
            catch (RemoteException exception) {
                exception.printStackTrace();
            }

            requestConfiguredHotspots();
        }

        public void onServiceDisconnected(ComponentName className) {
            _serviceMessenger = null;
            _bound = false;
        }
    };

    @Override
    protected void onStart() {
        super.onStart();

        bindService(new Intent(this, WiFiMonitor.class), _connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (_bound) {
            unbindService(_connection);
            _bound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (! serviceIsRunning()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]
                        {
                                Manifest.permission.ACCESS_FINE_LOCATION
                        }, FINE_LOCATION_REQUEST);
            } else {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]
                            {
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            }, BACKGROUND_LOCATION_REQUEST);
                }
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                Intent serviceIntent = new Intent(this, WiFiMonitor.class);
                startForegroundService(serviceIntent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case FINE_LOCATION_REQUEST:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finishAndRemoveTask();

                    return;
                }

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]
                            {
                                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                            }, BACKGROUND_LOCATION_REQUEST);
                }

                return;

            case BACKGROUND_LOCATION_REQUEST:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    finishAndRemoveTask();

                    return;
                }

                Intent serviceIntent = new Intent(this, WiFiMonitor.class);
                startForegroundService(serviceIntent);
        }
    }

    private void requestConfiguredHotspots() {
        Message message = Message.obtain(null, WiFiMonitor.MESSAGE_GET_WIFI_ONLY_HOTSPOTS);

        try {
            _serviceMessenger.send(message);
        }
        catch (RemoteException exception) {
            exception.printStackTrace();
        }
    }

    private void showHotspots(ArrayList<String> hotspots) {
        for (String hotspot: hotspots) {
            Log.i(MainActivity.class.getName(), hotspot);
        }
    }

    private void updateConnectedWiFi(String wiFiName) {
        TextView textView = findViewById(R.id.textViewWiFiName);
        textView.setText(wiFiName);
    }

    private boolean serviceIsRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        for (ActivityManager.RunningServiceInfo service: activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (WiFiMonitor.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }
}