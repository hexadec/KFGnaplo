package hu.kfg.naplo;

import android.content.Context;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Substitution extends Object {

    boolean today;
    int period;
    boolean over = false;
    int room;
    String group;
    String subject;
    String teacher;
    String comment;
    String missing;
    private Context context;

    Substitution(Context con, String t) {
        teacher = t;
        context = con;
    }

    Substitution(Context con) {
        context = con;
    }

    void setTime(int per, boolean tod) {
        period = per;
        today = tod;
        if (per == -1) {
            over = true;
            return;
        }
        over = tod && (Integer.valueOf(new SimpleDateFormat("HHmm").format(new Date())) > (per + 7) * 100 + 45);
    }

    void setTeacher(String t) {
        teacher = t;
    }

    void setRoom(int r) {
        room = r;
    }

    void setSubject(String subject) {
        if (subject.length() < 2) {
            this.subject = context.getString(R.string.lyukasora);
        } else {
            this.subject = subject;
        }
    }

    void setComment(String c) {
        comment = c;
    }

    void setGroup(String gr) {
        group = gr;
    }

    void setMissingTeacher(String name) {
        missing = name;
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

    String getMissingTeacher() {
        return missing;
    }

    /**
     * @param format R for -room,
     *               CC for full comment, C8 to limit to 8 characters,
     *               G for group,
     *               T for teacher,
     *               MM for missing teacher
     *               S for subject,
     *               DD for a * mark if tomorrow,
     *               P for period
     * @return formatted text to output
     */
    String toString(String format) {
        if (format == null || format.length() < 1) return null;
        if (room != 0) format = format.replace("R", "/" + room);
        else format = format.replace("R","");
        format = format.replace("DD", today?"":"*");
        format = format.replace("G", group);
        format = format.replace("P", "" + period);
        format = format.replace("S", subject);
        format = format.replace("T", teacher);
        format = format.replace("MM", missing);
        try {
            format = format.replace("C8", "(" + comment.substring(0, 8) + "…)");
        } catch (Exception e) {
            format = format.replace("C8", "CC");
        }
        if (comment.length() > 0)
            format = format.replace("CC", "(" + comment + ")");
        else
            format = format.replace("CC","");
        return format;

    }
}
