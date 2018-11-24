package hu.kfg.naplo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventsDB extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Common.db";
    private static final String EVENTS_TABLE_NAME = "events";
    private static final String EVENTS_COLUMN_ID = "id";
    private static final String EVENTS_COLUMN_START = "start";
    private static final String EVENTS_COLUMN_FINISH = "finish";
    private static final String EVENTS_COLUMN_NAME = "name";

    static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private Date maxYear;

    public EventsDB(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        maxYear = null;
        db.execSQL(
                "CREATE TABLE events (id integer PRIMARY KEY, start text,finish text,name text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + EVENTS_TABLE_NAME);
        onCreate(db);
    }

    boolean insertEvent(String name, Date start, Date finish) {
        maxYear = null;
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(EVENTS_COLUMN_FINISH, dateFormat.format(finish));
        contentValues.put(EVENTS_COLUMN_START, dateFormat.format(start));
        contentValues.put(EVENTS_COLUMN_NAME, name);
        return db.insert(EVENTS_TABLE_NAME, null, contentValues) > -1;
    }

    boolean insertEvent(Event event) {
        return insertEvent(event.getName(), event.getStart(), event.getEnd());
    }

    ArrayList<Event> getEvents() {
        ArrayList<Event> array_list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT * FROM " + EVENTS_TABLE_NAME + " ORDER BY " + EVENTS_COLUMN_START, null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            try {
                String name = res.getString(res.getColumnIndex(EVENTS_COLUMN_NAME));
                Date start = dateFormat.parse(res.getString(res.getColumnIndex(EVENTS_COLUMN_START)));
                Date end = dateFormat.parse(res.getString(res.getColumnIndex(EVENTS_COLUMN_FINISH)));
                array_list.add(new Event(name, start, end));
            } catch (Exception e) {
                e.printStackTrace();
            }
            res.moveToNext();
        }
        res.close();
        return array_list;
    }

    Date getMaxYear() {
        if (maxYear != null) {
            return maxYear;
        }
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("SELECT start FROM " + EVENTS_TABLE_NAME + " ORDER BY " + EVENTS_COLUMN_START + " DESC LIMIT 1", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            try {
                return maxYear = dateFormat.parse(res.getString(res.getColumnIndex(EVENTS_COLUMN_START)));
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                res.close();
            }
            res.moveToNext();
        }
        res.close();
        return null;
    }

    boolean upgradeDatabase(List<Event> events) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS events");
        onCreate(db);
        for (Event e : events) {
            if (!insertEvent(e)) return false;
        }
        return true;
    }

    boolean cleanDatabase() {
        try {
            SQLiteDatabase db = this.getWritableDatabase();
            db.execSQL("DROP TABLE IF EXISTS events");
            onCreate(db);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
