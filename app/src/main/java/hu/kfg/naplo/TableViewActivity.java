package hu.kfg.naplo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.text.Html;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.Toast;

public class TableViewActivity extends Activity {

    DBHelper db;
    int upgraderesult = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_view);
        TableLayout table = (TableLayout) findViewById(R.id.table);
        db = new DBHelper(this);
        if (db.numberOfRows()<1) {
            updateDatabase(this, db);
        } else {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setMessage(Html.fromHtml(getString(R.string.update_grades)));
            builder1.setCancelable(false);

            builder1.setPositiveButton(
                    R.string.update,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                            updateDatabase(TableViewActivity.this, db);
                            if (upgraderesult==4) {
                                Toast t = new Toast(TableViewActivity.this);
                                t.setText(R.string.emptydb);
                                t.setDuration(Toast.LENGTH_SHORT);
                                t.setGravity(Gravity.CENTER,0,0);
                                t.show();

                            }
                        }
                    });
            builder1.setNeutralButton(
                            R.string.not_now,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
    }

    void updateDatabase(final Context context, DBHelper db) {
        ProgressDialog pdialog = ProgressDialog.show(TableViewActivity.this, "",
                getString(R.string.upgrading), true);
        Thread t = new Thread(new Runnable() {
            public void run(){
                Intent intent = new Intent(TableViewActivity.this, ChangeListener.class);
                intent.putExtra("dbupgrade",true);
                upgraderesult = ChangeListener.doCheck(context,intent);
            }

        });
        upgraderesult = -10;
        t.start();
        try {
            t.join(20000);
        } catch (Exception e) {

        }
        pdialog.cancel();
        Toast.makeText(context, ""+upgraderesult+"/"+db.numberOfRows(), Toast.LENGTH_SHORT).show();
    }

}
