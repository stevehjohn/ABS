package com.outsidecontextproblem.abs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.outsidecontextproblem.abs.adapters.HotSpotAdapter;
import com.outsidecontextproblem.abs.helpers.SwipeToDeleteCallback;
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

    private final ArrayList<String> _hotspots = new ArrayList<>();
    private HotSpotAdapter _hotSpotAdapter;

    private String _currentHotSpot;

    private RecyclerView _recyclerView;
    private Button _addButton;

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

        _recyclerView = findViewById(R.id.recyclerHotspots);
        _recyclerView.setLayoutManager(new LinearLayoutManager(this));
        _hotSpotAdapter = new HotSpotAdapter(_hotspots, this);
        _recyclerView.setAdapter(_hotSpotAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(_hotSpotAdapter));
        itemTouchHelper.attachToRecyclerView(_recyclerView);

        _addButton = findViewById(R.id.buttonAdd);
        _addButton.setOnClickListener(view -> AddCurrentWifiHotSpot());
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

            _hotspots.add(hotspot);
        }

        _hotSpotAdapter.notifyDataSetChanged();
    }

    private void updateConnectedWiFi(String wiFiName) {
        TextView textView = findViewById(R.id.textViewWiFiName);

        if (wiFiName == null) {
            textView.setText(R.string.ui_main_searching);
            _addButton.setEnabled(false);
            return;
        }

        _addButton.setEnabled(true);

        textView.setText(wiFiName);

        _currentHotSpot = wiFiName;
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

    private void AddCurrentWifiHotSpot() {
        if (_hotspots.contains(_currentHotSpot)) {
            Snackbar snackbar = Snackbar.make(_recyclerView, String.format(getResources().getString(R.string.hotspot_exists), _currentHotSpot), Snackbar.LENGTH_LONG);
            snackbar.setAnimationMode(Snackbar.ANIMATION_MODE_FADE);
            snackbar.show();

            _recyclerView.scrollToPosition(_hotspots.indexOf(_currentHotSpot));

            return;
        }

        _hotspots.add(0, _currentHotSpot);

        _hotSpotAdapter.notifyItemInserted(0);

        _recyclerView.scrollToPosition(0);
    }
}