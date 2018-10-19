package hu.kfg.naplo;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;

public class TableRedirectActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startActivity(new Intent(this, TableViewActivity.class));
        super.onCreate(savedInstanceState);
    }

}
