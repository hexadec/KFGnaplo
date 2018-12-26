package hu.kfg.naplo;

import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Display;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AbsencesActivity extends Activity {

    boolean lightmode = false;
    AbsencesDB db;
    SimpleDateFormat date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("light_theme_mode", false)) {
            setTheme(R.style.AppThemeLight);
            lightmode = true;
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_absences);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        db = new AbsencesDB(this);

        if (db.numberOfRows() < 1) {
            Log.e("AbsencesActivity", "Empty DB");
            //doStuff();
        } else {
            updateViews();
        }
    }

    private Spanned formatAbsence(Absence absence) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat register = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH);
        sb.append("<div align=\"left\" style=\"text-align:left;\"><b><big>")
                .append(absence.period)
                .append(".&ensp;&ensp;")
                .append(absence.subject != null && absence.subject.length() > 13
                        ? absence.subject.substring(0, 12) + "…" : absence.subject)
                .append("&ensp;-&ensp;")
                .append(absence.justificationState)
                .append("</b></big><br/>");
        sb.append(register.format(absence.dayOfRegister))
                .append("</div>")
                .append("<div align=\"center\" style=\"text-align:center;\">")
                .append(absence.teacher)
                .append("<br/></div>");

        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N ?
                Html.fromHtml(sb.toString(), Html.FROM_HTML_MODE_COMPACT) :
                Html.fromHtml(sb.toString());
    }

    private void updateViews() {
        List<Date> dates = db.getDates();
        if (dates == null) {
            Toast.makeText(AbsencesActivity.this, R.string.db_error, Toast.LENGTH_LONG).show();
            return;
        }
        TableLayout table = findViewById(R.id.absencestable);
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
        for (Date date : dates) {
            List<Absence> absences = db.getAbsencesOnDay(date);
            final TableRow row2 = new TableRow(AbsencesActivity.this);
            row2.setLayoutParams(lp);
            TextView lView2 = new TextView(AbsencesActivity.this);
            lView2.setMinWidth((int) (size.x / 1.1));
            lView2.setText(Html.fromHtml("<big><u><b>" + this.date.format(date)));
            row2.addView(lView2);
            table.addView(row2);
            for (Absence absence : absences) {
                final TableRow row = new TableRow(AbsencesActivity.this);
                row.setLayoutParams(lp);
                TextView lView = new TextView(AbsencesActivity.this);
                lView.setMinWidth((int) (size.x / 1.1));
                lView.setText(formatAbsence(absence));
                lView.setBackground(layerDrawable);
                row.addView(lView);
                table.addView(row);
            }
        }
        if (dates == null || dates.size() == 0) {
            final TableRow row = new TableRow(AbsencesActivity.this);
            row.setLayoutParams(lp);
            TextView lView = new TextView(AbsencesActivity.this);
            lView.setMinWidth((int) (size.x / 1.1));
            lView.setText(Html.fromHtml(String.format(getString(R.string.no_absences))));
            row.addView(lView);
            table.addView(row);
        }
    }

}
