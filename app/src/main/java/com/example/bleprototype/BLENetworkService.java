package com.example.bleprototype;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class BLENetworkService extends Service implements BLEManager.BLEListener, RelayManager.RelayLogListener {

    private static final String CHANNEL_ID = "BLEMeshChannel";
    private static final int NOTIFICATION_ID = 1001;

    private final IBinder binder = new LocalBinder();
    private BLEManager bleManager;
    private RelayManager relayManager;
    private ServiceLogCallback uiCallback;

    public interface ServiceLogCallback {
        void onLog(String message);
    }

    public class LocalBinder extends Binder {
        public BLENetworkService getService() {
            return BLENetworkService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BLE Emergency Mesh Active")
                .setContentText("Listening and relaying emergency signals...")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        bleManager = new BLEManager(this, this);
        relayManager = new RelayManager(this, bleManager, this);

        bleManager.startScanning();
    }

    public void setUiCallback(ServiceLogCallback callback) {
        this.uiCallback = callback;
    }

    public void sendReport(short seq, byte type, float lat, float lon) {
        if (relayManager != null) {
            relayManager.createAndBroadcastReport(seq, type, lat, lon);
        }
    }

    @Override
    public void onPacketReceived(byte[] rawData, int rssi) {
        if (relayManager != null) {
            relayManager.processIncomingData(rawData, rssi);
        }
    }

    @Override
    public void onLog(String message) {
        onLogMessage(message);
    }

    @Override
    public void onLogMessage(String msg) {
        if (uiCallback != null) {
            uiCallback.onLog(msg);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "BLE Mesh Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}