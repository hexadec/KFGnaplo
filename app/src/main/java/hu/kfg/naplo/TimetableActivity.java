package hu.kfg.naplo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimetableActivity extends Activity {

    private Date currentDateShown;
    private SimpleDateFormat dateFormat;
    private TimetableDB db;
    private EventsDB eventsDB;
    private List<Event> events;
    private TextView eventView;
    private boolean lightmode = false;

    static final String NO_LESSONS_INDICATORS[] = {"Síszünet", "Tavaszi szünet", "Őszi szünet", "Téli szünet",
            "Projektnap", "Projekt nap"};
    static final String EVENTS_URL = "https://apps.karinthy.hu/events/hu/eventlist.php?year=%s#act";

    private static final int DAYS_TO_DOWNLOAD = 21;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("light_theme_mode", false)) {
            setTheme(R.style.AppThemeLight);
            lightmode = true;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        currentDateShown = new Date();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd, EEEE", Locale.getDefault());
        db = new TimetableDB(this);
        eventsDB = new EventsDB(TimetableActivity.this);
        eventView = findViewById(R.id.event_field);
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_WEEK, 5);
        if (db.numberOfRows() < 1 || (db.lastDay() != null && db.lastDay().before(cal.getTime()))) {
            doStuff();
        } else {
            updateViews();
        }
        Thread t = new Thread(r);
        t.start();
    }

    Runnable r = new Runnable() {
        @Override
        public void run() {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.DAY_OF_WEEK, DAYS_TO_DOWNLOAD);
            SimpleDateFormat smd = new SimpleDateFormat("yyyy", Locale.getDefault());
            events = eventsDB.getEvents();
            try {
                if (events == null || events.size() < 1) {
                    events = ChangeListener.doEventsCheck(TimetableActivity.this, new Date());
                    eventsDB.upgradeDatabase(events);
                }
                if (Integer.valueOf(smd.format(cal.getTime())) > Integer.valueOf(smd.format(eventsDB.getMaxYear()))) {
                    Log.i("Timetable-events", "Downloading events for next year");
                    if (Integer.valueOf(smd.format(new Date())) >= (cal.get(Calendar.YEAR))) {
                        events = ChangeListener.doEventsCheck(TimetableActivity.this, new Date());
                    } else {
                        events = ChangeListener.doEventsCheck(TimetableActivity.this, new Date());
                        events.addAll(ChangeListener.doEventsCheck(TimetableActivity.this, cal.getTime()));
                    }
                    eventsDB.upgradeDatabase(events);
                }
            } catch (NullPointerException | NumberFormatException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            doStuffWithEvents();
        }
    };

    private void doStuff() {
        if (PreferenceManager.getDefaultSharedPreferences(TimetableActivity.this).getString("password2", "").length() <= 1) {
            Toast t = Toast.makeText(TimetableActivity.this, R.string.incorrect_credentials, Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            finish();
            return;
        }
        ConnectivityManager cm =
                (ConnectivityManager) TimetableActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null || cm.getActiveNetworkInfo() == null) {
            Toast.makeText(TimetableActivity.this, R.string.no_network_conn, Toast.LENGTH_SHORT).show();
            return;
        }
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnected();
        if (!isConnected) {
            Toast.makeText(TimetableActivity.this, R.string.no_network_conn, Toast.LENGTH_SHORT).show();
            return;
        }
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date());
                cal.add(Calendar.DAY_OF_WEEK, DAYS_TO_DOWNLOAD);
                final int result = ChangeListener.getTimetable(TimetableActivity.this, new Date(), cal.getTime(), true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (result) {
                            case ChangeListener.NETWORK_RELATED_ERROR:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final Toast t = Toast.makeText(TimetableActivity.this, R.string.ohno, Toast.LENGTH_SHORT);
                                        t.setGravity(Gravity.CENTER, 0, 0);
                                        t.show();
                                    }
                                });
                                break;
                            case ChangeListener.CREDENTIALS_ERROR:
                            case ChangeListener.TOKEN_ERROR:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final Toast t = Toast.makeText(TimetableActivity.this, R.string.incorrect_credentials, Toast.LENGTH_SHORT);
                                        t.setGravity(Gravity.CENTER, 0, 0);
                                        t.show();
                                        ((TableLayout) findViewById(R.id.timetable)).removeAllViews();
                                    }
                                });
                                break;
                        }
                        updateViews();
                    }
                });
            }
        });
        thread.start();
        eventsDB.cleanDatabase();
        new Thread(r).start();
    }

    private void updateViews() {
        TextView dateField = findViewById(R.id.date_field);
        dateField.setText(dateFormat.format(currentDateShown));
        List<Lesson> lessons = db.getLessonsOnDay(currentDateShown);
        if (lessons == null) {
            Toast.makeText(TimetableActivity.this, R.string.db_error, Toast.LENGTH_LONG).show();
            return;
        }
        TableLayout table = findViewById(R.id.timetable);
        table.removeAllViews();
        table.setMeasureWithLargestChildEnabled(true);
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final TableLayout.LayoutParams lp = new TableLayout.LayoutParams(
                TableLayout.LayoutParams.MATCH_PARENT,
                TableLayout.LayoutParams.WRAP_CONTENT);
        int strokeColor = getResources().getColor(android.R.color.darker_gray);
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.RECTANGLE);
        background.setStroke(3, strokeColor);
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{background});
        layerDrawable.setLayerInset(0, -3, -3, -3, 0);
        for (Lesson l : lessons) {
            final TableRow row = new TableRow(TimetableActivity.this);
            row.setLayoutParams(lp);
            TextView lView = new TextView(TimetableActivity.this);
            lView.setMinWidth((int) (size.x / 1.1));
            lView.setText(formatLesson(l));
            lView.setBackground(layerDrawable);
            row.addView(lView);
            table.addView(row);
        }
        if (lessons.size() == 0) {
            final TableRow row = new TableRow(TimetableActivity.this);
            row.setLayoutParams(lp);
            TextView lView = new TextView(TimetableActivity.this);
            lView.setMinWidth((int) (size.x / 1.1));
            lView.setText(Html.fromHtml(String.format(getString(R.string.no_lessons_on_day))));
            row.addView(lView);
            table.addView(row);
        }
        doStuffWithEvents();
    }

    private Spanned formatLesson(Lesson lesson) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div align=\"left\" style=\"text-align:left;\"><b><big>")
                .append(lesson.period)
                .append(".&ensp;&ensp;&ensp;")
                .append(lesson.subject != null && lesson.subject.length() > 15
                        ? lesson.subject.substring(0, 14) + "…" : lesson.subject)
                .append("&ensp;-&ensp;")
                .append(lesson.room);
        if (lesson.subjectCat != null && lesson.subjectCat.length() > 1) {
            sb.append("</b><br/>");
            sb.append(lesson.subjectCat != null && lesson.subjectCat.length() > 25
                    ? lesson.subjectCat.substring(0, 24) + "…" : lesson.subjectCat);
            sb.append("</big><br/>");
        } else {
            sb.append("</b></big><br/>");
        }
        sb.append(TimetableDB.time.format(lesson.from))
                .append('-')
                .append(TimetableDB.time.format(lesson.to))
                .append("</div>")
                .append("<div align=\"center\" style=\"text-align:center;\">")
                .append(lesson.teacher)
                .append("<br/>")
                .append(lesson.group)
                .append("</div>");

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_COMPACT) :
                Html.fromHtml(sb.toString());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.timetablemenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Calendar cal = Calendar.getInstance();
        switch (item.getItemId()) {
            case R.id.next_day:
                cal.setTime(currentDateShown);
                cal.add(Calendar.DAY_OF_WEEK, 1);
                if (db.lastDay() != null && db.lastDay().before(cal.getTime())) {
                    Toast.makeText(this, R.string.missing_timetable_on_day, Toast.LENGTH_SHORT).show();
                    return true;
                }
                currentDateShown = cal.getTime();
                updateViews();
                return true;
            case R.id.prev_day:
                cal.setTime(currentDateShown);
                cal.add(Calendar.DAY_OF_WEEK, -1);
                if (db.firstDay() != null && db.firstDay().after(cal.getTime())) {
                    Toast.makeText(this, R.string.missing_timetable_on_day, Toast.LENGTH_SHORT).show();
                    return true;
                }
                currentDateShown = cal.getTime();
                updateViews();
                return true;
            case R.id.refresh_timetable:
                currentDateShown = new Date();
                doStuff();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void doStuffWithEvents() {
        String toShow = "";
        if (events == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    eventView.setText("");
                    eventView.setVisibility(View.GONE);
                }
            });
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(currentDateShown);
        cal.add(Calendar.DAY_OF_WEEK, -1);
        //Log.i("Events", " " + events.size());
        for (Event e : events) {
            if (e.getStart().before(currentDateShown) && e.getEnd().after(cal.getTime())
                    || dateFormat.format(e.getStart()).equals(dateFormat.format(currentDateShown))) {
                toShow += e.getName();
                Log.i("I", e.getStart() + "/" + e.getEnd());
            }
        }
        if (lightmode) {
            eventView.setTextColor(Color.parseColor("#EEEEEEEE"));
            eventView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_light));
        }
        if (!toShow.equals("")) {
            final String toShow2 = toShow;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    eventView.setText(toShow2);
                    eventView.setVisibility(View.VISIBLE);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    eventView.setText("");
                    eventView.setVisibility(View.GONE);
                }
            });
        }
    }
}
