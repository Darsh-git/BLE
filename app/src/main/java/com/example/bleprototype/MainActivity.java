package com.example.bleprototype;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BLEPrototype";
    private static final UUID SERVICE_UUID = UUID.fromString("8f8b7d9c-4c2c-4704-b09b-4c7b1b6e82d7");
    private static final UUID PACKET_UUID = UUID.fromString("b178c1d0-9bf7-4bc0-b202-5ba6f7d3f6d1");
    private static final int MANUFACTURER_ID = 0xFFFF;

    private static final int REQUEST_CODE = 1001;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;
    private TextView logView;
    private boolean isAdvertising = false;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logView = findViewById(R.id.logView);

        Button startAdvertising = findViewById(R.id.btn_start_advertising);
        Button startScanning = findViewById(R.id.btn_start_scanning);
        Button sendPacket = findViewById(R.id.btn_send_packet);

        startAdvertising.setOnClickListener(v -> startAdvertising());
        startScanning.setOnClickListener(v -> startScanning());
        sendPacket.setOnClickListener(v -> sendPacket());

        setupBluetooth();
    }

    private void setupBluetooth() {
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            bluetoothAdapter = manager.getAdapter();
        }

        if (bluetoothAdapter == null) {
            log("Bluetooth not available on this device.");
            return;
        }

        requestNeededPermissions();
    }

    private void requestNeededPermissions() {
        List<String> permissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_CODE);
                return;
            }
        }
    }

    private void startAdvertising() {
        if (!isBluetoothReady()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            log("Missing BLUETOOTH_ADVERTISE permission.");
            return;
        }

        if (isAdvertising) {
            log("Advertising already started.");
            return;
        }

        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            log("BLE advertising not supported on this device.");
            return;
        }

        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            log("Could not get BluetoothLeAdvertiser.");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                .addManufacturerData(MANUFACTURER_ID, buildTestPacket().getBytes(StandardCharsets.UTF_8))
                .build();

        advertiser.startAdvertising(settings, data, advertiseCallback);
        isAdvertising = true;
        log("Started BLE advertising for service " + SERVICE_UUID);
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            isAdvertising = true;
            log("Advertising started successfully.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            isAdvertising = false;
            log("Advertising failed. Error code: " + errorCode);
        }
    };

    private void startScanning() {
        if (!isBluetoothReady()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            log("Missing BLUETOOTH_SCAN permission.");
            return;
        }

        if (isScanning) {
            log("Scanning already started.");
            return;
        }

        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            log("Could not get BluetoothLeScanner.");
            return;
        }

        ScanFilter filter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);

        scanner.startScan(filters, settings, scanCallback);
        isScanning = true;
        log("Started BLE scanning for service " + SERVICE_UUID);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            ScanRecord record = result.getScanRecord();
            if (device == null) {
                return;
            }
            String msg = "Found device: " + device.getAddress();
            if (record != null) {
                byte[] data = record.getManufacturerSpecificData(MANUFACTURER_ID);
                if (data != null && data.length > 0) {
                    msg += " | payload=" + new String(data, StandardCharsets.UTF_8);
                } else {
                    log("Ignoring device without packet service data: " + device.getAddress());
                    return;
                }
            } else {
                log("Ignoring device without scan record: " + device.getAddress());
                return;
            }
            log(msg);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            isScanning = false;
            if (errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED) {
                log("Scan failed: already started. Stopping and restarting scan.");
                if (scanner != null) {
                    scanner.stopScan(scanCallback);
                }
            } else {
                log("Scan failed: " + errorCode);
            }
        }
    };

    private void sendPacket() {
        if (!isBluetoothReady()) {
            return;
        }

        log("Sending test packet: " + buildTestPacket());
        if (!isAdvertising) {
            startAdvertising();
        }
    }

    private String buildTestPacket() {
        return "A12345|MEDICAL|TTL=5";
    }

    private boolean isBluetoothReady() {
        if (bluetoothAdapter == null) {
            log("Bluetooth adapter is null.");
            return false;
        }
        if (!bluetoothAdapter.isEnabled()) {
            log("Bluetooth is disabled. Please enable Bluetooth.");
            Toast.makeText(this, "Enable Bluetooth first.", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            log("Missing BLUETOOTH_CONNECT permission.");
            return false;
        }
        return true;
    }

    private void log(String message) {
        Log.d(TAG, message);
        runOnUiThread(() -> {
            String existing = logView.getText() == null ? "" : logView.getText().toString();
            logView.setText(existing + "\n" + message);
        });
    }
}
