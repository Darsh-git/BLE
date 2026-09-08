package com.example.bleprototype;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class PacketRepository {
    private final DatabaseHelper dbHelper;

    public PacketRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public synchronized boolean hasPacket(int packetId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT 1 FROM " + DatabaseHelper.TABLE_PACKETS +
                " WHERE " + DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(packetId)});
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) cursor.close();
        return exists;
    }

    public synchronized void savePacket(int packetId, byte type, float lat, float lon, byte ttl) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ID, packetId);
        values.put(DatabaseHelper.COLUMN_TYPE, type);
        values.put(DatabaseHelper.COLUMN_LAT, lat);
        values.put(DatabaseHelper.COLUMN_LON, lon);
        values.put(DatabaseHelper.COLUMN_TTL, ttl);
        values.put(DatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis());

        db.insertWithOnConflict(DatabaseHelper.TABLE_PACKETS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
}