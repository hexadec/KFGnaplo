package hu.kfg.naplo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.app.Activity;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_view);
        db = new DBHelper(this);
        if (db.numberOfRows() < 1) {
            updateDatabase(db);
        } else {
            doStuff(db);
        }
    }

    void doStuff(DBHelper db) {
        TableLayout table = (TableLayout) findViewById(R.id.table);
        table.removeAllViews();
        table.setMeasureWithLargestChildEnabled(true);
        ArrayList<String> subjects = db.getSubjects();
        for (int i = -1; i < subjects.size(); i++) {
            TableRow row = new TableRow(this);

            TableLayout.LayoutParams lp = new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);

            row.setPadding(15, 0, 15, 0);

            if (i == -1)
                row.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));


            TextView Header = new TextView(this);

            Header.setGravity(Gravity.CENTER);
            if (i == -1) {
                Header.setText(R.string.subjects);
                Header.setTextSize(18.0f);
                Header.setTextColor(Color.parseColor("#FFFFFF"));
            } else {
                Header.setText(subjects.get(i).length() > 20 ? subjects.get(i).substring(0, 17) + "…" : subjects.get(i));
                Header.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                Header.setTextSize(18.0f);
                Header.setBackground(getResources().getDrawable(R.drawable.month_single));
            }
            Header.setPadding((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()),
                    (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()),
                    (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics()),
                    (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
            Header.setHeight((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, VIEW_HEIGHT, getResources().getDisplayMetrics()));
            Header.setTypeface(null, Typeface.BOLD);

            row.addView(Header);
            if (i != -1) {
                List<Grade> grades = db.getSubjectGradesG(subjects.get(i));
                double avg = 0;
                for (Grade g : grades) {
                    avg += g.value;
                }
                avg /= grades.size();
                int month = 0;
                boolean which = false;
                for (int j = -1; j < grades.size(); j++) {
                    SimpleDateFormat s = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                    SimpleDateFormat m = new SimpleDateFormat("MM", Locale.ENGLISH);
                    int mo = 0;
                    int mo2 = 0;
                    try {
                        Date d = s.parse(grades.get(j).date);
                        mo = Integer.valueOf(m.format(d));
                        if ((mo != month) || (j + 1 == grades.size() && mo != month)) {
                            Log.w("month","m");
                            row.addView(monthSpelled(d, which));
                        }
                        Date dd = s.parse(grades.get(j + 1).date);
                        mo2 = Integer.valueOf(m.format(dd));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    TextView Values = new TextView(this);
                    Values.setPadding((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()),
                            (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()),
                            (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()),
                            (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics()));
                    Values.setGravity(Gravity.CENTER);
                    Values.setTextSize(18.0f);
                    Values.setTextColor(Color.parseColor("#FFFFFF"));
                    Values.setTypeface(null, Typeface.ITALIC);
                    Values.setText(j == -1 ? new DecimalFormat("#.##").format(avg) : "" + grades.get(j).value);
                    Values.setId(j != -1 ? grades.get(j).id + 1000000 : grades.get(j + 1).id - 30000);
                    Values.setOnClickListener(this);
                    Values.setHeight((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, VIEW_HEIGHT, getResources().getDisplayMetrics()));
                    if ((j + 1 == grades.size() && mo == month) || (mo == month && mo != mo2)) {
                        if (which) {
                            Values.setBackground(getResources().getDrawable(R.drawable.month_end));
                        } else {
                            Values.setBackground(getResources().getDrawable(R.drawable.month_end2));
                        }
                        which = !which;
                    } else if ((mo != month && mo != mo2) || (j + 1 == grades.size() && mo != month)) {
                        if (which) {
                            Values.setBackground(getResources().getDrawable(R.drawable.month_end));
                        } else {
                            Values.setBackground(getResources().getDrawable(R.drawable.month_end2));
                        }
                        which = !which;
                    } else if (j==-1) {
                        if (which) {
                            Values.setBackground(getResources().getDrawable(R.drawable.month_single));
                        } else {
                            Values.setBackground(getResources().getDrawable(R.drawable.month_single2));
                        }
                        which = !which;
                    } else {
                        if (which) {
                            Values.setBackground(getResources().getDrawable(R.drawable.cell));
                        } else {
                            Values.setBackground(getResources().getDrawable(R.drawable.cell2));
                        }
                    }
                    row.addView(Values);
                    month = mo;
                }
            } else {
                TextView Header2 = new TextView(this);
                Header2.setGravity(Gravity.CENTER);
                Header2.setText(R.string.average);
                Header2.setTextSize(18.0f);
                Header2.setTextColor(Color.parseColor("#FFFFFF"));
                Header2.setTypeface(null, Typeface.BOLD);
                row.addView(Header2);
            }
            table.addView(row);
        }
    }

    void updateDatabase(DBHelper db) {
        if (PreferenceManager.getDefaultSharedPreferences(TableViewActivity.this).getString("url","").length()<MainActivity.URL_MIN_LENGTH) {
            Toast t = Toast.makeText(TableViewActivity.this, R.string.gyia_expired_or_faulty, Toast.LENGTH_LONG);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
            finish();
            return;
        }
        ProgressDialog pdialog = ProgressDialog.show(TableViewActivity.this, "",
                getString(R.string.upgrading), true);
        Thread thr = new Thread(new Runnable() {
            public void run() {
                Intent intent = new Intent(TableViewActivity.this, ChangeListener.class);
                intent.putExtra("dbupgrade", true);
                intent.putExtra("error", true);
                intent.setAction("hu.kfg.naplo.CHECK_NOW");
                upgraderesult = ChangeListener.doCheck(TableViewActivity.this, intent);
            }

        });
        upgraderesult = -10;
        thr.start();
        try {
            thr.join(20000);
        } catch (Exception e) {

        }
        pdialog.cancel();
        Toast.makeText(TableViewActivity.this, "" + db.numberOfRows(), Toast.LENGTH_SHORT).show();
        if (upgraderesult == 4) {
            Toast t = Toast.makeText(TableViewActivity.this, R.string.emptydb, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
        } else if (upgraderesult == 3) {
            doStuff(db);
        } else if (upgraderesult == -7) {
            Toast t = Toast.makeText(TableViewActivity.this, R.string.gyia_expired_or_faulty, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
        } else {
            Toast t = Toast.makeText(TableViewActivity.this, R.string.ohno, Toast.LENGTH_SHORT);
            t.setGravity(Gravity.CENTER, 0, 0);
            t.show();
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub

        int clicked_id = v.getId();
        if (clicked_id > -1) {
            Grade g = db.getGradeById(clicked_id - 1000000);
            TextView Header2 = new TextView(this);
            Header2.setGravity(Gravity.CENTER);
            Header2.setText("" + g.value);
            Header2.setTextSize(26.0f);
            Header2.setTextColor(Color.parseColor("#FFFFFF"));
            Header2.setTypeface(null, Typeface.BOLD);
            TextView messageText = new TextView(this);
            messageText.setText(Html.fromHtml("<i>&#9658; " + g.subject + "<br/>&#9658; " + g.date + "<br/>&#9658; " + g.teacher + "<br/>&#9658; " + g.description + "</i>"));
            messageText.setGravity(Gravity.LEFT);
            messageText.setPadding(40, 10, 10, 10);
            messageText.setTextAppearance(this, android.R.style.TextAppearance_Medium);
            Header2.setPadding(0, 20, 0, 20);
            new AlertDialog.Builder(this)
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
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.refresh:
                if (getSystemService(Context.CONNECTIVITY_SERVICE) != null
                        && ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null
                        && ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().isConnected()) {
                    updateDatabase(db);
                } else {
                    Toast t = Toast.makeText(this, R.string.no_network_conn, Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.TOP, 0, 30);
                    t.show();
                }
                return true;
            case R.id.infomenu:
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
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
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private View monthSpelled(Date d, boolean whichColor) {
        String month_spelled = "-";
        try {
            month_spelled = new SimpleDateFormat("MMM", Locale.getDefault()).format(d);
        } catch (Exception e) {
            e.printStackTrace();
        }
        TextView Values = new TextView(this);
        Values.setPadding((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics()),
                (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 7.5f, getResources().getDisplayMetrics()),
                (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6, getResources().getDisplayMetrics()),
                (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics()));
        Values.setGravity(Gravity.CENTER);
        Values.setTextSize(12.0f);
        Values.setTextColor(Color.parseColor("#FFFFFF"));
        Values.setTypeface(null, Typeface.ITALIC);
        Values.setText(month_spelled);
        Values.setOnClickListener(this);
        Values.setHeight((int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, VIEW_HEIGHT, getResources().getDisplayMetrics()));
        if (whichColor) {
            Values.setBackground(getResources().getDrawable(R.drawable.month_start));
        } else {
            Values.setBackground(getResources().getDrawable(R.drawable.month_start2));
        }
        return Values;
    }

}
