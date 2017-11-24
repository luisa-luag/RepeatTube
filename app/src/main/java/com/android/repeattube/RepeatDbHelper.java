package com.android.repeattube;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by otavi on 11/24/2017.
 */

public class RepeatDbHelper extends SQLiteOpenHelper {

    //DB name and version
    private static final String DATABASE_NAME = "repeat.db";
    private static final int DATABASE_VERSION = 1;

    public RepeatDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String SQL_CREATE_REPEAT_TABLE = "CREATE TABLE " +
                RepeatContract.RepeatEntry.TABLE_NAME + " (" +
                RepeatContract.RepeatEntry.COLUMN_VIDEO_ID + " STRING PRIMARY KEY, " +
                RepeatContract.RepeatEntry.COLUMN_REPEAT_COUNT + " INTEGER NOT NULL" +
                ");";

        sqLiteDatabase.execSQL(SQL_CREATE_REPEAT_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }
}
