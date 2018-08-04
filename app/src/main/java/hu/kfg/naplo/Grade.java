package hu.kfg.naplo;


public class Grade extends Object{
    byte value;
    String date;
    String teacher;
    String subject;
    String description;
    int id;

    public Grade(byte value) {
        this.value = value;
    }

    public boolean addDescription(String s) {
        boolean returnv = description != null;
        description = s;
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

    @Override
    public String toString() {
        return value + "/" + subject + "/" + teacher + "/" + date + "/" + description;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Grade && toString().equals(obj.toString());
    }
}
