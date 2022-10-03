package com.outsidecontextproblem.abs;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.outsidecontextproblem.abs.services.WiFiMonitor;

public class MainActivity extends AppCompatActivity {

    private final int FINE_LOCATION_REQUEST = 1;
    private final int BACKGROUND_LOCATION_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]
                    {
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, FINE_LOCATION_REQUEST);
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
}