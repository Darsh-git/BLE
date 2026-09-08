package com.example.bleprototype;

import android.content.Context;

public class RelayManager {
    public interface RelayLogListener {
        void onLogMessage(String msg);
    }

    private final Context context;
    private final PacketRepository repository;
    private final BLEManager bleManager;
    private final RelayLogListener logListener;

    public RelayManager(Context context, BLEManager bleManager, RelayLogListener logListener) {
        this.context = context;
        this.repository = new PacketRepository(context);
        this.bleManager = bleManager;
        this.logListener = logListener;
    }

    public void processIncomingData(byte[] rawData, int rssi) {
        PacketSerializer.ParsedPacket packet = PacketSerializer.parsePacket(rawData);
        if (packet == null) return;

        if (repository.hasPacket(packet.packetId)) {
            return;
        }

        repository.savePacket(packet.packetId, packet.type, packet.latitude, packet.longitude, packet.ttl);

        logListener.onLogMessage("RECEIVED NEW PACKET! ID: " + packet.packetId + " | TTL: " + packet.ttl + " | RSSI: " + rssi);

        if (packet.ttl > 1) {
            byte newTtl = (byte) (packet.ttl - 1);
            logListener.onLogMessage("RELAYING PACKET ID " + packet.packetId + " with TTL " + newTtl);
            byte[] relayData = PacketSerializer.createPacket(
                    packet.packetId, newTtl, packet.type, packet.latitude, packet.longitude);
            bleManager.startAdvertising(relayData);
        } else {
            logListener.onLogMessage("TTL expired for ID " + packet.packetId + ". Relay terminated.");
        }
    }

    public void createAndBroadcastReport(short sequence, byte type, float lat, float lon) {
        int packetId = PacketSerializer.generateUniquePacketId(context, sequence);
        byte initialTtl = 3;

        repository.savePacket(packetId, type, lat, lon, initialTtl);
        logListener.onLogMessage("CREATED NEW REPORT -> ID: " + packetId + " | Type: " + type);

        byte[] payload = PacketSerializer.createPacket(packetId, initialTtl, type, lat, lon);
        bleManager.startAdvertising(payload);
    }
}