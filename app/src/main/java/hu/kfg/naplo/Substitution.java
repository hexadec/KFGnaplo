package hu.kfg.naplo;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Substitution extends Object {

    private boolean today;
    private int period;
    private boolean over = false;
    private int room;
    private String group;
    private String subject;
    private String teacher;
    private String comment;
    private String missing;
    private Context context;

    public Substitution(Context con) {
        context = con;
    }

    public void setTime(int per, boolean tod) {
        period = per;
        today = tod;
        if (per == -1) {
            over = true;
            return;
        }
        over = tod && (Integer.valueOf(new SimpleDateFormat("HHmm").format(new Date())) > (per + 7) * 100 + 45);
    }

    public void setTeacher(String t) {
        teacher = t;
    }

    public void setRoom(int r) {
        room = r;
    }

    public void setSubject(String subject) {
        if (subject.length() < 2) {
            this.subject = context.getString(R.string.lyukasora);
        } else {
            this.subject = subject;
        }
    }

    public void setComment(String c) {
        comment = c;
        if (comment.contains("megtartja")) {
            subject = "??";
        }
    }

    public void setGroup(String gr) {
        group = gr;
    }

    public void setMissingTeacher(String name) {
        missing = name;
    }

    public String getGroup() {
        return group;
    }

    public boolean isOver() {
        return over;
    }

    /**
     *
     * @return an integer value based on which the lessons can be ordered.
     * The value takes day and period into account.
     */
    public int getTimeValue() {
        int val = period;
        val += today ? 0 : 10;
        return val;
    }

    public String getTeacher() {
        return teacher;
    }

    public String getSubject() {
        return subject;
    }

    /**
     * @param format RR for -room,
     *               CC for full comment, C9 to limit to 9 characters,
     *               GG for group,
     *               TE for teacher,
     *               MT for missing teacher
     *               SS for subject,
     *               DD for a * mark if tomorrow,
     *               PP for period
     * @return formatted text to output
     */
    public String toString(String format) {
        if (format == null || format.length() < 1) return null;
        if (room != 0) format = format.replace("RR", "/" + room);
        else format = format.replace("RR", "");
        format = format.replace("DD", today ? "" : "*");
        format = format.replace("GG", group);
        format = format.replace("PP", "" + period);
        format = format.replace("SS", subject);
        format = format.replace("TE", subject.equals("??") ? missing : teacher);
        format = format.replace("MT", missing);
        try {
            format = format.replace("C9", "(" + comment.substring(0, 9) + "…)");
        } catch (Exception e) {
            format = format.replace("C9", "CC");
        }
        if (comment.length() > 0)
            format = format.replace("CC", "(" + comment + ")");
        else
            format = format.replace("CC", "");
        return format;

    }

    @Override
    public String toString() {
        return toString("Group: GG/Stand-in: TE/Missing: MT/Subject: SS/When: PPDD/Comment: CC/Room: RR");
    }
}
