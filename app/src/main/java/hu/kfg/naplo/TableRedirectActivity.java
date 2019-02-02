package hu.kfg.naplo;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;

public class TableRedirectActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i("TableRedirect", "Activity started");
        startActivity(new Intent(this, TableViewActivity.class));
        finish();
        super.onCreate(savedInstanceState);
    }

}
