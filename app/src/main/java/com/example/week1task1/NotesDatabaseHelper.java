package com.example.week1task1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class NotesDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_NAME = "notes";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_BODY = "body";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_COLOR = "color";

    public NotesDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_BODY + " TEXT, " +
                COLUMN_DATE + " TEXT, " +
                COLUMN_COLOR + " INTEGER);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long insertNote(String title, String body, String date, int color) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_BODY, body);
        values.put(COLUMN_DATE, date);
        values.put(COLUMN_COLOR, color);
        return db.insert(TABLE_NAME, null, values);
    }

    public void clearAllNotes() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, null, null);
    }

    public List<MainActivity.Note> getAllNotes() {
        List<MainActivity.Note> notes = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(TABLE_NAME, null, null, null, null, null, COLUMN_ID + " DESC");
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    int id = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ID));
                    String title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE));
                    String body = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BODY));
                    String date = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE));
                    int color = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_COLOR));
                    
                    // Passing empty userId, it will be assigned the correct UID during migration in MainActivity
                    notes.add(new MainActivity.Note(
                        String.valueOf(id), 
                        title != null ? title : "", 
                        body != null ? body : "", 
                        date != null ? date : "", 
                        color, 
                        ""
                    ));
                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return notes;
    }
}
