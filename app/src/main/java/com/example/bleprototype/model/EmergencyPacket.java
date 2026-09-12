package com.example.bleprototype.model;

import java.util.Locale;
import java.util.UUID;

public class EmergencyPacket {
    private final String packetId;
    private final String type;
    private final String severity;
    private final String location;
    private final int ttl;
    private final long createdAt;
    private final long receivedAt;
    private final String sourceDevice;
    private final int relayCount;
    private final String status;
    private final double latitude;
    private final double longitude;

    public EmergencyPacket(String packetId, String type, int ttl, long timestamp,
                          double latitude, double longitude, String sourceAddress) {
        this(packetId, type, "MEDIUM", formatLocation(latitude, longitude), timestamp, 0L,
                ttl, sourceAddress, 0, "PENDING", latitude, longitude);
    }

    public EmergencyPacket(String packetId, String type, String severity, String location,
                           long createdAt, long receivedAt, int ttl, String sourceDevice,
                           int relayCount, String status) {
        this(packetId, type, severity, location, createdAt, receivedAt, ttl, sourceDevice,
                relayCount, status, 0.0, 0.0);
    }

    private EmergencyPacket(String packetId, String type, String severity, String location,
                            long createdAt, long receivedAt, int ttl, String sourceDevice,
                            int relayCount, String status, double latitude, double longitude) {
        this.packetId = packetId;
        this.type = type;
        this.severity = severity;
        this.location = location;
        this.ttl = ttl;
        this.createdAt = createdAt;
        this.receivedAt = receivedAt;
        this.sourceDevice = sourceDevice;
        this.relayCount = relayCount;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getPacketId() {
        return packetId;
    }

    public String getType() {
        return type;
    }

    public String getSeverity() {
        return severity;
    }

    public String getLocation() {
        return location;
    }

    public int getTtl() {
        return ttl;
    }

    public long getTimestamp() {
        return createdAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getReceivedAt() {
        return receivedAt;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getSourceAddress() {
        return sourceDevice;
    }

    public String getSourceDevice() {
        return sourceDevice;
    }

    public int getRelayCount() {
        return relayCount;
    }

    public String getStatus() {
        return status;
    }

    public EmergencyPacket withDecrementedTtl() {
        return new EmergencyPacket(packetId, type, severity, location, createdAt, receivedAt,
                Math.max(0, ttl - 1), sourceDevice, relayCount, status, latitude, longitude);
    }

    private static String formatLocation(double latitude, double longitude) {
        return String.format(Locale.US, "%.5f,%.5f", latitude, longitude);
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "PacketId=%s, Type=%s, TTL=%d, Lat=%.5f, Lng=%.5f, Source=%s",
                packetId, type, ttl, latitude, longitude, sourceDevice);
    }
}
