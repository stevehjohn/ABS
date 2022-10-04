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
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.TextView;

import com.outsidecontextproblem.abs.services.WiFiMonitor;

public class MainActivity extends AppCompatActivity {

    private final int FINE_LOCATION_REQUEST = 1;
    private final int BACKGROUND_LOCATION_REQUEST = 2;

    private Messenger _serviceMessenger;
    private Messenger _incomingMessenger = new Messenger(new IncomingHandler());
    private boolean _bound;

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 123:
                    updateConnectedWiFi(message.getData().getString("WIFI"));

                    break;
                default:
                    super.handleMessage(message);
            }
        }
    }
    private ServiceConnection _connection = new ServiceConnection() {
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
        }

        public void onServiceDisconnected(ComponentName className) {
            _serviceMessenger = null;
            _bound = false;
        }
    };

    private CountDownTimer _timer;

    @Override
    protected void onStart() {
        super.onStart();

        bindService(new Intent(this, WiFiMonitor.class), _connection, Context.BIND_AUTO_CREATE);

//        _timer = new CountDownTimer(Long.MAX_VALUE, 2000) {
//            @Override
//            public void onTick(long l) {
//                if (! _bound) {
//                    return;
//                }
//
//                Message message = Message.obtain(null, WiFiMonitor.MESSAGE_GET_CURRENT_WIFI, 0, 0);
//
//                try {
//                    _messenger.send(message);
//                }
//                catch (RemoteException exception) {
//                    exception.printStackTrace();
//                }
//            }
//
//            @Override
//            public void onFinish() {
//
//            }
//        }.start();
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