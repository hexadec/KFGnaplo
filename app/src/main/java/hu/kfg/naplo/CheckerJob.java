package hu.kfg.naplo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

public class CheckerJob extends Job {

    public static final String TAG = "mainjob";
    public static int MINUTE = 60000;

    @Override
    @NonNull
    protected Result onRunJob(Params params) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        ChangeListener.onRunJob(App.getContext(), new Intent("hu.kfg.naplo.CHECK_NOW").putExtra("runnomatterwhat", true).putExtra("error",true));
        scheduleJob();
        return Result.SUCCESS;
    }

    static void scheduleJob() {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(App.getContext());
        long repeat = Long.valueOf(pref.getString("auto_check_interval", "300")) * MINUTE;
        int jobId = new JobRequest.Builder(CheckerJob.TAG)
                .setExecutionWindow(repeat-MINUTE*20, repeat+MINUTE*40)
                .setBackoffCriteria(5_000L, JobRequest.BackoffPolicy.EXPONENTIAL)
                .setRequiresDeviceIdle(false)
                .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                .setRequirementsEnforced(false)
                .setUpdateCurrent(true)
                .build()
                .schedule();
    }

    static void runJobImmediately() {
        new JobRequest.Builder(CheckerJob.TAG)
                .startNow()
                .build()
                .schedule();
    }
}
