package hu.kfg.naplo;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
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
    private List<Event> events;
    private TextView eventView;

    static final String NO_LESSONS_INDICATORS[] = {"Síszünet", "Tavaszi szünet", "Őszi szünet", "Téli szünet",
            "Projektnap", "Projekt nap"};
    static final String EVENTS_URL = "https://apps.karinthy.hu/events/hu/eventlist.php?year=%s#act";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);
        currentDateShown = new Date();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd, EEEE", Locale.getDefault());
        db = new TimetableDB(this);
        eventView = findViewById(R.id.event_field);
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_WEEK, 5);
        if (db.numberOfRows() < 1 || (db.lastDay() != null && db.lastDay().before(cal.getTime()))) {
            doStuff();
        } else {
            updateViews();
        }
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                events = ChangeListener.doEventsCheck(TimetableActivity.this, new Date());
                doStuffWithEvents();
            }
        });
        t.start();
    }

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
                cal.add(Calendar.DAY_OF_WEEK, 21);
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
        for (Lesson l : lessons) {
            final TableRow row = new TableRow(TimetableActivity.this);
            TableLayout.LayoutParams lp = new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);
            TextView lView = new TextView(TimetableActivity.this);
            lView.setMinWidth((int) (size.x / 1.1));
            lView.setText(formatLesson(l));
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
                .append(lesson.subject)
                .append("&ensp;-&ensp;")
                .append(lesson.room);
        if (lesson.subjectCat != null && lesson.subjectCat.length() > 1) {
            sb.append("</b><br/>");
            sb.append(lesson.subjectCat);
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
            /*case R.id.infomenu_timetable:
                return true;*/
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
        for (Event e : events) {
            if (e.getStart().before(currentDateShown) && e.getEnd().after(currentDateShown)
                    || dateFormat.format(e.getStart()).equals(dateFormat.format(currentDateShown))) {
                toShow += e.getName();
                Log.e("I", e.toString());
            }
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
