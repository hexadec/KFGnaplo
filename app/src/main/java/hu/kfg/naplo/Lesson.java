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

    public void setSubjectCat(final String subjectCat, boolean noReplace) {
        if (noReplace) {
            this.subjectCat = subjectCat;
            return;
        }
        if (subjectCat == null || subjectCat.equalsIgnoreCase("Na")) {
            if (subject.equalsIgnoreCase("tör") || subject.equalsIgnoreCase("törE")) {
                this.subjectCat = "Történelem";
            } else if (subject.equalsIgnoreCase("törny")) {
                this.subjectCat = "Történelem szaknyelv";
            } else {
                this.subjectCat = "";
            }
        } else if (subjectCat.equalsIgnoreCase("Filozófia") && subject.equalsIgnoreCase("tok")) {
            this.subjectCat = "TOK (Theory of knowledge)";
        } else if (subject.equalsIgnoreCase("angcc")) {
            this.subjectCat = "angol c.c.";
        } else if (subjectCat.equalsIgnoreCase("némcc")) {
            this.subjectCat = "német c.c.";
        } else if (subject.equalsIgnoreCase("pszi")) {
            this.subjectCat = "Pszichológia";
        } else if (subject.equalsIgnoreCase("ofő")) {
            this.subjectCat = "Osztályfőnöki";
        } else if (subject.equalsIgnoreCase("lab")) {
            this.subjectCat = "Labor";
        } else if (subject.equalsIgnoreCase("föl")) {
            this.subjectCat = "Földrajz";
        } else if (subject.equalsIgnoreCase("fölny")) {
            this.subjectCat = "Földrajz szaknyelv";
        } else if (subject.equalsIgnoreCase("fizny")) {
            this.subjectCat = "Fizika szaknyelv";
        } else if (subject.equalsIgnoreCase("közg")) {
            this.subjectCat = "Közgazdaságtan";
        } else if (subject.equalsIgnoreCase("bio") || subject.equalsIgnoreCase("bioE")) {
            this.subjectCat = "Biológia";
        } else if (subject.equalsIgnoreCase("eti")) {
            this.subjectCat = "Etika";
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
