package hu.kfg.naplo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class TimetableDB extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Lessons.db";
    private static final String LESSONS_TABLE_NAME = "lessons";
    private static final String LESSONS_COLUMN_ID = "id";
    private static final String LESSONS_COLUMN_SUBJECT = "subject";
    private static final String LESSONS_COLUMN_TEACHER = "teacher";
    private static final String LESSONS_COLUMN_ROOM = "room";
    private static final String LESSONS_COLUMN_START = "start";
    private static final String LESSONS_COLUMN_FINISH = "finish";
    private static final String LESSONS_COLUMN_CLASS = "class";
    private static final String LESSONS_COLUMN_TOPIC = "topic";
    private static final String LESSONS_COLUMN_PERIOD = "period";
    private static final String LESSONS_COLUMN_CATEGORY = "category";


    static final SimpleDateFormat start = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
    static final SimpleDateFormat dayOnly = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    static final SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.getDefault());

    TimetableDB(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE lessons (id integer PRIMARY KEY, subject text,teacher text,room int, start text, finish text, class text,topic text, period smallint, category text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LESSONS_TABLE_NAME);
        onCreate(db);
    }

    boolean insertLesson(String subject, String teacher, int room, Date from, Date to, String group, String topic, byte period, String category) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(LESSONS_COLUMN_SUBJECT, subject);
        contentValues.put(LESSONS_COLUMN_TEACHER, teacher);
        contentValues.put(LESSONS_COLUMN_ROOM, room);
        contentValues.put(LESSONS_COLUMN_CLASS, group);
        contentValues.put(LESSONS_COLUMN_TOPIC, topic);
        contentValues.put(LESSONS_COLUMN_START, start.format(from));
        contentValues.put(LESSONS_COLUMN_FINISH, time.format(to));
        contentValues.put(LESSONS_COLUMN_PERIOD, period);
        contentValues.put(LESSONS_COLUMN_CATEGORY, category);
        return db.insert(LESSONS_TABLE_NAME, null, contentValues) > -1;
    }

    boolean insertLesson(Lesson lesson) {
        return insertLesson(lesson.subject, lesson.teacher, lesson.room, lesson.from, lesson.to, lesson.group, lesson.topic, lesson.period, lesson.subjectCat);
    }

    ArrayList<String> getSubjects() {
        ArrayList<String> array_list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT DISTINCT subject FROM lessons ORDER BY subject", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            array_list.add(res.getString(res.getColumnIndex(LESSONS_COLUMN_SUBJECT)));
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    List<Lesson> getLessonsOnDay(Date day1) {
        List<Lesson> array_list = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + LESSONS_TABLE_NAME
                + " WHERE " + LESSONS_COLUMN_START + " like '" + dayOnly.format(day1) + "%' " +
                "ORDER BY " + LESSONS_COLUMN_START + "", null);
        res.moveToFirst();
        Lesson l;
        while (!res.isAfterLast()) {
            try {
                l = new Lesson(res.getString(res.getColumnIndex(LESSONS_COLUMN_SUBJECT)),
                        res.getString(res.getColumnIndex(LESSONS_COLUMN_TEACHER)),
                        res.getInt(res.getColumnIndex(LESSONS_COLUMN_ROOM)),
                        start.parse(res.getString(res.getColumnIndex(LESSONS_COLUMN_START))),
                        time.parse(res.getString(res.getColumnIndex(LESSONS_COLUMN_FINISH))),
                        res.getString(res.getColumnIndex(LESSONS_COLUMN_CLASS)),
                        (byte) res.getShort(res.getColumnIndex(LESSONS_COLUMN_PERIOD)));
                l.setTopic(res.getString(res.getColumnIndex(LESSONS_COLUMN_TOPIC)));
                l.subjectCat = (res.getString(res.getColumnIndex(LESSONS_COLUMN_CATEGORY)));
                l.addID(res.getInt(res.getColumnIndex(LESSONS_COLUMN_ID)));
                array_list.add(l);
            } catch (Exception pe) {
                pe.printStackTrace();
                return null;
            }
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    Date lastDay() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT start FROM lessons ORDER BY start DESC LIMIT 1", null);
        res.moveToFirst();
        while (!res.isAfterLast()) {
            try {
                Date d = start.parse(res.getString(res.getColumnIndex(LESSONS_COLUMN_START)));
                res.close();
                return d;
            } catch (ParseException pe) {
                pe.printStackTrace();
            }
            res.moveToNext();
        }
        res.close();
        return null;
    }

    Date firstDay() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT start FROM lessons ORDER BY start LIMIT 1", null);
        res.moveToFirst();
        while (!res.isAfterLast()) {
            try {
                Date d = start.parse(res.getString(res.getColumnIndex(LESSONS_COLUMN_START)));
                res.close();
                return d;
            } catch (ParseException pe) {
                pe.printStackTrace();
            }
            res.moveToNext();
        }
        res.close();
        return null;
    }

    Lesson getLessonById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM lessons WHERE id=\"" + id + "\"", null);
        res.moveToFirst();
        Lesson l = null;
        while (!res.isAfterLast()) {
            try {
                l = new Lesson(res.getString(res.getColumnIndex(LESSONS_COLUMN_SUBJECT)),
                        res.getString(res.getColumnIndex(LESSONS_COLUMN_TEACHER)),
                        res.getInt(res.getColumnIndex(LESSONS_COLUMN_ROOM)),
                        start.parse(res.getString(res.getColumnIndex(LESSONS_COLUMN_START))),
                        time.parse(res.getString(res.getColumnIndex(LESSONS_COLUMN_FINISH))),
                        res.getString(res.getColumnIndex(LESSONS_COLUMN_CLASS)),
                        (byte) res.getShort(res.getColumnIndex(LESSONS_COLUMN_PERIOD)));
                l.setTopic(res.getString(res.getColumnIndex(LESSONS_COLUMN_TOPIC)));
                l.subjectCat = (res.getString(res.getColumnIndex(LESSONS_COLUMN_CATEGORY)));
                l.addID(res.getInt(res.getColumnIndex(LESSONS_COLUMN_ID)));
            } catch (Exception pe) {
                pe.printStackTrace();
                return null;
            }
            res.moveToNext();
        }
        res.close();
        return l;
    }

    String getStudentClass() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT " + LESSONS_COLUMN_CLASS +" FROM " + LESSONS_TABLE_NAME
                + " WHERE " + LESSONS_COLUMN_SUBJECT + " = 'osztályfőnöki' " +
                " LIMIT 1", null);
        res.moveToFirst();
        try {
            if (!res.isAfterLast()) {
                String cls = res.getString(res.getColumnIndex(LESSONS_COLUMN_CLASS));
                res.close();
                return cls;
            }
        } catch (Exception pe) {
            pe.printStackTrace();
        }
        res.close();
        return null;
    }

    int numberOfRows() {
        int numRows = 0;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            numRows = (int) DatabaseUtils.queryNumEntries(db, LESSONS_TABLE_NAME);
        } finally {
            return numRows;
        }
    }

    boolean upgradeDatabase(List<Lesson> lessons) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS lessons");
        onCreate(db);
        for (Lesson lesson : lessons) {
            if (!insertLesson(lesson)) return false;
        }
        return true;
    }

    void cleanDatabase() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS lessons");
        onCreate(db);
    }
}