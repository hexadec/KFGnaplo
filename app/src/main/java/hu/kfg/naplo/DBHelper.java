package hu.kfg.naplo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "Grades.db";
    public static final String GRADES_TABLE_NAME= "grades";
    public static final String GRADES_COLUMN_SUBJECT = "subject";
    public static final String GRADES_COLUMN_ID = "id";
    public static final String GRADES_COLUMN_DESCRIPTION = "description";
    public static final String GRADES_COLUMN_DATE = "date";
    public static final String GRADES_COLUMN_VALUE = "value";
    public static final String GRADES_COLUMN_TEACHER = "teacher";
    private HashMap hp;

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL(
                "create table grades " +
                        "(id integer primary key, description text,teacher text,date text, subject text,value smallint)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS grades");
        onCreate(db);
    }

    public boolean insertGrade(String description, String teacher, String date, String subject, byte grade) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("description", description);
        contentValues.put("teacher", teacher);
        contentValues.put("date", date);
        contentValues.put("subject", subject);
        contentValues.put("grade", grade);
        db.insert("grades", null, contentValues);
        return true;
    }

    public Cursor getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from grades where id=" + id + "", null);
        return res;
    }

    public int numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, GRADES_TABLE_NAME);
        return numRows;
    }

    public boolean updateGrade(Integer id, String description, String teacher, String date, String subject, byte grade) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("description", description);
        contentValues.put("teacher", teacher);
        contentValues.put("date", date);
        contentValues.put("subject", subject);
        contentValues.put("grade", grade);
        db.update("grades", contentValues, "id = ? ", new String[]{Integer.toString(id)});
        return true;
    }

    public Integer deleteGrade(Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("grades",
                "id = ? ",
                new String[]{Integer.toString(id)});
    }

    public ArrayList<String> getAllGrades() {
        ArrayList<String> array_list = new ArrayList<String>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from grades", null);
        res.moveToFirst();

        while (res.isAfterLast() == false) {
            array_list.add(res.getString(res.getColumnIndex(GRADES_TABLE_NAME)));
            res.moveToNext();
        }
        return array_list;
    }
}