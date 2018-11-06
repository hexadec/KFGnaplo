package hu.kfg.naplo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.app.Activity;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TableViewActivity extends Activity implements View.OnClickListener {

    DBHelper db;
    int upgraderesult = 0;
    int VIEW_HEIGHT = 30;
    boolean darkmode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_view);
        darkmode = PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dark_mode", true);
        db = new DBHelper(TableViewActivity.this);
        if (db.numberOfRows() < 1) {
            updateDatabase(db);
        } else {
            doStuff(db);
        }
    }

    void doStuff(final DBHelper db) {
        final TableLayout table = findViewById(R.id.table);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                table.removeAllViews();
                table.setMeasureWithLargestChildEnabled(true);
            }
        });
        //db.TestinsertGradeForEachMonth();
        final ArrayList<String> subjects = db.getSubjects();
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = -1; i < subjects.size(); i++) {
                    final TableRow row = new TableRow(TableViewActivity.this);

                    TableLayout.LayoutParams lp = new TableLayout.LayoutParams(
                            TableLayout.LayoutParams.MATCH_PARENT,
                            TableLayout.LayoutParams.WRAP_CONTENT);
                    row.setLayoutParams(lp);

                    row.setPadding(15, 0, 15, 0);

                    if (i == -1)
                        row.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));


                    TextView Header = new TextView(TableViewActivity.this);

                    Header.setGravity(Gravity.CENTER);
                    if (i == -1) {
                        Header.setText(R.string.subjects);
                        Header.setTextSize(18.0f);
                        Header.setTextColor(Color.parseColor("#FFFFFF"));
                    } else {
                        Header.setText(subjects.get(i).length() > 20 ? subjects.get(i).substring(0, 17) + "…" : subjects.get(i));
                        Header.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                        Header.setTextSize(18.0f);
                        Header.setBackground(createBackground(11, 3));
                    }
                    Header.setPadding(applyDimension(5),
                            applyDimension(1),
                            applyDimension(5),
                            applyDimension(1));
                    Header.setHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, VIEW_HEIGHT, getResources().getDisplayMetrics()));
                    Header.setTypeface(null, Typeface.BOLD);

                    row.addView(Header);
                    if (i != -1) {
                        final List<Grade> grades = db.getSubjectGradesG(subjects.get(i));
                        if (grades == null) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(TableViewActivity.this, R.string.db_error, Toast.LENGTH_LONG).show();
                                }
                            });
                            return;
                        }
                        double avg = 0;
                        for (Grade g : grades) {
                            avg += g.value;
                        }
                        avg /= grades.size();
                        int month = 0;
                        int doublegrade = 0;
                        for (int j = -1; j < grades.size(); j++) {
                            SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                            SimpleDateFormat m = new SimpleDateFormat("MM", Locale.ENGLISH);
                            int mo = 0;
                            int mo2 = 0;
                            try {
                                Date d = s.parse(grades.get(j).date);
                                mo = Integer.valueOf(m.format(d));
                                if (((mo != month) || (j + 1 == grades.size() && mo != month)) && doublegrade == 0) {
                                    row.addView(monthSpelled(d));
                                }
                                Date dd = s.parse(grades.get(j + 1).date);
                                if (j != -1 && j + 1 < grades.size()) {
                                    if (grades.get(j).description.equals(grades.get(j + 1).description)
                                            && grades.get(j).date.equals(grades.get(j + 1).date)
                                            && grades.get(j).subject.equals(grades.get(j + 1).subject)) {
                                        if (Math.abs(d.getTime() - dd.getTime()) < 40 * 1000) {
                                            doublegrade++;
                                            continue;
                                        }
                                    }
                                }
                                mo2 = Integer.valueOf(m.format(dd));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            int monthNumber = mo >= 9 ? mo - 9 : mo + 3;
                            if (doublegrade > 0) {
                                int sum = 0;
                                for (int k = 0; k < doublegrade + 1; k++) {
                                    sum += grades.get(j - k).value;
                                }
                                avg = ((double) sum) / (doublegrade + 1);
                            }
                            TextView Values = new TextView(TableViewActivity.this);
                            Values.setPadding(applyDimension(10),
                                    applyDimension(1),
                                    applyDimension(10),
                                    applyDimension(1));
                            Values.setGravity(Gravity.CENTER);
                            Values.setTextSize(18.0f);
                            Values.setTextColor(Color.parseColor(doublegrade > 0 ? "#FF4500" : "#FFFFFF"));
                            Values.setTypeface(null, Typeface.ITALIC);
                            Values.setText(j == -1 || doublegrade > 0 ? new DecimalFormat("#.##").format(avg) : "" + grades.get(j).value);
                            Values.setId(j != -1 ? grades.get(j).id + 1000000 : grades.get(j + 1).id - 30000);
                            final int minPos = j - doublegrade;
                            final int maxPos = j;
                            final double val = avg;
                            Values.setOnClickListener(doublegrade > 0 ? new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    multiGradeListener(grades.subList(minPos, maxPos + 1), val);
                                }
                            } : TableViewActivity.this);
                            Values.setHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, VIEW_HEIGHT, getResources().getDisplayMetrics()));
                            if ((j + 1 == grades.size() && mo == month) || (mo == month && mo != mo2)) {
                                Values.setBackground(createBackground(monthNumber, 2));
                            } else if ((mo != month && mo != mo2) || (j + 1 == grades.size() && mo != month)) {
                                Values.setBackground(createBackground(monthNumber, 2));
                            } else if (j == -1) {
                                Values.setBackground(createBackground(10, 3));
                            } else {
                                Values.setBackground(createBackground(monthNumber, 1));
                            }
                            row.addView(Values);
                            month = mo;
                            doublegrade = 0;
                        }
                    } else {
                        TextView Header2 = new TextView(TableViewActivity.this);
                        Header2.setGravity(Gravity.CENTER);
                        Header2.setText(R.string.average);
                        Header2.setTextSize(18.0f);
                        Header2.setTextColor(Color.parseColor("#FFFFFF"));
                        Header2.setTypeface(null, Typeface.BOLD);
                        row.addView(Header2);
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            table.addView(row);
                        }
                    });
                }
            }
        }).start();

    }

    void updateDatabase(final DBHelper db) {
        if (PreferenceManager.getDefaultSharedPreferences(TableViewActivity.this).getString("password2", "").length() <= 1) {
            Toast t = Toast.makeText(TableViewActivity.this, R.string.incorrect_credentials, Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            finish();
            return;
        }
        final ProgressDialog pdialog = ProgressDialog.show(TableViewActivity.this, "",
                getString(R.string.upgrading), true);
        pdialog.show();
        Thread thr = new Thread(new Runnable() {
            public void run() {
                final Intent intent = new Intent(TableViewActivity.this, ChangeListener.class);
                intent.putExtra("dbupgrade", true);
                intent.putExtra("error", true);
                intent.setAction("hu.kfg.naplo.CHECK_NOW");
                Thread t2 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            upgraderesult = ChangeListener.getEkretaGrades(TableViewActivity.this, intent);
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
                            final Toast t = Toast.makeText(TableViewActivity.this, R.string.emptydb, Toast.LENGTH_SHORT);
                            t.setGravity(Gravity.CENTER, 0, 0);
                            t.show();

                        }
                    });
                } else if (upgraderesult == ChangeListener.UPGRADE_DONE) {
                    doStuff(db);
                    Toast.makeText(TableViewActivity.this, TableViewActivity.this.getString(R.string.title_activity_table_view) + ": " + db.numberOfRows(), Toast.LENGTH_SHORT).show();
                } else if (upgraderesult == ChangeListener.TOKEN_ERROR || upgraderesult == ChangeListener.CREDENTIALS_ERROR) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Toast t = Toast.makeText(TableViewActivity.this, R.string.incorrect_credentials, Toast.LENGTH_SHORT);
                            t.setGravity(Gravity.CENTER, 0, 0);
                            t.show();
                            ((TableLayout) findViewById(R.id.table)).removeAllViews();
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final Toast t = Toast.makeText(TableViewActivity.this, R.string.ohno, Toast.LENGTH_SHORT);
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
    public void onClick(View v) {
        int clicked_id = v.getId();
        if (clicked_id > -1) {
            Grade g = db.getGradeById(clicked_id - 1000000);
            TextView Header2 = new TextView(TableViewActivity.this);
            Header2.setGravity(Gravity.CENTER);
            Header2.setText(String.format(Locale.getDefault(), "%d", g.value));
            Header2.setTextSize(26.0f);
            Header2.setTextColor(Color.parseColor(darkmode ? "#FFFFFF" : "#000000"));
            Header2.setTypeface(null, Typeface.BOLD);
            TextView messageText = new TextView(TableViewActivity.this);
            messageText.setText(Html.fromHtml("<i>&#9658; " + g.subject + "<br/>&#9658; " +
                    g.date + "<br/>&#9658; " + getString(R.string.save_date) + " " + g.save_date + "<br/>&#9658; " + g.teacher + "<br/>&#9658; " +
                    g.description + "</i>"));
            messageText.setGravity(Gravity.START);
            messageText.setPadding(40, 10, 10, 10);
            messageText.setTextAppearance(TableViewActivity.this, android.R.style.TextAppearance_Medium);
            Header2.setPadding(0, 20, 0, 20);
            new AlertDialog.Builder(TableViewActivity.this)
                    .setCustomTitle(Header2)
                    .setPositiveButton(g.value > 3 ? "OK :)" : g.value > 2 ? "OK :/" : "OK :(", null)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setCancelable(true)
                    .setView(messageText)
                    .show();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tablemenu, menu);
        boolean enabled = getPackageManager().getComponentEnabledSetting(new ComponentName(this, TableRedirectActivity.class))
                != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        try {
            menu.getItem(1).setIcon(enabled ? getResources().getDrawable(R.drawable.pin_light) : getResources().getDrawable(R.drawable.pin_dark));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                if (getSystemService(Context.CONNECTIVITY_SERVICE) != null
                        && ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null
                        && ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().isConnected()) {
                    updateDatabase(db);
                } else {
                    Toast t = Toast.makeText(TableViewActivity.this, R.string.no_network_conn, Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.TOP, 0, 30);
                    t.show();
                }
                return true;
            case R.id.infomenu:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(TableViewActivity.this);
                builder1.setMessage(Html.fromHtml(getString(R.string.grades_info_menu)));
                builder1.setCancelable(false);

                builder1.setPositiveButton(
                        "Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert11 = builder1.create();
                alert11.show();
                return true;
            case R.id.shortcut:
                //TODO Warn user about Huawei (or other vendors') bug!
                boolean enabled = getPackageManager().getComponentEnabledSetting(new ComponentName(this, TableRedirectActivity.class))
                        != PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
                try {
                    item.setIcon(!enabled ? getResources().getDrawable(R.drawable.pin_light) : getResources().getDrawable(R.drawable.pin_dark));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                toggleShortCut(enabled);
                Toast.makeText(this, enabled ? R.string.shortcut_off : R.string.shortcut_on, Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private View monthSpelled(Date d) {
        String month_spelled = "-";
        int mo = 0;
        try {
            month_spelled = new SimpleDateFormat("MMM", Locale.getDefault()).format(d);
            mo = Integer.valueOf(new SimpleDateFormat("MM", Locale.getDefault()).format(d));
        } catch (Exception e) {
            e.printStackTrace();
        }
        int monthNumber = mo >= 9 ? mo - 9 : mo + 3;
        TextView Values = new TextView(TableViewActivity.this);
        Values.setPadding(applyDimension(6),
                applyDimension(7.5f),
                applyDimension(6),
                applyDimension(3));
        Values.setGravity(Gravity.CENTER);
        Values.setTextSize(12.0f);
        Values.setTextColor(Color.parseColor("#FFFFFF"));
        Values.setTypeface(null, Typeface.ITALIC);
        Values.setText(month_spelled);
        Values.setOnClickListener(TableViewActivity.this);
        Values.setHeight(applyDimension(VIEW_HEIGHT));
        Values.setBackground(createBackground(monthNumber, 0));
        return Values;
    }

    int applyDimension(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    void multiGradeListener(List<Grade> grades, double value) {
        TextView Header2 = new TextView(TableViewActivity.this);
        Header2.setGravity(Gravity.CENTER);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < grades.size(); i++) {
            text.append(grades.get(i).value);
            if (i + 1 < grades.size())
                text.append(", ");
        }
        Header2.setText(text.toString());
        Header2.setTextSize(26.0f);
        Header2.setTextColor(Color.parseColor("#FFFFFF"));
        Header2.setTypeface(null, Typeface.BOLD);
        TextView messageText = new TextView(TableViewActivity.this);
        messageText.setText(Html.fromHtml("<i>&#9658; " + grades.get(0).subject + "<br/>&#9658; " + grades.get(0).date + "<br/>&#9658; " + TableViewActivity.this.getString(R.string.save_date) + " " + grades.get(0).save_date + "<br/>&#9658; " + grades.get(0).teacher + "<br/>&#9658; " + grades.get(0).description + "</i>"));
        messageText.setGravity(Gravity.START);
        messageText.setPadding(40, 10, 10, 10);
        messageText.setTextAppearance(TableViewActivity.this, android.R.style.TextAppearance_Medium);
        Header2.setPadding(0, 20, 0, 20);
        new AlertDialog.Builder(TableViewActivity.this)
                .setCustomTitle(Header2)
                .setPositiveButton(value >= 4 ? "OK :)" : value >= 3 ? "OK :/" : "OK :(", null)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setCancelable(true)
                .setView(messageText)
                .show();
    }

    void toggleShortCut(boolean enabledNow) {
        final PackageManager p = getPackageManager();
        ComponentName componentName = new ComponentName(this, TableRedirectActivity.class);
        p.setComponentEnabledSetting(componentName, enabledNow ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        //Try as a possible workaround on Huawei EMUI devices
        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(2000);
                ComponentName main = new ComponentName(TableViewActivity.this, MainActivity.class);
                p.setComponentEnabledSetting(main, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            }
        }).start();
    }

    Drawable createBackground(int monthNumber, int mode) {
        int[] colors = new int[]{Color.parseColor("#99F8C471"), Color.parseColor("#99F39C12"), Color.parseColor("#99B9770E"),
                Color.parseColor("#99AED6F1"), Color.parseColor("#995DADE2"), Color.parseColor("#992E86C1"),
                Color.parseColor("#9982E0AA"), Color.parseColor("#992ECC71"), Color.parseColor("#99239B56"),
                Color.parseColor("#999B59B6"), Color.parseColor("#99E74C3C"), Color.parseColor("#00000000")};
        int strokeColor = getResources().getColor(android.R.color.darker_gray);
        GradientDrawable background = new GradientDrawable();
        background.setColor(colors[monthNumber < 10 || monthNumber > 11 ? monthNumber % 10 : monthNumber]);
        background.setShape(GradientDrawable.RECTANGLE);
        background.setStroke(3, strokeColor);
        LayerDrawable layerDrawable = new LayerDrawable(new Drawable[]{background});
        switch (mode) {
            case 1:
                layerDrawable.setLayerInset(0, -3, 0, -3, 0);
                break;
            case 2:
                layerDrawable.setLayerInset(0, -3, 0, 0, 0);
                break;
            case 3:
                layerDrawable.setLayerInset(0, 0, 0, 0, 0);
                break;
            case 0:
                layerDrawable.setLayerInset(0, 0, 0, -3, 0);
                break;
            default:
                break;
        }
        return layerDrawable;
    }

}
