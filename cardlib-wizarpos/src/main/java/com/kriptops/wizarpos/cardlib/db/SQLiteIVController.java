package com.kriptops.wizarpos.cardlib.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteIVController extends SQLiteOpenHelper implements IVController {

    public static final int DB_VERSION = 1;
    public static final String DB_NAME = "cardlib_database";
    public static final String IV_TABLE_NAME = "t_iv";
    public static final String IV_USAGE_COLUMN = "iv_usage";
    public static final String IV_VALUE_COLUMN = "iv_value";

    public SQLiteIVController(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + IV_TABLE_NAME + " (" +
                IV_TABLE_NAME +" INTEGER PRIMARY KEY AUTOINCREMENT," +
                IV_USAGE_COLUMN + " TEXT," +
                IV_VALUE_COLUMN + " TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    @Override
    public void saveIv(String usage, String iv) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();

        db.delete(IV_TABLE_NAME, IV_USAGE_COLUMN + " like ?", new String [] { usage });

        ContentValues contentValues = new ContentValues();
        contentValues.put(IV_USAGE_COLUMN, usage);
        contentValues.put(IV_VALUE_COLUMN, iv);

        db.insert(IV_TABLE_NAME, null, contentValues);
    }

    @Override
    public String readIv(String usage) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                IV_TABLE_NAME,
                new String [] { IV_VALUE_COLUMN },
                IV_USAGE_COLUMN + " like ?",
                new String [] { usage },
                null,
                null,
                null
        );

        try {
            if (cursor.getCount() == 0) {
                cursor.close();
                return null;
            }
            cursor.moveToFirst();
            return cursor.getString(1);
        } finally {
            cursor.close();
        }
    }
}
