package hu.kfg.naplo;

import android.support.annotation.NonNull;

import java.util.Date;

public class Event {

    private Date start;
    private Date end;
    private String name;

    public Event(@NonNull String name, @NonNull Date start, @NonNull Date end) {
        this.name = name;
        this.start = start;
        this.end = end;
    }

    public Date getStart() {
        return start;
    }

    public Date getEnd() {
        return end;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "Info: " + name + ":" + start + "-" + end;
    }
}
