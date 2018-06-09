package hu.kfg.naplo;

import java.text.SimpleDateFormat;
import java.util.Date;

class Substitution {

    boolean today;
    int period;
    boolean over;
    int room;
    String group;
    String subject;
    String teacher;
    String comment;

    Substitution(String t) {
        teacher = t;
    }

    void setTime(int per, boolean tod) {
        period = per;
        today = tod;
        over = tod && Integer.valueOf(new SimpleDateFormat("HHmm").format(new Date())) < (per + 7) * 100 + 45;
    }

    void setTeacher(String t) {
        teacher = t;
    }

    void setRoom(int r) {
        room = r;
    }

    void setSubject(String subject) {
        this.subject = subject;
    }

    void setComment(String c) {
        comment = c;
    }

    void setGroup(String gr) {
        group = gr;
    }

    String getGroup() {
        return group;
    }

    boolean isOver() {
        return over;
    }

    String getTeacher() {
        return teacher;
    }

    /**
     *
     * @param format R for room,
     *               C0 for full comment, C10 to limit to 10 characters,
     *               G for group,
     *               T for teacher,
     *               S for subject,
     *               DD for a * mark if tomorrow
     * @return formatted text to output
     */
    String toString(String format) {
        if (format == null || format.length() < 1) return null;
        format = format.replace("R",""+room);
        format = format.replace("DD","*");
        format = format.replace("G",group);
        format = format.replace("S",subject);
        format = format.replace("T",teacher);
        format = format.replace("C10",comment.substring(0,10)+"…");
        format = format.replace("C0",comment);
        return format;

    }
}
