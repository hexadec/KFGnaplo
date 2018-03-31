package hu.kfg.naplo;

import android.app.Application;
import android.content.Context;

import com.evernote.android.job.JobManager;

/**
 * Created by cseha on 2018.03.31..
 */

public class App extends Application {

    private static App instance;

    @Override
    public void onCreate() {
        instance = this;
        super.onCreate();
        JobManager.create(this).addJobCreator(new CheckerJobCreator());
    }

    public static App getInstance() {
        return instance;
    }

    public static Context getContext(){
        return instance;
    }
}
