package hu.kfg.naplo;

import android.util.Log;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Absence {

    String mode;
    String subject;
    String teacher;
    String justificationState;
    Date dayOfAbsence;
    Date dayOfRegister;
    byte period;

    public static final SimpleDateFormat absenceFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH);

    public Absence(String mode, String subject, String teacher, String justificationState, String doa, String dor, byte period) {
        this.mode = mode;
        this.subject = subject;
        this.teacher = teacher;
        this.justificationState = justificationState;
        this.period = period;
        try {
            dayOfAbsence = absenceFormat.parse(doa);
            String dorSub = dor.split("\\.")[0];
            dayOfRegister = absenceFormat.parse(dor);
        } catch (Exception e) {
            Log.e("Absence", "Faulty date: " + dor);
            e.printStackTrace();
        }

        try {
            Field[] fields = getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                if (field.get(this) instanceof String) {
                    String f = ((String) field.get(this));
                    if (f == null || f.equalsIgnoreCase("Na")) {
                        field.set(this, "---");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Absence(String mode, String subject, String teacher, String justificationState, Date doa, Date dor, byte period) {
        this.mode = mode;
        this.subject = subject;
        this.teacher = teacher;
        this.justificationState = justificationState;
        this.dayOfAbsence = doa;
        this.dayOfRegister = dor;
        this.period = period;
    }
}
