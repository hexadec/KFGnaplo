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

public class GradesDB extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Grades.db";
    private static final String GRADES_TABLE_NAME= "grades";
    private static final String GRADES_COLUMN_SUBJECT = "subject";
    private static final String GRADES_COLUMN_ID = "id";
    private static final String GRADES_COLUMN_DESCRIPTION = "description";
    private static final String GRADES_COLUMN_DATE = "date";
    private static final String GRADES_COLUMN_SAVE_DATE = "save_date";
    private static final String GRADES_COLUMN_VALUE = "value";
    private static final String GRADES_COLUMN_TEACHER = "teacher";
    private static final String GRADES_COLUMN_WEIGHTED = "weighted";

    GradesDB(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE grades (id integer PRIMARY KEY, description text,teacher text,date text, save_date text, subject text,value smallint)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS grades");
        onCreate(db);
    }

    boolean insertGrade(String description, String teacher, String date, String save_date, String subject, byte grade) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("description", description);
        contentValues.put("teacher", teacher);
        contentValues.put("date", date);
        contentValues.put("save_date", save_date);
        contentValues.put("subject", subject);
        contentValues.put("value", grade);
        return db.insert("grades", null, contentValues)>-1;
    }

    boolean insertGrade(Grade grade) {
        return insertGrade(grade.description, grade.teacher, grade.date, grade.save_date, grade.subject, grade.value);
    }

    ArrayList<String> getSubjects() {
        ArrayList<String> array_list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT DISTINCT subject FROM grades ORDER BY subject", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            array_list.add(res.getString(res.getColumnIndex(GRADES_COLUMN_SUBJECT)));
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    List<Grade> getSubjectGradesG(String subject) {
        try {
            List<Grade> array_list = new ArrayList<>();

            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM grades WHERE subject=\"" + subject + "\" ORDER BY date DESC", null);
            res.moveToFirst();
            Grade g;
            while (!res.isAfterLast()) {
                g = new Grade((byte) res.getShort(res.getColumnIndex(GRADES_COLUMN_VALUE)));
                g.addSubject(res.getString(res.getColumnIndex(GRADES_COLUMN_SUBJECT)));
                g.addTeacher(res.getString(res.getColumnIndex(GRADES_COLUMN_TEACHER)));
                g.addDescription(res.getString(res.getColumnIndex(GRADES_COLUMN_DESCRIPTION)));
                g.addDate(res.getString(res.getColumnIndex(GRADES_COLUMN_DATE)));
                g.addSaveDate(res.getString(res.getColumnIndex(GRADES_COLUMN_SAVE_DATE)));
                g.addID(res.getInt(res.getColumnIndex(GRADES_COLUMN_ID)));
                array_list.add(g);
                res.moveToNext();
            }
            res.close();
            return array_list;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    Grade getGradeById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM grades WHERE id=\"" + id + "\"", null);
        res.moveToFirst();
        Grade g = new Grade((byte)0);
        while (!res.isAfterLast()) {
            g = new Grade((byte)res.getShort(res.getColumnIndex(GRADES_COLUMN_VALUE)));
            g.addSubject(res.getString(res.getColumnIndex(GRADES_COLUMN_SUBJECT)));
            g.addTeacher(res.getString(res.getColumnIndex(GRADES_COLUMN_TEACHER)));
            g.addDescription(res.getString(res.getColumnIndex(GRADES_COLUMN_DESCRIPTION)));
            g.addDate(res.getString(res.getColumnIndex(GRADES_COLUMN_DATE)));
            g.addSaveDate(res.getString(res.getColumnIndex(GRADES_COLUMN_SAVE_DATE)));
            g.addID(res.getInt(res.getColumnIndex(GRADES_COLUMN_ID)));
            res.moveToNext();
        }
        res.close();
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

    void cleanDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS grades");
        onCreate(db);
    }

    void TestinsertGradeForEachMonth() {
        for (int i = 1; i < 11; i++) {
            int month = i > 6 ? i + 2 : i;
            String year = month > 8 ? "2016" : "2017";
            insertGrade("test2", "Random teacher", year + "-" + (month < 10 ? "0" : "") + Integer.toString(month) + "-24", "2019-07-21 18:22:10", "Teszt", (byte) 5);
        }
    }
}