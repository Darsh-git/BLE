package com.example.bleprototype;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.*;
import android.content.Context;
import android.os.ParcelUuid;

import java.util.Collections;

public class BLEManager {
    public static final ParcelUuid SERVICE_UUID = ParcelUuid.fromString("0000180D-0000-1000-8000-00805f9b34fb");

    public interface BLEListener {
        void onPacketReceived(byte[] rawData, int rssi);
        void onLog(String message);
    }

    private final Context context;
    private final BLEListener listener;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser advertiser;
    private BluetoothLeScanner scanner;

    private boolean isAdvertising = false;
    private boolean isScanning = false;

    public BLEManager(Context context, BLEListener listener) {
        this.context = context;
        this.listener = listener;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            this.bluetoothAdapter = bluetoothManager.getAdapter();
        }
    }

    public void startAdvertising(byte[] payload) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            listener.onLog("BLE Error: Bluetooth disabled.");
            return;
        }
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            listener.onLog("BLE Error: Advertiser unsupported.");
            return;
        }
        if (isAdvertising) stopAdvertising();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceData(SERVICE_UUID, payload)
                .build();

        try {
            advertiser.startAdvertising(settings, data, advertiseCallback);
        } catch (SecurityException e) {
            listener.onLog("Security Exception on startAdvertising: " + e.getMessage());
        }
    }

    public void stopAdvertising() {
        if (advertiser != null && isAdvertising) {
            try {
                advertiser.stopAdvertising(advertiseCallback);
                isAdvertising = false;
                listener.onLog("BLE: Stopped advertising.");
            } catch (SecurityException e) {
                listener.onLog("Security Exception on stopAdvertising.");
            }
        }
    }

    public void startScanning() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) return;
        scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null || isScanning) return;

        ScanFilter filter = new ScanFilter.Builder().setServiceData(SERVICE_UUID, null).build();
        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();

        try {
            scanner.startScan(Collections.singletonList(filter), settings, scanCallback);
            isScanning = true;
            listener.onLog("BLE: Active continuous scan started.");
        } catch (SecurityException e) {
            listener.onLog("Security Exception on startScanning.");
        }
    }

    public void stopScanning() {
        if (scanner != null && isScanning) {
            try {
                scanner.stopScan(scanCallback);
                isScanning = false;
                listener.onLog("BLE: Scan stopped.");
            } catch (SecurityException e) {
                listener.onLog("Security Exception on stopScanning.");
            }
        }
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            isAdvertising = true;
            listener.onLog("BLE: Broadcast payload active on mesh.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            isAdvertising = false;
            listener.onLog("BLE Advertising failed, code: " + errorCode);
        }
    };

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getScanRecord() != null) {
                byte[] rawData = result.getScanRecord().getServiceData(SERVICE_UUID);
                if (rawData != null) {
                    listener.onPacketReceived(rawData, result.getRssi());
                }
            }
        }
    };
}