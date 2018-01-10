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
import android.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class JobManagerService extends JobService {

    public static int MINUTE = 60000;

    public static final int NIGHTMODE_START = 2230;
    public static final int NIGHTMODE_STOP = 530;

    @Override
    public boolean onStartJob(JobParameters params) { //schedule before anything else
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getBoolean("nightmode",false)) {
            SimpleDateFormat sdf = new SimpleDateFormat("HHmm", Locale.US);
            int time = Integer.valueOf(sdf.format(new Date()));
            if (time < NIGHTMODE_STOP || time > NIGHTMODE_START) {
                KeyguardManager mKM = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
                if(mKM != null && mKM.inKeyguardRestrictedInputMode() ) {
                    scheduleJob(getApplicationContext(), true);
                    return true;
                } else {
                    PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
                    if (powerManager==null||!powerManager.isInteractive()){ 
                        scheduleJob(getApplicationContext(), true);
                        return true; 
                    }
                }
            }

        }
        scheduleJob(getApplicationContext(), false);
        Intent i = new Intent("hu.kfg.naplo.CHECK_NOW");
        i.putExtra("runnomatterwhat",true);
        sendBroadcast(i);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    public static void scheduleJob(Context context, boolean nighttime) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        long repeate = Long.valueOf(pref.getString("auto_check_interval","300"))*MINUTE;
        repeate /= nighttime&&Build.VERSION.SDK_INT>=26?8:1;
        ComponentName serviceComponent = new ComponentName(context, JobManagerService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, serviceComponent);
        builder.setMinimumLatency(repeate-MINUTE*20);
        builder.setOverrideDeadline(repeate+MINUTE*40);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        jobScheduler.schedule(builder.build());
    }
}
