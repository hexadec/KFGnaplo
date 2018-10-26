package hu.kfg.naplo;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Lesson {

    String subject;
    String teacher;
    String topic;
    int room;
    Date from;
    Date to;
    Date when;
    String group;

    public Lesson(String subject, String teacher, int room, Date from, Date to, Date when, String group) {
        this.subject = subject;
        this.teacher = teacher;
        this.room = room;
        this.from = from;
        this.to = to;
        this.when = when;
        this.group = group;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    @Override
    public String toString() {
        SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat dayMonth = new SimpleDateFormat("MMM dd", Locale.getDefault());
        StringBuilder sb = new StringBuilder();
        return sb.append(subject)
                .append('/')
                .append(teacher)
                .append(':')
                .append(room)
                .append('@')
                .append(dayMonth.format(when))
                .append(" (")
                .append(time.format(from))
                .append('-')
                .append(time.format(to))
                .append(')').toString();
    }
}
