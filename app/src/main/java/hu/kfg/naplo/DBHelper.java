package hu.kfg.naplo;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Grades.db";
    private static final String GRADES_TABLE_NAME= "grades";
    private static final String GRADES_COLUMN_SUBJECT = "subject";
    private static final String GRADES_COLUMN_ID = "id";
    private static final String GRADES_COLUMN_DESCRIPTION = "description";
    private static final String GRADES_COLUMN_DATE = "date";
    private static final String GRADES_COLUMN_VALUE = "value";
    private static final String GRADES_COLUMN_TEACHER = "teacher";
    private static final String GRADES_COLUMN_WEIGHTED = "weighted";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
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

    boolean insertGrade(String description, String teacher, String date, String subject, byte grade) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("description", description);
        contentValues.put("teacher", teacher);
        contentValues.put("date", date);
        contentValues.put("subject", subject);
        contentValues.put("value", grade);
        return db.insert("grades", null, contentValues)>-1;
    }

    boolean insertGrade(Grade grade) {
        //Log.i("Grades",grade.description+grade.teacher+grade.date+grade.subject+grade.value);
        return insertGrade(grade.description, grade.teacher, grade.date, grade.subject, grade.value);
    }

    Cursor getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from grades where id=" + id + "", null);
        return res;
    }

    ArrayList<String> getSubjects() {
        ArrayList<String> array_list = new ArrayList<String>();

        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select distinct subject from grades order by subject", null);
        res.moveToFirst();

        while (res.isAfterLast() == false) {
            array_list.add(res.getString(res.getColumnIndex(GRADES_COLUMN_SUBJECT)));
            res.moveToNext();
        }
        return array_list;
    }

    List<Grade> getSubjectGradesG(String subject) {
        List<Grade> array_list = new ArrayList<Grade>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from grades where subject=\"" + subject + "\" order by date desc", null);
        res.moveToFirst();
        Grade g;
        while (res.isAfterLast() == false) {
            g = new Grade((byte)res.getShort(res.getColumnIndex(GRADES_COLUMN_VALUE)));
            g.addSubject(res.getString(res.getColumnIndex(GRADES_COLUMN_SUBJECT)));
            g.addTeacher(res.getString(res.getColumnIndex(GRADES_COLUMN_TEACHER)));
            g.addDescription(res.getString(res.getColumnIndex(GRADES_COLUMN_DESCRIPTION)));
            g.addDate(res.getString(res.getColumnIndex(GRADES_COLUMN_DATE)));
            g.addID(res.getInt(res.getColumnIndex(GRADES_COLUMN_ID)));
            array_list.add(g);
            res.moveToNext();
        }
        return array_list;
    }

    Grade getGradeById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from grades where id=\"" + id + "\"", null);
        res.moveToFirst();
        Grade g = new Grade((byte)0);
        while (res.isAfterLast() == false) {
            g = new Grade((byte)res.getShort(res.getColumnIndex(GRADES_COLUMN_VALUE)));
            g.addSubject(res.getString(res.getColumnIndex(GRADES_COLUMN_SUBJECT)));
            g.addTeacher(res.getString(res.getColumnIndex(GRADES_COLUMN_TEACHER)));
            g.addDescription(res.getString(res.getColumnIndex(GRADES_COLUMN_DESCRIPTION)));
            g.addDate(res.getString(res.getColumnIndex(GRADES_COLUMN_DATE)));
            g.addID(res.getInt(res.getColumnIndex(GRADES_COLUMN_ID)));
            res.moveToNext();
        }
        return g;
    }

    int numberOfRows() {
        int numRows = 0;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            numRows = (int) DatabaseUtils.queryNumEntries(db, GRADES_TABLE_NAME);
        } finally {
            return numRows;
        }
    }

    boolean upgradeDatabase(List<Grade> grades) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS grades");
        onCreate(db);
        for (Grade grade:grades) {
            if (!insertGrade(grade)) return false;
        }
        return true;
    }

    boolean cleanDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS grades");
        onCreate(db);
        return true;
    }
}