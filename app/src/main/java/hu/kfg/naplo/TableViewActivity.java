package hu.kfg.naplo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.ResourceBusyException;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.app.Activity;
import android.text.Html;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class TableViewActivity extends Activity {

    DBHelper db;
    int upgraderesult = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_view);
        db = new DBHelper(this);
        if (db.numberOfRows() < 1) {
            updateDatabase(db);
            if (upgraderesult == 4) {
                Toast t = new Toast(TableViewActivity.this);
                t.setText(R.string.emptydb);
                t.setDuration(Toast.LENGTH_SHORT);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
            } else if (upgraderesult == 3) {
                doStuff(db);
            } else if (upgraderesult == 5) {
                Toast t = new Toast(TableViewActivity.this);
                t.setText(R.string.ohno);
                t.setDuration(Toast.LENGTH_SHORT);
                t.setGravity(Gravity.CENTER, 0, 0);
                t.show();
            }
        } else {
            if (((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() != null && ((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().isConnected()) {
                AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
                builder1.setMessage(Html.fromHtml(getString(R.string.update_grades)));
                builder1.setCancelable(false);

                builder1.setNeutralButton(
                        R.string.update,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                updateDatabase(db);
                                if (upgraderesult == 4) {
                                    Toast t = new Toast(TableViewActivity.this);
                                    t.setText(R.string.emptydb);
                                    t.setDuration(Toast.LENGTH_SHORT);
                                    t.setGravity(Gravity.CENTER, 0, 0);
                                    t.show();
                                } else if (upgraderesult == 3) {
                                    doStuff(db);
                                } else if (upgraderesult == 5) {
                                    Toast t = new Toast(TableViewActivity.this);
                                    t.setText(R.string.ohno);
                                    t.setDuration(Toast.LENGTH_SHORT);
                                    t.setGravity(Gravity.CENTER, 0, 0);
                                    t.show();
                                }
                            }
                        });
                builder1.setPositiveButton(
                        R.string.not_now,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                doStuff(db);
                            }
                        });
                AlertDialog alert11 = builder1.create();
                alert11.show();
            } else {
                doStuff(db);
            }
        }
    }

    void doStuff(DBHelper db) {
        TableLayout table = (TableLayout) findViewById(R.id.table);
        ArrayList<String> subjects = db.getSubjects();
        for(int i = -1; i < subjects.size(); i++) {
            TableRow row = new TableRow(this);

            TableLayout.LayoutParams lp = new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT);
            row.setLayoutParams(lp);

            row.setPadding(15, 3, 15, 3);

            if (i==-1) row.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));


            TextView Header = new TextView(this);

            Header.setGravity(Gravity.CENTER);
            if (i==-1) {
                Header.setText(R.string.subjects);
                Header.setTextSize(18.0f);
                Header.setTextColor(Color.parseColor("#FFFFFF"));
            }
            else {
                Header.setText(subjects.get(i).length()>20?subjects.get(i).substring(0,17)+"…":subjects.get(i));
                Header.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                Header.setTextSize(18.0f);
            }
            Header.setPadding(15, 0, 15, 0);
            Header.setTypeface(null, Typeface.BOLD);

            row.addView(Header);
            if (i!=-1) {
                ArrayList<Short> grades = db.getSubjectGrades(subjects.get(i));
                for (int j = 0; j < grades.size(); j++) {
                    TextView Values = new TextView(this);
                    Values.setPadding(15, 0, 15, 0);
                    Values.setGravity(Gravity.CENTER);
                    Values.setTextSize(16.0f);
                    Values.setTextColor(Color.parseColor("#FFFFFF"));
                    Values.setTypeface(null, Typeface.ITALIC);
                    Values.setText(""+grades.get(j));
                    row.addView(Values);
                }
            }
            table.addView(row);
        }
    }

    void updateDatabase(DBHelper db) {
        ProgressDialog pdialog = ProgressDialog.show(TableViewActivity.this, "",
                getString(R.string.upgrading), true);
        Thread t = new Thread(new Runnable() {
            public void run(){
                Intent intent = new Intent(TableViewActivity.this, ChangeListener.class);
                intent.putExtra("dbupgrade",true);
                upgraderesult = ChangeListener.doCheck(TableViewActivity.this,intent);
            }

        });
        upgraderesult = -10;
        t.start();
        try {
            t.join(20000);
        } catch (Exception e) {

        }
        pdialog.cancel();
        Toast.makeText(TableViewActivity.this, ""+upgraderesult+"/rows affected: "+db.numberOfRows(), Toast.LENGTH_SHORT).show();
    }

}
