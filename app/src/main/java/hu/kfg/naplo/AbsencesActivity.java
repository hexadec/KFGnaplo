package hu.kfg.naplo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
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
import android.widget.ScrollView;
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
    private int upgraderesult = 0;
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
            updateDatabase();
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
        for (int i = 0; i < dates.size(); i++) {
            Date date = dates.get(i);
            List<Absence> absences = db.getAbsencesOnDay(date);
            final TableRow row2 = new TableRow(AbsencesActivity.this);
            row2.setLayoutParams(lp);
            TextView lView2 = new TextView(AbsencesActivity.this);
            lView2.setMinWidth((int) (size.x / 1.1));
            lView2.setText(Html.fromHtml((i > 0 ? "<br/>" : "") + "<div style=\"text-align:center;\"><big><big><u><b>" + this.date.format(date) + "</b></u></big></big>"));
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

    void updateDatabase() {
        if (PreferenceManager.getDefaultSharedPreferences(AbsencesActivity.this).getString("password2", "").length() <= 1) {
            Toast t = Toast.makeText(AbsencesActivity.this, R.string.incorrect_credentials, Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            finish();
            return;
        }
        final ProgressDialog pdialog = ProgressDialog.show(AbsencesActivity.this, "",
                getString(R.string.upgrading), true);
        pdialog.show();
        Thread thr = new Thread(new Runnable() {
            public void run() {
                final Intent intent = new Intent(AbsencesActivity.this, ChangeListener.class);
                intent.putExtra("dbupgrade", true);
                intent.putExtra("error", true);
                intent.setAction("hu.kfg.naplo.CHECK_NOW");
                Thread t2 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            upgraderesult = ChangeListener.getEkretaGrades(AbsencesActivity.this, intent);
                        } catch (Exception e) {
                            upgraderesult = -10;
                            e.printStackTrace();
                        }
                    }
                });
                upgraderesult = -10;
                t2.start();
                try {
                    t2.join(20000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pdialog.cancel();
                Looper.prepare();
                if (upgraderesult == ChangeListener.DB_EMPTY) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Toast t = Toast.makeText(AbsencesActivity.this, R.string.empty_absences, Toast.LENGTH_SHORT);
                            t.setGravity(Gravity.CENTER, 0, 0);
                            t.show();

                        }
                    });
                } else if (upgraderesult == ChangeListener.UPGRADE_DONE) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateViews();
                        }
                    });
                } else if (upgraderesult == ChangeListener.TOKEN_ERROR || upgraderesult == ChangeListener.CREDENTIALS_ERROR) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Toast t = Toast.makeText(AbsencesActivity.this, R.string.incorrect_credentials, Toast.LENGTH_SHORT);
                            t.setGravity(Gravity.CENTER, 0, 0);
                            t.show();
                            ((TableLayout) findViewById(R.id.absencestable)).removeAllViews();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Toast t = Toast.makeText(AbsencesActivity.this, R.string.ohno, Toast.LENGTH_SHORT);
                            t.setGravity(Gravity.CENTER, 0, 0);
                            t.show();
                        }
                    });
                }
                Looper.loop();
            }

        });
        thr.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.absencesmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.statistics:
                AlertDialog.Builder adb = new AlertDialog.Builder(this);
                adb.setTitle(R.string.statistics);
                adb.setPositiveButton("OK", null);
                StringBuilder text = new StringBuilder();
                text.append(getString(R.string.stat_total_missed))
                        .append(": ")
                        .append(db.numberOfRows())
                        .append("\n")
                        .append(getString(R.string.stat_total_unjust))
                        .append(": ")
                        .append(db.getUnjustifiedAbsences());
                List<String> subjects = db.getSubjects();
                for (String subject : subjects) {
                    text.append("\n");
                    text.append(subject);
                    text.append(": ");
                    text.append(db.getAbsencesNumberBySubject(subject));
                }
                adb.setMessage(text.toString());
                adb.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

}
