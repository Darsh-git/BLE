package com.example.bleprototype.network;

import com.example.bleprototype.model.EmergencyPacket;

import java.util.Locale;
import java.util.UUID;

public class PacketManager {
    public EmergencyPacket decodePacket(String rawPayload) {
        if (rawPayload == null || rawPayload.isEmpty()) {
            throw new IllegalArgumentException("Packet payload is empty");
        }

        String[] parts = rawPayload.split("\\|");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid packet payload: " + rawPayload);
        }

        String packetId = parts[0];
        String type = parts[1];
        if (packetId.trim().isEmpty() || type.trim().isEmpty() || !parts[2].startsWith("TTL=")) {
            throw new IllegalArgumentException("Invalid packet fields: " + rawPayload);
        }

        int ttl;
        try {
            ttl = Integer.parseInt(parts[2].substring(4));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid TTL in packet: " + rawPayload, exception);
        }

        return new EmergencyPacket(packetId, type, ttl, System.currentTimeMillis(), 0.0, 0.0, "unknown");
    }

    public String encodePacket(EmergencyPacket packet) {
        return String.format(Locale.US, "%s|%s|TTL=%d", packet.getPacketId(), packet.getType(), packet.getTtl());
    }

    public String generatePacketId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.US);
    }

    public boolean validatePacket(EmergencyPacket packet) {
        return packet != null && packet.getPacketId() != null && !packet.getPacketId().trim().isEmpty()
                && packet.getType() != null && !packet.getType().trim().isEmpty();
    }
}
