package hu.kfg.naplo;

import android.os.Bundle;
import android.app.Activity;
import android.widget.TableLayout;

public class TableViewActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_view);
        TableLayout table = (TableLayout) findViewById(R.id.table);

    }

}
