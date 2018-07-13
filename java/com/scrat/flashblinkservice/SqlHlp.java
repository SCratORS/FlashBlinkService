package com.scrat.flashblinkservice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

class SqlHlp extends SQLiteOpenHelper {
    SqlHlp(Context context) {
        super(context, "logsDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table logsTable ("
                + "id integer primary key autoincrement,"
                + "dta datetime,"
                + "intent text" + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void post(ContentValues cVal) {
        if (this.count() > 10) this.clear();
        SQLiteDatabase db = this.getWritableDatabase();
        db.insert("logsTable", null, cVal);
        db.close();
    }

    private void clear() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("logsTable", null, null);
        db.close();
    }

    private long count() {
        SQLiteDatabase db = this.getReadableDatabase();
        long recordsCount;
        Cursor c = db.query("logsTable", null, null, null, null, null, null);
        recordsCount = c.getCount();
        c.close();
        return recordsCount;
    }

    public List<ContentValues> getRecord() {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues cVal;
        List<ContentValues> list = new ArrayList<>();
        Cursor c = db.query("logsTable", null, null, null, null, null, null);
        int dateColIndex = c.getColumnIndex("dta");
        int intentColIndex = c.getColumnIndex("intent");
        if (c.moveToFirst()) {
            do {
                cVal = new ContentValues();
                cVal.put("intent", c.getString(intentColIndex));
                cVal.put("dta", c.getString(dateColIndex));
                list.add(cVal);
            } while (c.moveToNext());
        }
        c.close();
        db.close();
        return list;
    }
}
