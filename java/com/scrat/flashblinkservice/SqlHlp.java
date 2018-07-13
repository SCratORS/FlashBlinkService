package com.scrat.flashblinkservice;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SqlHlp extends SQLiteOpenHelper {
    public SqlHlp(Context context) {
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
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
