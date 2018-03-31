package hu.kfg.naplo;

import android.app.KeyguardManager;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JobManagerService extends JobService {

    /*public static int MINUTE = 60000;

    public static final int NIGHTMODE_START = 2230;
    public static final int NIGHTMODE_STOP = 530;*/

    @Override
    public boolean onStartJob(JobParameters params) { //schedule before anything else
        /*
        Intent i = new Intent("hu.kfg.naplo.CHECK_NOW");
        i.putExtra("runnomatterwhat", true);
        sendBroadcast(i);
        scheduleJob(getApplicationContext(), false);
        jobFinished(params, false);
        */
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

   /*  public static void scheduleJob(Context context, boolean nighttime) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        long repeat = Long.valueOf(pref.getString("auto_check_interval", "300")) * MINUTE;
        // ignored repeat /= nighttime&&Build.VERSION.SDK_INT>=26?8:1;
        ComponentName serviceComponent = new ComponentName(context, JobManagerService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(repeat - MINUTE * 20);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        try {
            jobScheduler.cancelAll();
        } catch (Exception e) {
        }
        SystemClock.sleep(100);
        jobScheduler.schedule(builder.build());

    }
*/

}
