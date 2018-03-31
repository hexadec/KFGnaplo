package hu.kfg.naplo;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

public class CheckerJobCreator implements JobCreator {

    @Override
    @Nullable
    public Job create(@NonNull String tag) {
        switch (tag) {
            case CheckerJob.TAG:
                return new CheckerJob();
            default:
                return null;
        }
    }
}
