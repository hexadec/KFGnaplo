package hu.kfg.naplo;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Lesson {

    String subject;
    String teacher;
    String topic;
    int room;
    Date from;
    Date to;
    String group;
    int id;
    byte period;
    String subjectCat;

    public Lesson(String subject, String teacher, int room, Date from, Date to, String group, byte period) {
        this.subject = subject;
        this.teacher = teacher;
        this.room = room;
        this.from = from;
        this.to = to;
        this.group = group;
        this.period = period;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void setSubjectCat(final String subjectCat) {
        if (subjectCat == null || subjectCat.equalsIgnoreCase("Na")) {
            if (subject.equalsIgnoreCase("tör")) {
                this.subjectCat = "Történelem";
            } else {
                this.subjectCat = "";
            }
        } else if (subjectCat.equalsIgnoreCase("Filozófia") && subject.equalsIgnoreCase("tok")) {
            this.subjectCat = "TOK (Theory of knowledge)";
        } else {
            this.subjectCat = subjectCat;
        }
    }

    public void addID(int id) {
        this.id = id;
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
                .append(dayMonth.format(from))
                .append(" (")
                .append(time.format(from))
                .append('-')
                .append(time.format(to))
                .append(')').toString();
    }
}
