package com.scrat.flashblinkservice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class SqlHlp extends SQLiteOpenHelper {

    SqlHlp(Context context) {
        super(context, "DB", null, 1);
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

    private String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "dd MMMM HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }


    void post(String msg) {
        ContentValues cVal = new ContentValues();
        cVal.put("intent", msg);
        cVal.put("dta", getDateTime());
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.query("logsTable", null, null, null, null, null, null);
        if (c.moveToFirst() && (c.getCount() > 50)) db.delete("logsTable", "id="+c.getInt(c.getColumnIndex("id")), null);
        db.insert("logsTable", null, cVal);
        c.close();
        db.close();
        cVal.clear();
    }

    List<ContentValues> getRecord() {
        SQLiteDatabase db = this.getReadableDatabase();
        ContentValues cVal;
        List<ContentValues> list = new ArrayList<>();
        Cursor c = db.query("logsTable", null, null, null, null, null, "id DESC");
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
