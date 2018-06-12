package hu.kfg.naplo;

import android.content.*;
import android.preference.*;

import android.net.*;
import android.os.*;
import android.service.notification.StatusBarNotification;
import android.util.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;
import java.io.*;

import android.widget.*;
import android.app.*;

import java.text.*;

public class ChangeListener {
    /*private static */
    static final int NIGHTMODE_START = 2230;
    static final int NIGHTMODE_STOP = 530;
    static final String MODE_TEACHER = "teacher";
    static final String MODE_TRUE = "true";
    static final String MODE_STANDINS = "standins";
    static final String MODE_NAPLO = "naplo";
    static final String MODE_FALSE = "false";

    final static String TAG = "KFGnaplo-check";
    static final int STANDINS_ID = 100;

    static boolean running = false;

    public static void onRunJob(final Context context, final Intent intent) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        final String mode = pref.getString("notification_mode", MODE_FALSE);
        if (mode.equals(MODE_FALSE)) {
            return;
        }
        if (intent != null && intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            //JobManagerService.scheduleJob(context, false);
            CheckerJob.runJobImmediately();
            return;
        }
        if (((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() == null
                || !((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().isConnected()) {
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                switch (mode) {
                    case MODE_TRUE:
                        doStandinsCheck(context, new Intent("hu.kfg.standins.CHECK_NOW").putExtra("runnomatterwhat", true).putExtra("error", true));
                        doCheck(context, intent);
                        break;
                    case MODE_NAPLO:
                        doCheck(context, intent);
                        break;
                    case MODE_TEACHER:
                    case MODE_STANDINS:
                        doStandinsCheck(context, new Intent("hu.kfg.standins.CHECK_NOW").putExtra("runnomatterwhat", true).putExtra("error", true));
                        break;
                    case MODE_FALSE:
                        break;

                }
            }

        }).start();


    }


    public static int doCheck(final Context context, final Intent intent) {
        final Handler showSuccessToast = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
            }
        };
        final SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(context);
        String kfgserver = pref.getString("url", "1");
        if (kfgserver.length() < MainActivity.URL_MIN_LENGTH) {
            if (intent.hasExtra("error") && intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
                showSuccessToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, R.string.insert_code, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return -1;
        }

        String version = "0.0";
        android.content.pm.PackageInfo pInfo = null;
        try {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            version = pInfo.versionName;
        } catch (Exception e) {
        }

        HttpURLConnection urlConnection;
        try {
            URL url = new URL(kfgserver);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; Karinthy Naplo v" + version + ")");
            urlConnection.setInstanceFollowRedirects(true);
        } catch (IOException e) {
            Log.e(TAG, "Cannot load website!");
            if (intent.hasExtra("error") && intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
                showSuccessToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, R.string.cannot_reach_site, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            e.printStackTrace();
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "Unknown error!");
            if (intent.hasExtra("error") && intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
                showSuccessToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            e.printStackTrace();
            return -1;
        }

        StringBuilder sb = new StringBuilder();
        int counter = 0;
        int notesc = 0;
        boolean hasstarted = false;
        boolean hasended = false;
        byte notes[] = new byte[512];
        String subjects[] = new String[512];
        String descriptions[] = new String[512];
        List<Grade> mygrades = new ArrayList<>();
        try {
            if (urlConnection.getResponseCode() % 300 < 100) {
                notifyIfChanged(new int[]{1, 0, 0}, context, "https://naplo.karinthy.hu/", context.getString(R.string.gyia_expired_not));
                Log.w(TAG, urlConnection.getResponseCode() + "/" + urlConnection.getContentLength());
                if (intent.hasExtra("error") && intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
                    showSuccessToast.postAtFrontOfQueue(new Runnable() {
                        public void run() {
                            Toast.makeText(context, R.string.gyia_expired_or_faulty, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                return -7;
            }
            BufferedReader rd = new BufferedReader
                    (new InputStreamReader(urlConnection.getInputStream(), "ISO-8859-2"));
            String line;
            notesc = 0;
            counter = 0;
            Grade grade = new Grade((byte) 0);
            while ((line = rd.readLine()) != null) {
                if (!hasstarted && line.contains("<div data-role=\"content\" style=\"padding: 0px;\">"))
                    hasstarted = true;
                if (line.contains("</article>") && !hasended) {
                    hasended = true;
                    sb.append(line);
                    break;
                }
                if (line.contains("<div class=\"creditbox\"")) {
                    counter = 0;
                }
                if ((counter == 3 || counter == 2) && (line.contains("<div class=\"credit ini_fresh\">") || line.contains("<div class=\"credit ini_credit\">"))) {
                    grade = new Grade(notes[notesc] = (byte) Character.getNumericValue(line.charAt(line.indexOf(">") + 1)));
                    notesc++;
                }
                if ((counter == 4 || counter == 3) && (line.contains("<div class=\"teacher\">"))) {
                    int i = line.indexOf(">");
                    grade.addTeacher(line.substring(i + 1, line.indexOf("<", i)));
                }
                if ((counter == 6 || counter == 7) && (line.contains("<span id=\"stamp_correct_"))) {
                    int i = line.indexOf(">");
                    grade.addDate(line.substring(i + 1, line.indexOf("<", i)));
                }
                if ((counter == 8 || counter == 9) && (line.contains("<div class=\"description\">"))) {
                    int i = line.indexOf(">");
                    String desc;
                    descriptions[notesc - 1] = ((desc = line.substring(i + 1, line.indexOf("<", i))).length() > 21 ? desc.substring(0, 20) + "…" : desc);
                    grade.addDescription(desc);
                }
                if ((counter == 11 || counter == 10) && (line.contains("<div class=\"creditbox_footer\">"))) {
                    int i = line.indexOf(">");
                    grade.addSubject(subjects[notesc - 1] = line.substring(i + 1, line.indexOf("<", i)));
                    mygrades.add(grade);
                    //Log.i("Grades",grade.toString());
                }
                counter++;
                sb.append(line);
            }
            if (intent.hasExtra("dbupgrade")) {
                Log.i("Grades", "Size: " + mygrades.size());
                if (mygrades.size() < 1) return 4;
                if (new DBHelper(context).upgradeDatabase(mygrades) == true) return 3;
                else return 5;
            }
        } catch (Exception e) {
            Log.e(TAG, "Unknown error!");
            if (intent.hasExtra("error") && intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
                showSuccessToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            e.printStackTrace();
            return -1;
        }
        try {
            if (sb.toString().length() < 1000) {
                Log.w(TAG, sb.toString());
                throw new Exception("Content too small \nLength: " + sb.toString().length());
            }
            if (subjects[0] == null || subjects[0].length() < 2)
                throw new IndexOutOfBoundsException("Content too small \nLength: " + sb.toString());
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage());
            Log.w(TAG, "" + (subjects[0] == null));
            if (intent.hasExtra("error") && intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
                showSuccessToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.error_no_grades), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return -1;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            if (intent.hasExtra("error") && intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
                showSuccessToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return -1;
        }
        String[] s = new String[notesc];
        for (int i = 0; i < notesc; i++) {
            s[i] = subjects[i] + ": " + notes[i] + (descriptions[i].length() < 1 ? "" : "  (" + descriptions[i] + ")");
        }

        if (running) {
            Log.w(TAG, "A process is already running");
            return -1;
        }
        running = true;

        final int numofnotes = pref.getInt("numberofnotes", 0);
        final int numofnotes0 = notesc;
        try {

            pref.edit().putInt("numberofnotes", notesc).commit();
            pref.edit().putLong("last_check", System.currentTimeMillis()).commit();
            if (numofnotes0 - numofnotes > 0) {
                int i = numofnotes0 - numofnotes;
                String text = "";
                DBHelper db1 = new DBHelper(context);
                for (int i2 = 0; i2 < i; i2++) {
                    text += s[i2] + ", \n";
                    if (!intent.hasExtra("dbupgrade")) db1.insertGrade(mygrades.get(i2));
                }
                text = text.substring(0, text.length() - 2);
                notifyIfChanged(new int[]{0, pref.getBoolean("vibrate", false) ? 1 : 0, pref.getBoolean("flash", false) ? 1 : 0}, context, kfgserver, text);
                pref.edit().putString("lastSHA", SHA512(notes)).commit();
                running = false;
            } else {
                if (!SHA512(notes).equals(pref.getString("lastSHA", "ABCD"))) {
                    pref.edit().putString("lastSHA", SHA512(notes)).commit();
                    notifyIfChanged(new int[]{0, pref.getBoolean("vibrate", false) ? 1 : 0, pref.getBoolean("flash", false) ? 1 : 0}, context, kfgserver, context.getString(R.string.unknown_change));
                    //If a grade was modified, it's easier to update the whole DB
                    Intent intent2 = new Intent(context, ChangeListener.class);
                    intent2.putExtra("dbupgrade", true);
                    doCheck(context, intent);
                    running = false;
                    return 0;
                } else if (intent.hasExtra("error") && intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
                    showSuccessToast.postAtFrontOfQueue(new Runnable() {
                        public void run() {
                            Toast.makeText(context, context.getString(R.string.no_new_grade) + " " + numofnotes0 + "/" + numofnotes, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                running = false;
                return -1;
            }
        } catch (Exception e) {
            Log.e(TAG, "Unknown error!");
            if (intent.hasExtra("error") && intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
                showSuccessToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            e.printStackTrace();
            running = false;
            return -1;
        }
        running = false;
        return 0;
    }

    public static int doStandinsCheck(final Context context, final Intent intent) {
        final Handler showSuccessToast = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
            }
        };
        final String TAG = "KFGstandins-check";
        final SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(context);
        String classs = pref.getString("class", "noclass");
        String mode = pref.getString("notification_mode", MODE_FALSE);
        if (classs.equals("noclass")) {
            return -1;
        }
        if (classs.length() < 3) {
            showSuccessToast.postAtFrontOfQueue(new Runnable() {
                public void run() {
                    Toast.makeText(context, "Írd be az osztályodat!", Toast.LENGTH_SHORT).show();
                }
            });
            return -1;
        }
        String kfgserver = "https://apps.karinthy.hu/helyettesites/";
		/*String version = "0.0";
		android.content.pm.PackageInfo pInfo = null;
		try {
			pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			version = pInfo.versionName;
		} catch (Exception e){}*/

        HttpURLConnection urlConnection;
        try {
            URL url = new URL(kfgserver);
            urlConnection = (HttpURLConnection) url.openConnection();
            //urlConnection.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; Karinthy Naplo v"+ version + ")");
            urlConnection.setInstanceFollowRedirects(true);
        } catch (IOException e) {
            Log.e(TAG, "Cannot load website!");
            if (intent.hasExtra("error") && intent.getAction().equals("hu.kfg.standins.CHECK_NOW")) {
                showSuccessToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            e.printStackTrace();
            return -1;
        } catch (Exception e) {
            Log.e(TAG, "Unknown error!");
            if (intent.hasExtra("error") && intent.getAction().equals("hu.kfg.standins.CHECK_NOW")) {
                showSuccessToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            e.printStackTrace();
            return -1;
        }


        List<String> cls = new ArrayList<>();
        if (mode.equals(MODE_TEACHER)) {
            try {
                String name = classs.replace(", ", ",").replace(" ,", ",");
                String[] names = name.split(",");
                for (String n : names) {
                    cls.add(n);
                }
            } catch (Exception e) {
                cls.add(classs);
            }
        } else {
            cls.add(classs);
            if ((classs.endsWith("A") || classs.endsWith("B")) && !classs.endsWith(".IB")) {
                int i = Integer.valueOf(classs.split("[.]")[0]);
                if (i < 11) {
                    cls.add(i + ".AB");
                } else {
                    cls.add(i + ".AB");
                    cls.add(i + ".A+");

                }
                Log.d(TAG, i + ".AB");
            } else {
                if (classs.endsWith("C") || classs.endsWith("D")) {
                    int i = Integer.valueOf(classs.split("[.]")[0]);
                    cls.add(i + ".A+");
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        String lessonsToIgnore = pref.getString("ignore_lessons", "semmitsemignoral")
                .replace(", ", ",").replace(" ,", "");
        String ilessons[] = null;
        boolean ignore = false;
        try {
            ilessons = lessonsToIgnore.split(",");
            ignore = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        int day = 0;
        List<Substitution> subs = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader
                    (new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
            String line;
            int counter = 0;
            Substitution sub = new Substitution(context);
            while ((line = reader.readLine()) != null) {

                if (line.contains("live")) {
                    day = 1;
                }
                if (line.contains("live tomorrow")) {
                    day = 2;
                }
                if (line.contains("\"stand_in\"")) {
                    counter = 0;
                    if (sub.getGroup() != null) {
                        subs.add(sub);
                        sub = new Substitution(context);
                    }
                    sub.setTeacher(line.substring(21, line.length() - 5));
                }
                if (line.contains("\"subject\"") && counter == 3) {
                    sub.setSubject(line.substring(20, line.length() - 5));
                }
                if (line.contains("\"comment\"") && counter == 6) {
                    sub.setComment(line.substring(20, line.length() - 5));
                }
                if (line.contains("\"class\"") && counter == 2) {
                    sub.setGroup(line.substring(18, line.length() - 5));
                }
                if (line.contains("\"lesson\"") && counter == 1) {
                    int period = -1;
                    try {
                        period = Integer.valueOf(line.substring(19, line.length() - 6));
                    } catch (Exception e) {
                    }
                    sub.setTime(period, day == 1);
                }
                if (line.contains("\"room\"") && counter == 4) {
                    int room = 0;
                    try {
                        room = Integer.valueOf(line.substring(17, line.length() - 5));
                    } catch (Exception e) {
                    }
                    sub.setRoom(room);
                }
                if (line.contains("\"missing_teacher\"") && counter == 5) {
                    sub.setMissingTeacher(line.substring(28, line.length() - 5));
                }
                counter++;
                sb.append(line);


            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
        pref.edit().putLong("last_check2", System.currentTimeMillis()).commit();
        StringBuilder text = new StringBuilder();
        int numoflessons = 0;
        if (mode.equals(MODE_TEACHER)) {
            for (Substitution sub : subs) {
                for (String cla : cls) {
                    if (cla.equals(sub.getTeacher()) && !sub.isOver()) {
                        text.append("\n");
                        text.append(sub.toString("PDD. S: G C8 R"));
                        Log.d(TAG, sub.toString("PDD. S: G C8 R"));
                        numoflessons++;
                    }
                }
            }
        } else {
            substitutions: for (Substitution sub : subs) {
                for (String cla : cls) {
                    if (cla.equals(sub.getGroup()) && !sub.isOver()) {
                        if (ignore) {
                            for (String toIgnore : ilessons)
                                if (toIgnore.equals(sub.getSubject()))
                                    continue substitutions;
                        }
                        text.append("\n");
                        text.append(sub.toString("PDD. S: T C8 R, G"));
                        Log.d(TAG, sub.toString("PDD. S: T C8 R, G"));
                        numoflessons++;
                    }
                }
            }
        }

        if (pref.getBoolean("onlyonce", true) && pref.getString("last", "nuller").equals(text.toString() + (new SimpleDateFormat("yyyy/DDD", Locale.ENGLISH).format(new Date())))) {
            if (pref.getBoolean("always_notify", false)) {
                notifyIfStandinsChanged(new int[]{3, pref.getBoolean("vibrate", false) ? 1 : 0, pref.getBoolean("flash", false) ? 1 : 0}, context, classs, text.toString(), numoflessons);
            } else {
                if (intent.getAction() != null && intent.getAction().equals("hu.kfg.standins.CHECK_NOW")) {
                    showSuccessToast.postAtFrontOfQueue(new Runnable() {
                        public void run() {
                            Toast.makeText(context, R.string.no_new_substitution2, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        } else {
            if (text.toString().length() > 5) {
                notifyIfStandinsChanged(new int[]{0, pref.getBoolean("vibrate", false) ? 1 : 0, pref.getBoolean("flash", false) ? 1 : 0}, context, classs, text.toString(), numoflessons);
            } else {
                if (pref.getBoolean("always_notify", false)) {
                    notifyIfStandinsChanged(new int[]{3, pref.getBoolean("vibrate", false) ? 1 : 0, pref.getBoolean("flash", false) ? 1 : 0}, context, classs, text.toString(), numoflessons);
                } else {
                    if (intent.getAction() != null && intent.getAction().equals("hu.kfg.standins.CHECK_NOW")) {
                        showSuccessToast.postAtFrontOfQueue(new Runnable() {
                            public void run() {
                                Toast.makeText(context, R.string.no_new_substitution2, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        }
        pref.edit().putString("last", text.toString() + (new SimpleDateFormat("yyyy/DDD", Locale.ENGLISH).format(new Date()))).apply();

        return -1;
    }

    private static void notifyIfChanged(int[] state, Context context, String url, String subjects) {
        Intent intent = new Intent(context, TableViewActivity.class);
        Intent eintent = new Intent(Intent.ACTION_VIEW);
        eintent.setData(Uri.parse(url));

        final SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(context);
        int time = Integer.valueOf(new SimpleDateFormat("HHmm", Locale.US).format(new Date()));
        boolean nightmode = pref.getBoolean("nightmode", false) && (time > NIGHTMODE_START || time < NIGHTMODE_STOP);
        String oldtext = oldtext = null;
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (state[0] == 0 && notificationManager != null) {
            notificationManager.cancel(1);
            if (isNotificationVisible(context)) {
                oldtext = pref.getString("oldtext", null);
            }
        }
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
        PendingIntent epIntent = PendingIntent.getActivity(context, 0, eintent, 0);
        Notification.Builder n = new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(state[0] == 0 ? context.getString(R.string.new_grade) : context.getString(R.string.gyia_expired_not))
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= 21) {
            n.setSmallIcon(R.drawable.number_blocks);
        } else {
            n.setSmallIcon(R.drawable.number_blocks_mod);
        }
        //.setContentIntent(pIntent)
        if (state[1] == 1 && !nightmode) {
            n.setVibrate(new long[]{0, 60, 100, 70, 100, 60});
        }
        if (state[2] == 1 && !nightmode) {
            n.setLights(0xff00FF88, 350, 3000);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            n.setVisibility(Notification.VISIBILITY_PRIVATE);
        }
        n.addAction(android.R.drawable.ic_menu_view, context.getString(R.string.open), epIntent);
        n.addAction(android.R.drawable.ic_input_get, context.getString(R.string.grade_table), pIntent);
        Notification notification = new Notification.BigTextStyle(n)
                .bigText(((state[0] == 0 ? context.getString(R.string.new_grade) + "\n" : "") + subjects + (oldtext == null ? "" : "\n" + oldtext))).build();
//				notification.number = numberoflessons;
        notificationManager.notify(state[0], notification);
        pref.edit().putString("oldtext", subjects.length() > 100 ? subjects.substring(0, subjects.indexOf(",", 90)) + "…" : subjects).commit();
    }

    private static boolean isNotificationVisible(Context context) {
        if (Build.VERSION.SDK_INT < 23) return false;
        StatusBarNotification[] notifications = ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).getActiveNotifications();
        if (notifications.length == 0) return false;
        for (StatusBarNotification s : notifications) {
            if (s.getId() == 0) return true;
        }
        return false;
    }

    private static String SHA512(byte[] data) throws Exception {
        if (data == null) throw new Exception();
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(data);
        StringBuffer sb = new StringBuffer();
        byte[] byteData = md.digest();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    private static void notifyIfStandinsChanged(int[] state, Context context, String classs, String subjects, int numberoflessons) {
        Intent intent = new Intent(context, MainActivity.class);
        Intent eintent = new Intent(Intent.ACTION_VIEW);
        eintent.setData(Uri.parse("https://apps.karinthy.hu/helyettesites"));
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
        PendingIntent epIntent = PendingIntent.getActivity(context, 0, eintent, 0);
        Notification.Builder n = new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.kfg_standins))
                .setContentText(state[0] == 0 ? context.getString(R.string.new_substitution2) + " (" + classs + ")" + subjects : context.getString(R.string.no_new_substitution2) + " (" + classs + ")")
                .setSmallIcon(R.drawable.ic_standins)
                //.setContentIntent(pIntent)
                .setAutoCancel(true);
        int time = Integer.valueOf(new SimpleDateFormat("HHmm", Locale.US).format(new Date()));
        boolean nightmode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("nightmode", false) && (time > NIGHTMODE_START || time < NIGHTMODE_STOP);
        if (state[1] == 1 && state[0] != 3 && !nightmode) {
            n.setVibrate(new long[]{0, 60, 100, 70, 100, 60});
        }
        if (state[2] == 1 && state[0] != 3 && !nightmode) {
            n.setLights(0xff00FF88, 350, 3000);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            n.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= 16) {
            n.addAction(android.R.drawable.ic_menu_view, context.getString(R.string.open_site), epIntent);
            Notification notification = new Notification.BigTextStyle(n)
                    .bigText((state[0] == 0 ? context.getString(R.string.new_substitution2) + " (" + classs + ")" + subjects : context.getString(R.string.no_new_substitution2) + " (" + classs + ")")).build();
            notification.number = numberoflessons;
            notificationManager.notify(STANDINS_ID, notification);
        } else {
            Notification not = n.getNotification();
            not.number = numberoflessons;
            notificationManager.notify(STANDINS_ID, not);
        }
    }

}
