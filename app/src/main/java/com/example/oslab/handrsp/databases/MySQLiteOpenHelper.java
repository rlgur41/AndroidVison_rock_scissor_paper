package com.example.oslab.handrsp.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by 김기혁 on 2016-11-13.
 */

public class MySQLiteOpenHelper extends SQLiteOpenHelper {
    public MySQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version){
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table rsp (" +
                "_id integer primary key autoincrement," +
                "user_result integer," +
                "computer_result integer," +
                "winner integer);";
        db.execSQL(sql);
    }

    public void insert(int user_result, int computer_result, int winner){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("INSERT INTO rsp VALUES(null, " + user_result + ", " + computer_result + ", " + winner + ");");
        db.close();
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        String sql = "drop table if exists date";
        db.execSQL(sql);
        onCreate(db);
    }
}
