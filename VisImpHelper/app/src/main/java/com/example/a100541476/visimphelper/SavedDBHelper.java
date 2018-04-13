package com.example.a100541476.visimphelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SavedDBHelper extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_FILENAME = "saved.db";


    private final String CREATE_STATEMENT = "" +
            "create table saved(" +
            " id integer primary key autoincrement not null," +
            " name varchar(100) not null," +
            " data varchar(10000) not null)";

    private static final String DROP_STATEMENT = "" +
            "drop table saved";

    public SavedDBHelper(Context context){
        super(context, DATABASE_FILENAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {db.execSQL(CREATE_STATEMENT);}

    public void onUpgrade(SQLiteDatabase db, int old, int upgrade) {}

    public void deleteAll(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("saved", "", new String[] {});
    }

    public void addNew(String name, String data){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("data", data);
        db.insert("saved", null, values);
    }

    public String getSavedData(String name){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query("saved", new String[] {"data"}, "name = ?", new String[] {""+name}, "", "", "");
        cursor.moveToFirst();
        String data = cursor.getString(0);
        return data;
    }
}
