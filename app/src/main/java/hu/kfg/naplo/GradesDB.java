package hu.kfg.naplo;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;

public class GradesDB extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Grades.db";
    private static final String GRADES_TABLE_NAME = "grades";
    private static final String GRADES_COLUMN_SUBJECT = "subject";
    private static final String GRADES_COLUMN_ID = "id";
    private static final String GRADES_COLUMN_DESCRIPTION = "description";
    private static final String GRADES_COLUMN_DATE = "date";
    private static final String GRADES_COLUMN_SAVE_DATE = "save_date";
    private static final String GRADES_COLUMN_VALUE = "value";
    private static final String GRADES_COLUMN_TEACHER = "teacher";
    private static final String GRADES_COLUMN_MODE = "mode";
    private static final String GRADES_COLUMN_REGULAR = "regular";
    private static final String GRADES_COLUMN_WEIGHTED = "weighted";

    GradesDB(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE grades (id integer PRIMARY KEY, description text,teacher text,date text, save_date text, subject text,value tinyint, mode text, regular tinyint)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS grades");
        onCreate(db);
    }

    boolean insertGrade(String description, String teacher, String date, String save_date, String subject, byte grade, String mode, boolean regular) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(GRADES_COLUMN_DESCRIPTION, description);
        contentValues.put(GRADES_COLUMN_TEACHER, teacher);
        contentValues.put(GRADES_COLUMN_DATE, date);
        contentValues.put(GRADES_COLUMN_SAVE_DATE, save_date);
        contentValues.put(GRADES_COLUMN_SUBJECT, subject);
        contentValues.put(GRADES_COLUMN_VALUE, grade);
        contentValues.put(GRADES_COLUMN_MODE, mode);
        contentValues.put(GRADES_COLUMN_REGULAR, regular ? 1 : 0);
        return db.insert(GRADES_TABLE_NAME, null, contentValues) > -1;
    }

    boolean insertGrade(Grade grade) {
        return insertGrade(grade.description, grade.teacher, grade.date, grade.save_date, grade.subject, grade.value, grade.mode, grade.regular);
    }

    ArrayList<String> getSubjects() {
        ArrayList<String> array_list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT DISTINCT subject FROM grades ORDER BY subject", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            String subject = res.getString(res.getColumnIndex(GRADES_COLUMN_SUBJECT));
            if (!(subject == null || subject.equals("null")))
                array_list.add(subject);
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    int getNumberOfMaxGrades(int month) {
        ArrayList<String> array_list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT COUNT(*) AS numberOfGrades FROM " + GRADES_TABLE_NAME
                + " WHERE " + GRADES_COLUMN_DATE + " like '%-" + month + "-%' " +
                "GROUP BY " + GRADES_COLUMN_SUBJECT + " ORDER BY numberOfGrades DESC", null);
        res.moveToFirst();
        int result = res.getInt(0);
        res.close();
        return result;
    }

    List<Grade> getSubjectGrades(String subject) {
        try {
            List<Grade> array_list = new ArrayList<>();
            //Get an instance of the database object
            SQLiteDatabase db = this.getReadableDatabase();
            //Get all grades from the given subject and order them descending according to their date
            Cursor res = db.rawQuery("SELECT * FROM grades WHERE subject=\"" + subject + "\" ORDER BY date DESC", null);
            res.moveToFirst();
            Grade g;
            //Check if the current position of the Cursor object is in
            //the boundaries of the request
            while (!res.isAfterLast()) {
                //Add all parameters to the grade object
                g = new Grade((byte) res.getShort(res.getColumnIndex(GRADES_COLUMN_VALUE)));
                g.addSubject(res.getString(res.getColumnIndex(GRADES_COLUMN_SUBJECT)));
                g.addTeacher(res.getString(res.getColumnIndex(GRADES_COLUMN_TEACHER)));
                g.addDescription(res.getString(res.getColumnIndex(GRADES_COLUMN_DESCRIPTION)));
                g.addDate(res.getString(res.getColumnIndex(GRADES_COLUMN_DATE)));
                g.addSaveDate(res.getString(res.getColumnIndex(GRADES_COLUMN_SAVE_DATE)));
                g.addMode(res.getString(res.getColumnIndex(GRADES_COLUMN_MODE)));
                g.addID(res.getInt(res.getColumnIndex(GRADES_COLUMN_ID)));
                g.setRegular(res.getInt(res.getColumnIndex(GRADES_COLUMN_REGULAR)) != 0);
                //Save the object into the array
                array_list.add(g);
                res.moveToNext();
            }
            res.close();
            //If everything is OK, return the array (it may be empty)
            return array_list;
        } catch (Exception e) {
            //In case of an error, return null
            cleanDatabase();
            e.printStackTrace();
            return null;
        }
    }

    Grade getGradeById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM grades WHERE id=\"" + id + "\"", null);
        res.moveToFirst();
        Grade g = new Grade((byte) 0);
        while (!res.isAfterLast()) {
            g = new Grade((byte) res.getShort(res.getColumnIndex(GRADES_COLUMN_VALUE)));
            g.addSubject(res.getString(res.getColumnIndex(GRADES_COLUMN_SUBJECT)));
            g.addTeacher(res.getString(res.getColumnIndex(GRADES_COLUMN_TEACHER)));
            g.addDescription(res.getString(res.getColumnIndex(GRADES_COLUMN_DESCRIPTION)));
            g.addDate(res.getString(res.getColumnIndex(GRADES_COLUMN_DATE)));
            g.addSaveDate(res.getString(res.getColumnIndex(GRADES_COLUMN_SAVE_DATE)));
            g.addID(res.getInt(res.getColumnIndex(GRADES_COLUMN_ID)));
            g.setRegular(res.getInt(res.getColumnIndex(GRADES_COLUMN_REGULAR)) != 0);
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
        for (Grade grade : grades) {
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
            insertGrade("test2", "Random teacher", year + "-" + (month < 10 ? "0" : "") + Integer.toString(month) + "-24", "2019-07-21 18:22:10", "Teszt", (byte) 5, "MidYear", true);
            insertGrade("test3", "Sali", "2019-01-30", "2019-07-21 18:22:10", "fizika", (byte)5, "MidYear", true);
        }
    }
}