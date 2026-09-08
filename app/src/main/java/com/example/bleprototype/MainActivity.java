package com.example.bleprototype;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements BLENetworkService.ServiceLogCallback {

private static final int PERMISSION_REQUEST_CODE = 101;
private TextView logView;
private BLENetworkService bleService;
private boolean isBound = false;
private short localSequence = 0;
private FusedLocationProviderClient fusedLocationClient;

private final ServiceConnection serviceConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        BLENetworkService.LocalBinder binder = (BLENetworkService.LocalBinder) service;
        bleService = binder.getService();
        bleService.setUiCallback(MainActivity.this);
        isBound = true;
        onLog("Connected to BLE Mesh Foreground Service.");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        isBound = false;
        bleService = null;
    }
};

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    logView = findViewById(R.id.logView);
    Button btnStartAdvertising = findViewById(R.id.btn_start_advertising);
    Button btnSendPacket = findViewById(R.id.btn_send_packet);

    checkAndRequestPermissions();

    Intent intent = new Intent(this, BLENetworkService.class);
    startService(intent);
    bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

    btnStartAdvertising.setOnClickListener(v -> sendReportWithLocation((byte) 1));
    btnSendPacket.setOnClickListener(v -> sendReportWithLocation((byte) 9));
}

private void sendReportWithLocation(byte type) {
    if (!isBound || bleService == null) return;

    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        onLog("Location permission missing. Sending default coordinates.");
        localSequence++;
        bleService.sendReport(localSequence, type, 0.0f, 0.0f);
        return;
    }

    fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
        localSequence++;
        if (location != null) {
            float lat = (float) location.getLatitude();
            float lon = (float) location.getLongitude();
            onLog("Broadcasting alert with live GPS: " + lat + ", " + lon);
            bleService.sendReport(localSequence, type, lat, lon);
        } else {
            onLog("GPS location unavailable. Sending default coordinates (0.0, 0.0).");
            bleService.sendReport(localSequence, type, 0.0f, 0.0f);
        }
    });
}

private void checkAndRequestPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    } else {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, PERMISSION_REQUEST_CODE);
        }
    }
}

@Override
public void onLog(String message) {
    runOnUiThread(() -> logView.append("\n" + message));
}

@Override
protected void onDestroy() {
    super.onDestroy();
    if (isBound) {
        unbindService(serviceConnection);
        isBound = false;
    }
}
}