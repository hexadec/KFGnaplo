package hu.kfg.naplo;


import java.io.Serializable;

public class Grade implements Serializable{
    private static final long serialVersionUID = Integer.MAX_VALUE;
    byte value;
    String date;
    String save_date;
    String teacher;
    String subject;
    String description;
    transient int id;

    public Grade(byte value) {
        this.value = value;
    }

    public boolean addDescription(String s) {
        boolean returnv = description != null;
        if (s == null || s.length() < 1) {
            description = "";
        } else {
            description = s;
        }
        return returnv;
    }

    public void addID(int i) {
        id = i;
    }

    public boolean addSubject(String s) {
        boolean returnv = subject != null;
        subject = s;
        return returnv;
    }

    public boolean addTeacher(String s) {
        boolean returnv = teacher != null;
        teacher = s;
        return returnv;
    }

    public boolean addDate(String s) {
        boolean returnv = date != null;
        date = s;
        return returnv;
    }

    public boolean addSaveDate(String s) {
        boolean returnv = save_date != null;
        save_date = s;
        return returnv;
    }

    public String getNotificationFormat() {
        StringBuilder builder = new StringBuilder();
        builder.append(subject);
        builder.append(": ");
        builder.append(value);
        builder.append(" (");
        builder.append(description.length() > 21 ? description.substring(0, 20) + "…" : description);
        builder.append(')');
        return builder.toString();
    }

    @Override
    public String toString() {
        return value + "/" + subject + "/" + teacher + "/" + date + "/" + description + "/" + save_date;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Grade && toString().equals(obj.toString());
    }
}
