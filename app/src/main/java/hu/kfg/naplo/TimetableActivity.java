package hu.kfg.naplo;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TimetableActivity extends Activity {

    private Date currentDateShown;
    private SimpleDateFormat dateFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);
        currentDateShown = new Date();
        dateFormat = new SimpleDateFormat("YYYY-MM-dd", Locale.getDefault());
        updateViews();
    }

    private void updateViews() {
        TextView dateField = findViewById(R.id.date_field);
        dateField.setText(dateFormat.format(currentDateShown));
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
                currentDateShown = cal.getTime();
                updateViews();
                return true;
            case R.id.prev_day:
                cal.setTime(currentDateShown);
                cal.add(Calendar.DAY_OF_WEEK, -1);
                currentDateShown = cal.getTime();
                updateViews();
                return true;
            case R.id.refresh_timetable:
                return true;
            case R.id.infomenu_timetable:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
