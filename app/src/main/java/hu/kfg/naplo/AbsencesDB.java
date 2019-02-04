package hu.kfg.naplo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AbsencesDB extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Common.db";
    private static final String ABSENCES_TABLE_NAME = "absences";
    private static final String ABSENCES_COLUMN_ID = "id";
    private static final String ABSENCES_COLUMN_MODE = "mode";
    private static final String ABSENCES_COLUMN_SUBJECT = "subject";
    private static final String ABSENCES_COLUMN_TEACHER = "teacher";
    private static final String ABSENCES_COLUMN_JUSTSTATE = "juststate";
    private static final String ABSENCES_COLUMN_ABSENCE = "dayofabsence";
    private static final String ABSENCES_COLUMN_REGISTER = "dayofregister";
    private static final String ABSENCES_COLUMN_PERIOD = "period";

    private Date maxYear;

    public AbsencesDB(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        maxYear = null;
        db.execSQL(
                "CREATE TABLE absences (id integer PRIMARY KEY, mode text, subject text, teacher text, juststate text, dayofabsence text, dayofregister text, period smallint)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + ABSENCES_TABLE_NAME);
        onCreate(db);
    }

    boolean insertEvent(String mode, String subject, String teacher, String justState, Date dayOfAbsence, Date dayOfRegister, byte period) {
        try {
            maxYear = null;
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put(ABSENCES_COLUMN_MODE, mode);
            contentValues.put(ABSENCES_COLUMN_SUBJECT, subject);
            contentValues.put(ABSENCES_COLUMN_JUSTSTATE, justState);
            contentValues.put(ABSENCES_COLUMN_ABSENCE, Absence.absenceFormat.format(dayOfAbsence));
            contentValues.put(ABSENCES_COLUMN_REGISTER, Absence.absenceFormat.format(dayOfRegister));
            contentValues.put(ABSENCES_COLUMN_TEACHER, teacher);
            contentValues.put(ABSENCES_COLUMN_PERIOD, period);
            return db.insert(ABSENCES_TABLE_NAME, null, contentValues) > -1;
        } catch (Exception e) {
            Log.e("AbsencesDB", "Operation failed, cleaning database");
            e.printStackTrace();
            cleanDatabase();
            return false;
        }
    }

    boolean insertAbsence(Absence absence) {
        return insertEvent(absence.mode, absence.subject, absence.teacher, absence.justificationState, absence.dayOfAbsence, absence.dayOfRegister, absence.period);
    }

    ArrayList<Date> getDates() {
        ArrayList<Date> array_list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT DISTINCT " + ABSENCES_COLUMN_ABSENCE + " FROM " + ABSENCES_TABLE_NAME + " ORDER BY " + ABSENCES_COLUMN_ABSENCE + " DESC", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            try {
                array_list.add(Absence.absenceFormat.parse(res.getString(res.getColumnIndex(ABSENCES_COLUMN_ABSENCE))));
            } catch (Exception e) {
                e.printStackTrace();
            }
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    ArrayList<String> getSubjects() {
        ArrayList<String> array_list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT DISTINCT " + ABSENCES_COLUMN_SUBJECT + " FROM " + ABSENCES_TABLE_NAME + " GROUP BY " + ABSENCES_COLUMN_SUBJECT + " ORDER BY COUNT(*) DESC", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            try {
                array_list.add(res.getString(res.getColumnIndex(ABSENCES_COLUMN_SUBJECT)));
            } catch (Exception e) {
                e.printStackTrace();
            }
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    ArrayList<Absence> getAbsences() {
        try {
            ArrayList<Absence> array_list = new ArrayList<>();
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + ABSENCES_TABLE_NAME + " ORDER BY " + ABSENCES_COLUMN_ABSENCE, null);
            res.moveToFirst();

            while (!res.isAfterLast()) {
                try {
                    String mode = res.getString(res.getColumnIndex(ABSENCES_COLUMN_MODE));
                    String subject = res.getString(res.getColumnIndex(ABSENCES_COLUMN_SUBJECT));
                    String teacher = res.getString(res.getColumnIndex(ABSENCES_COLUMN_TEACHER));
                    String justState = res.getString(res.getColumnIndex(ABSENCES_COLUMN_JUSTSTATE));
                    byte period = (byte) res.getInt(res.getColumnIndex(ABSENCES_COLUMN_PERIOD));
                    Date absence = Absence.absenceFormat.parse(res.getString(res.getColumnIndex(ABSENCES_COLUMN_ABSENCE)));
                    Date register = Absence.absenceFormat.parse(res.getString(res.getColumnIndex(ABSENCES_COLUMN_REGISTER)));
                    array_list.add(new Absence(mode, subject, teacher, justState, absence, register, period));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                res.moveToNext();
            }
            res.close();
            return array_list;
        } catch (SQLiteException e) {
            Log.e("AbsencesDB", "Operation failed, cleaning database...");
            e.printStackTrace();
            cleanDatabase();
            return null;
        }
    }

    List<Absence> getAbsencesOnDay(Date day1) {
        try {
            List<Absence> array_list = new ArrayList<>();

            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + ABSENCES_TABLE_NAME
                    + " WHERE " + ABSENCES_COLUMN_ABSENCE + " like '" + Absence.absenceFormat.format(day1) + "%' " +
                    "ORDER BY " + ABSENCES_COLUMN_PERIOD + "", null);
            res.moveToFirst();
            while (!res.isAfterLast()) {
                try {
                    String mode = res.getString(res.getColumnIndex(ABSENCES_COLUMN_MODE));
                    String subject = res.getString(res.getColumnIndex(ABSENCES_COLUMN_SUBJECT));
                    String teacher = res.getString(res.getColumnIndex(ABSENCES_COLUMN_TEACHER));
                    String justState = res.getString(res.getColumnIndex(ABSENCES_COLUMN_JUSTSTATE));
                    byte period = (byte) res.getInt(res.getColumnIndex(ABSENCES_COLUMN_PERIOD));
                    Date absence = Absence.absenceFormat.parse(res.getString(res.getColumnIndex(ABSENCES_COLUMN_ABSENCE)));
                    Date register = Absence.absenceFormat.parse(res.getString(res.getColumnIndex(ABSENCES_COLUMN_REGISTER)));
                    array_list.add(new Absence(mode, subject, teacher, justState, absence, register, period));
                } catch (Exception pe) {
                    pe.printStackTrace();
                    return null;
                }
                res.moveToNext();
            }
            res.close();
            return array_list;
        } catch (Exception e) {
            Log.e("AbsencesDB", "Operation failed, cleaning database...");
            e.printStackTrace();
            cleanDatabase();
            return null;
        }
    }

    int getAbsencesNumberBySubject(String subject) {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + ABSENCES_TABLE_NAME
                    + " WHERE " + ABSENCES_COLUMN_SUBJECT + " like '" + subject + "' " +
                    "ORDER BY " + ABSENCES_COLUMN_PERIOD + "", null);
            res.moveToFirst();
            int count = res.getCount();
            res.close();
            return count;
        } catch (Exception e) {
            Log.e("AbsencesDB", "Operation failed, cleaning database...");
            e.printStackTrace();
            cleanDatabase();
            return 0;
        }
    }

    int getUnjustifiedAbsences() {
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            Cursor res = db.rawQuery("SELECT * FROM " + ABSENCES_TABLE_NAME
                    + " WHERE " + ABSENCES_COLUMN_JUSTSTATE + " not like '%Igazolt%' ", null);
            res.moveToFirst();
            int count = res.getCount();
            res.close();
            return count;
        } catch (Exception e) {
            Log.e("AbsencesDB", "Operation failed, cleaning database...");
            e.printStackTrace();
            cleanDatabase();
            return 0;
        }
    }

    int numberOfRows() {
        int numRows = 0;
        try {
            SQLiteDatabase db = this.getReadableDatabase();
            numRows = (int) DatabaseUtils.queryNumEntries(db, ABSENCES_TABLE_NAME);
        } finally {
            return numRows;
        }
    }

    boolean upgradeDatabase(List<Absence> absences) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS absences");
        onCreate(db);
        for (Absence a : absences) {
            if (!insertAbsence(a)) return false;
        }
        return true;
    }

    boolean cleanDatabase() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS absences");
            onCreate(db);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
