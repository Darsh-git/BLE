package com.example.bleprototype;

import android.content.Context;
import android.provider.Settings;
import java.nio.ByteBuffer;

public class PacketSerializer {
    public static final int PACKET_SIZE = 14;

    public static class ParsedPacket {
        public final int packetId;
        public final byte ttl;
        public final byte type;
        public final float latitude;
        public final float longitude;

        public ParsedPacket(int packetId, byte ttl, byte type, float latitude, float longitude) {
            this.packetId = packetId;
            this.ttl = ttl;
            this.type = type;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public static int generateUniquePacketId(Context context, short sequence) {
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        int deviceHash = (androidId != null) ? (androidId.hashCode() & 0xFFFF) : (int) (Math.random() * 0xFFFF);
        return (deviceHash << 16) | (sequence & 0xFFFF);
    }

    public static byte[] createPacket(int packetId, byte ttl, byte type, float lat, float lon) {
        ByteBuffer buffer = ByteBuffer.allocate(PACKET_SIZE);
        buffer.putInt(packetId);
        buffer.put(ttl);
        buffer.put(type);
        buffer.putFloat(lat);
        buffer.putFloat(lon);
        return buffer.array();
    }

    public static ParsedPacket parsePacket(byte[] rawData) {
        if (rawData == null || rawData.length < PACKET_SIZE) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(rawData);
        int packetId = buffer.getInt();
        byte ttl = buffer.get();
        byte type = buffer.get();
        float lat = buffer.getFloat();
        float lon = buffer.getFloat();

        return new ParsedPacket(packetId, ttl, type, lat, lon);
    }
}