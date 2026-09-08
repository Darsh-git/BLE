package com.example.bleprototype;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ble_emergency.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_PACKETS = "emergency_packets";
    public static final String COLUMN_ID = "packet_id";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_LAT = "latitude";
    public static final String COLUMN_LON = "longitude";
    public static final String COLUMN_TTL = "ttl";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_PACKETS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY, " +
                    COLUMN_TYPE + " INTEGER, " +
                    COLUMN_LAT + " REAL, " +
                    COLUMN_LON + " REAL, " +
                    COLUMN_TTL + " INTEGER, " +
                    COLUMN_TIMESTAMP + " INTEGER" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PACKETS);
        onCreate(db);
    }
}