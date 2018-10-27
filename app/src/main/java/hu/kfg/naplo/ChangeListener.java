package hu.kfg.naplo;

import android.content.*;
import android.preference.*;

import android.net.*;
import android.os.*;
import android.service.notification.StatusBarNotification;
import android.util.*;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.MessageDigest;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjuster;
import java.util.*;
import java.io.*;

import android.widget.*;
import android.app.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.*;

import javax.net.ssl.HttpsURLConnection;

import hu.hexadec.textsecure.Cryptography;

public class ChangeListener {

    private static final int NIGHTMODE_START = 2200;
    private static final int NIGHTMODE_STOP = 600;
    private static final long[] VIBR_PATTERN = new long[]{50, 60, 100, 70, 100, 60};
    private static final int LED_COLOR = 0xff00FF88;
    static final String MODE_TEACHER = "teacher";
    static final String MODE_TRUE = "true";
    static final String MODE_STANDINS = "standins";
    static final String MODE_NAPLO = "naplo";
    static final String MODE_FALSE = "false";

    private static final String TAG = "KFGnaplo-check";
    private static final int STANDINS_ID = 100;

    private static boolean running = false; //From old workaround, probably not necessary?

    static final int DB_EMPTY = 4;
    static final int UPGRADE_DONE = 3;
    static final int UPGRADE_FAILED = 5;
    static final int DONE = 0;
    static final int DONE_NO_CHANGE = 1;

    static final int TOKEN_ERROR = -10;
    static final int UNKNOWN_ERROR = -1, STD_ERROR = -1;
    static final int NETWORK_RELATED_ERROR = -2;
    static final int CREDENTIALS_ERROR = -7;

    static final String CHANNEL_STANDINS = "standins";
    static final String CHANNEL_GRADES = "grades";
    static final String CHANNEL_NIGHT = "night";

    public static final String eURL = "https://klik035252001.e-kreta.hu";
    public static final String eCODE = "klik035252001";

    public static void onRunJob(final Context context, final Intent intent) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        final String mode = pref.getString("notification_mode", MODE_TRUE);
        if (mode.equals(MODE_FALSE)) {
            return;
        }
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            CheckerJob.runJobImmediately();
            return;
        }
        ConnectivityManager cManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cManager == null
                || cManager.getActiveNetworkInfo() == null
                || !cManager.getActiveNetworkInfo().isConnected()) {
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                switch (mode) {
                    case MODE_TRUE:
                        //doCheck(context, intent);
                        try {
                            getEkretaGrades(context, intent);
                        } catch (JSONException je) {
                            je.printStackTrace();
                            Log.e(TAG, "JSON Processing error!");
                        }
                        doStandinsCheck(context, intent);
                        break;
                    case MODE_NAPLO:
                        //doCheck(context, intent);
                        try {
                            getEkretaGrades(context, intent);
                        } catch (JSONException je) {
                            je.printStackTrace();
                            Log.e(TAG, "JSON Processing error!");
                        }
                        break;
                    case MODE_TEACHER:
                    case MODE_STANDINS:
                        doStandinsCheck(context, intent);
                        break;
                    case MODE_FALSE:
                        break;

                }
            }

        }).start();


    }

    private static void doStandinsCheck(final Context context, final Intent intent) {
        final Handler showToast = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
            }
        };
        final String TAG = "KFGstandins-check";
        final SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(context);
        String classs = pref.getString("class", "noclass");
        String mode = pref.getString("notification_mode", MODE_TRUE);
        if (classs.equals("noclass")) {
            return;
        }
        if (classs.length() < 3) {
            showToast.postAtFrontOfQueue(new Runnable() {
                public void run() {
                    Toast.makeText(context, "Írd be az osztályodat!", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        String kfgserver = "https://apps.karinthy.hu/helyettesites/";

        HttpURLConnection urlConnection;
        try {
            URL url = new URL(kfgserver);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(true);
        } catch (IOException e) {
            Log.e(TAG, "Cannot load website!");
            if (intent.hasExtra("error")) {
                showToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
            e.printStackTrace();
            return;
        } catch (Exception e) {
            Log.e(TAG, "Unknown error!");
            if (intent.hasExtra("error")) {
                showToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            e.printStackTrace();
            return;
        }


        List<String> cls = new ArrayList<>();
        if (mode.equals(MODE_TEACHER)) {
            try {
                String name = classs.replace(", ", ",").replace(" ,", ",");
                String[] names = name.split(",");
                cls.addAll(Arrays.asList(names));
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

        String lessonsToIgnore = pref.getString("ignore_lessons", "semmitsemignoral")
                .replace(", ", ",").replace(" ,", "");
        List<String> ilessons = new LinkedList<>();
        try {
            ilessons = Arrays.asList(lessonsToIgnore.split(","));
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, ilessons.size() + " <-- size of ignore");
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
                    sub.setTeacher(line.substring(line.indexOf(">") + 1, line.lastIndexOf("<")));
                }
                if (line.contains("\"subject\"") && counter == 3) {
                    sub.setSubject(line.substring(line.indexOf(">") + 1, line.lastIndexOf("<")));
                }
                if (line.contains("\"comment\"") && counter == 6) {
                    sub.setComment(line.substring(line.indexOf(">") + 1, line.lastIndexOf("<")));
                }
                if (line.contains("\"class\"") && counter == 2) {
                    sub.setGroup(line.substring(line.indexOf(">") + 1, line.lastIndexOf("<")));
                }
                if (line.contains("\"lesson\"") && counter == 1) {
                    int period = -1;
                    try {
                        period = Integer.valueOf(line.substring(line.indexOf(">") + 1, line.lastIndexOf("<")));
                    } catch (Exception e) {
                        Log.d(TAG, "No lesson specified");
                    }
                    sub.setTime(period, day == 1);
                }
                if (line.contains("\"room\"") && counter == 4) {
                    int room = 0;
                    try {
                        room = Integer.valueOf(line.substring(line.indexOf(">") + 1, line.lastIndexOf("<")));
                    } catch (Exception e) {
                        Log.d(TAG, "No room specified!");
                    }
                    sub.setRoom(room);
                }
                if (line.contains("\"missing_teacher\"") && counter == 5) {
                    sub.setMissingTeacher(line.substring(line.indexOf(">") + 1, line.lastIndexOf("<")));
                }
                counter++;

            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        Log.d(TAG, "Subtitutions: " + subs.size());
        pref.edit().putLong("last_check2", System.currentTimeMillis()).commit();
        StringBuilder text = new StringBuilder();
        int numoflessons = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            subs.sort(new Comparator<Substitution>() {
                @Override
                public int compare(Substitution o1, Substitution o2) {
                    return o1.getTimeValue() - o2.getTimeValue();
                }
            });
        }
        if (mode.equals(MODE_TEACHER)) {
            for (Substitution sub : subs) {
                for (String cla : cls) {
                    if (cla.equals(sub.getTeacher()) && !sub.isOver()) {
                        text.append("\n");
                        text.append(sub.toString("PPDD. SS: GG C9 RR MT"));
                        Log.d(TAG, sub.toString("PPDD. SS: GG C9 RR MT"));
                        numoflessons++;
                    }
                }
            }
        } else {
            substitutions:
            for (Substitution sub : subs) {
                for (String cla : cls) {
                    if (cla.equals(sub.getGroup()) && !sub.isOver()) {
                        for (String toIgnore : ilessons)
                            if (toIgnore.equalsIgnoreCase(sub.getSubject()))
                                continue substitutions;
                        text.append("\n");
                        text.append(sub.toString("PPDD. SS: TE C9 RR, GG"));
                        Log.d(TAG, sub.toString("PPDD. SS: TE C9 RR, GG"));
                        numoflessons++;
                    } /*else {
                        Log.d(TAG, sub.toString("---PPDD. S: TE C9 R, G") + "//" + cla + "//" + sub.getGroup() + "//");
                    } */
                }
            }
        }

        if (pref.getBoolean("onlyonce", true) && pref.getString("last", "nuller").equals(text.toString() + (new SimpleDateFormat("yyyy/DDD", Locale.ENGLISH).format(new Date()))) && !intent.hasExtra("show_anyway")) {
            showToast.postAtFrontOfQueue(new Runnable() {
                public void run() {
                    Toast.makeText(context, R.string.no_new_substitution2, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            if (text.toString().length() > 5) {
                notifyIfStandinsChanged(new int[]{0, pref.getBoolean("vibrate", false) ? 1 : 0, pref.getBoolean("flash", false) ? 1 : 0}, context, classs, text.toString(), numoflessons);
            } else {
                showToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, R.string.no_new_substitution2, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
        pref.edit().putString("last", text.toString() + (new SimpleDateFormat("yyyy/DDD", Locale.ENGLISH).format(new Date()))).apply();
    }

    private static void notifyIfChanged(int[] state, Context context, String url, String subjects) {
        try {
            setUpNotificationChannels(context);
        } catch (Exception e) {
            Log.e(TAG, "Error while creating channels!");
            e.printStackTrace();
        }
        Intent intent = new Intent(context, TableViewActivity.class);
        Intent main = new Intent(context, MainActivity.class);
        Intent eintent = new Intent(Intent.ACTION_VIEW);
        eintent.setData(Uri.parse(url));

        final SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(context);
        int time = Integer.valueOf(new SimpleDateFormat("HHmm", Locale.US).format(new Date()));
        boolean nightmode = pref.getBoolean("nightmode", false) && (time > NIGHTMODE_START || time < NIGHTMODE_STOP);
        String oldtext = "";
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager instance is null!");
            return;
        }
        if (state[0] == 0) {
            notificationManager.cancel(1);
            if (isNotificationVisible(context)) {
                oldtext += '\n';
                oldtext += pref.getString("oldtext", "");
            }
        }
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
        PendingIntent epIntent = PendingIntent.getActivity(context, 0, eintent, 0);
        PendingIntent mainIntent = PendingIntent.getActivity(context, 0, main, 0);
        Notification.Builder n;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            n = new Notification.Builder(context, nightmode || state[0] == 1 ? CHANNEL_NIGHT : CHANNEL_GRADES);
        } else {
            n = new Notification.Builder(context);
        }
        n.setContentTitle(context.getString(R.string.app_name))
                .setContentText(state[0] == 0 ? context.getString(R.string.new_grade) : state[0] == 1 ? context.getString(R.string.wrong_username_not) : context.getString(R.string.ekreta_new))
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= 21) {
            n.setSmallIcon(R.drawable.number_blocks);
        } else {
            n.setSmallIcon(R.drawable.number_blocks_mod);
        }
        //.setContentIntent(pIntent)
        if (state[1] == 1 && !nightmode) {
            n.setVibrate(VIBR_PATTERN);
        }
        if (state[2] == 1 && !nightmode) {
            n.setLights(LED_COLOR, 350, 3000);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            n.setVisibility(Notification.VISIBILITY_PRIVATE);
        }
        if (state[0] == 0) {
            n.addAction(android.R.drawable.ic_menu_view, context.getString(R.string.open), epIntent);
            n.addAction(android.R.drawable.ic_input_get, context.getString(R.string.grade_table), pIntent);
        } else {
            n.addAction(android.R.drawable.ic_menu_edit, context.getString(R.string.open_app), mainIntent);
        }
        Notification notification = new Notification.BigTextStyle(n)
                .bigText(state[0] == 0 ? (context.getString(R.string.new_grade) + "\n" + subjects + oldtext) : state[0] == 1 ? context.getString(R.string.wrong_username_not) : context.getString(R.string.ekreta_new)).build();
        notificationManager.notify(state[0], notification);
        pref.edit().putString("oldtext", subjects.length() > 100 ? subjects.substring(0, subjects.indexOf(",", 90)) + "…" : subjects).commit();
    }

    private static boolean isNotificationVisible(Context context) {
        NotificationManager nManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT < 23 || nManager == null) return false;
        StatusBarNotification[] notifications = nManager.getActiveNotifications();
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
        StringBuilder sb = new StringBuilder();
        byte[] byteData = md.digest();
        for (byte byted : byteData) {
            sb.append(Integer.toString((byted & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    static void setUpNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            //Setup standins channel
            NotificationChannel notificationChannel =
                    new NotificationChannel(CHANNEL_STANDINS, context.getString(R.string.standins), NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(LED_COLOR);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(VIBR_PATTERN);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            notificationManager.createNotificationChannel(notificationChannel);

            //Setup grades channel
            notificationChannel =
                    new NotificationChannel(CHANNEL_GRADES, context.getString(R.string.grades), NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(LED_COLOR);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(VIBR_PATTERN);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(notificationChannel);

            //Setup nightmode channel
            notificationChannel =
                    new NotificationChannel(CHANNEL_NIGHT, context.getString(R.string.night_notifications), NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setLightColor(LED_COLOR);
            notificationChannel.enableLights(false);
            notificationChannel.enableVibration(false);
            notificationChannel.setVibrationPattern(new long[]{0, 0});
            notificationChannel.setSound(null, null);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private static void notifyIfStandinsChanged(int[] state, Context context, String classs, String subjects, int numberoflessons) {
        try {
            setUpNotificationChannels(context);
        } catch (Exception e) {
            Log.e(TAG, "Error while creating channels!");
            e.printStackTrace();
        }
        int time = Integer.valueOf(new SimpleDateFormat("HHmm", Locale.US).format(new Date()));
        boolean nightmode = PreferenceManager.getDefaultSharedPreferences(context).getBoolean("nightmode", false) && (time > NIGHTMODE_START || time < NIGHTMODE_STOP);
        Intent eintent = new Intent(Intent.ACTION_VIEW);
        eintent.setData(Uri.parse("https://apps.karinthy.hu/helyettesites"));
        PendingIntent epIntent = PendingIntent.getActivity(context, 0, eintent, 0);
        Notification.Builder n;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            n = new Notification.Builder(context, state[0] != 3 && !nightmode ? CHANNEL_STANDINS : CHANNEL_NIGHT);
        } else {
            n = new Notification.Builder(context);
        }
        n.setContentTitle(context.getString(R.string.kfg_standins));
        n.setContentText(state[0] == 0 ? context.getString(R.string.new_substitution2) + " (" + classs + ")" + subjects : context.getString(R.string.no_new_substitution2) + " (" + classs + ")");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            n.setSmallIcon(R.drawable.ic_standins);
        else
            n.setSmallIcon(R.drawable.ic_standins3);
        n.setAutoCancel(true);
        if (state[1] == 1 && state[0] != 3 && !nightmode) {
            n.setVibrate(VIBR_PATTERN);
        }
        if (state[2] == 1 && state[0] != 3 && !nightmode) {
            n.setLights(LED_COLOR, 350, 3000);
        }
        if (Build.VERSION.SDK_INT >= 21) {
            n.setVisibility(Notification.VISIBILITY_PUBLIC);
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            Log.e(TAG, "NotificationManager instance is null!");
            return;
        }
        n.addAction(android.R.drawable.ic_menu_view, context.getString(R.string.open_site), epIntent);
        Notification notification = new Notification.BigTextStyle(n)
                .bigText((state[0] == 0 ? context.getString(R.string.new_substitution2) + " (" + classs + ")" + subjects : context.getString(R.string.no_new_substitution2) + " (" + classs + ")")).build();
        notification.number = numberoflessons;
        notificationManager.notify(STANDINS_ID, notification);
    }

    static String getToken(final Context context, String username, String password, boolean forceCreate) throws Exception {
        final Handler showToast = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
            }
        };

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if (System.currentTimeMillis() - prefs.getLong("token_created", 0) < 10 * 60 * 1000 && !forceCreate) {
            if (prefs.getString("access_token", null) != null) {
                Log.d(TAG, "Using previously generated token...");
                return prefs.getString("access_token", "");
            }
        }
        URL url = new URL(ChangeListener.eURL + "/idp/api/v1/Token");

        HttpsURLConnection request = (HttpsURLConnection) (url.openConnection());
        String post = "institute_code=" + ChangeListener.eCODE + "&userName=" + username + "&password=" + password + "&grant_type=password&client_id=919e0c1c-76a2-4646-a2fb-7085bbbf3c56";

        if (username == null || username.length() < 2 || password == null || password.length() < 2) {
            showToast.postAtFrontOfQueue(new Runnable() {
                public void run() {
                    Toast.makeText(context, R.string.incorrect_credentials, Toast.LENGTH_SHORT).show();
                }
            });
            Log.e(TAG, "No credentials");
            notifyIfChanged(new int[]{1, 1, 1}, context, eURL, "");
            throw new IllegalAccessException("No credentials");
        }

        request.setDoOutput(true);
        request.addRequestProperty("Accept", "application/json");
        request.addRequestProperty("HOST", ChangeListener.eURL.replace("https://", ""));
        request.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        request.setRequestMethod("POST");
        request.connect();
        OutputStreamWriter writer = new OutputStreamWriter(request.getOutputStream());
        writer.write(post);
        writer.flush();
        Log.i(TAG, "Response code: " + request.getResponseCode() + "/" + request.getResponseMessage());

        if (request.getResponseCode() == 401) {
            showToast.postAtFrontOfQueue(new Runnable() {
                public void run() {
                    Toast.makeText(context, R.string.incorrect_credentials, Toast.LENGTH_SHORT).show();
                }
            });
            Log.e(TAG, "Invalid credentials");
            notifyIfChanged(new int[]{1, 1, 1}, context, eURL, "");
            throw new IllegalAccessException("Invalid credentials");
        }

        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            JSONObject jObject = new JSONObject(sb.toString());
            request.disconnect();
            prefs.edit().putString("access_token", jObject.getString("access_token"))
                    .putLong("token_created", System.currentTimeMillis()).commit();
            return jObject.getString("access_token");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        request.disconnect();

        throw new IllegalAccessException("No token returned");
    }

    static int getEkretaGrades(final Context context, Intent intent) throws JSONException {
        final Handler showToast = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
            }
        };
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        if (pref.getString("url", null) != null) {
            notifyIfChanged(new int[]{2, 1, 1}, context, eURL, "");
            return CREDENTIALS_ERROR;
        }
        JSONObject resultStuff;
        String password = "null";
        String password_crypt = pref.getString("password2", null);
        if (password_crypt != null && password_crypt.length() >= 4) {
            Cryptography cr = new Cryptography();
            password = cr.cryptThreedog(password_crypt, true, pref.getString("username", "null"));
        }
        try {
            URL url = new URL(eURL + "/mapi/api/v1/Student");
            HttpsURLConnection request = (HttpsURLConnection) (url.openConnection());
            request.addRequestProperty("Accept", "application/json");
            request.addRequestProperty("Authorization", "Bearer " + getToken(context, pref.getString("username", "null"), password != null ? password : "null", intent.hasExtra("create_new_token")));
            request.addRequestProperty("HOST", eURL.replace("https://", ""));
            request.addRequestProperty("Connection", "keep-alive");
            request.setRequestMethod("GET");
            request.connect();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            resultStuff = new JSONObject(sb.toString());
            request.disconnect();
        } catch (IllegalAccessException | FileNotFoundException e) {
            e.printStackTrace();
            return TOKEN_ERROR;
        } catch (IOException e) {
            e.printStackTrace();
            return NETWORK_RELATED_ERROR;
        } catch (Exception e) {
            e.printStackTrace();
            return UNKNOWN_ERROR;
        }

        final List<Grade> mygrades = new ArrayList<>();
        JSONArray rawGrades = resultStuff.getJSONArray("Evaluations");
        Log.d(TAG, rawGrades.getJSONObject(0).toString());
        for (int i = 0; i < rawGrades.length(); i++) {
            JSONObject currentItem = rawGrades.getJSONObject(i);
            SimpleDateFormat importedFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Grade grade = new Grade((byte) currentItem.getInt("NumberValue"));
            grade.addSubject(currentItem.getString("Subject"));
            grade.addTeacher(currentItem.getString("Teacher"));
            String date = currentItem.getString("Date");
            String createTime = currentItem.getString("CreatingTime");
            try {
                date = dateFormat.format(importedFormat.parse(date));
                createTime = createTime.replace("T", " ").substring(0, 19);
            } catch (Exception e) {
                e.printStackTrace();
            }
            grade.addDate(date);
            grade.addSaveDate(createTime);
            grade.addDescription(currentItem.getString("Theme") + " - " + currentItem.getString("Mode"));
            Log.e(TAG, grade.save_date + "--" + createTime.length());
            mygrades.add(grade);
        }
        if (intent.hasExtra("dbupgrade")) {
            Log.i("Grades", "Size: " + mygrades.size());
            pref.edit().putInt("numberofnotes", rawGrades.length())
                    .putLong("last_check", System.currentTimeMillis()).commit();
            if (mygrades.size() < 1) {
                new DBHelper(context).cleanDatabase();
                return DB_EMPTY;
            }
            if (new DBHelper(context).upgradeDatabase(mygrades)) return UPGRADE_DONE;
            else return UPGRADE_FAILED;
        }
        try {
            if (resultStuff.toString().length() < 300) {
                Log.w(TAG, rawGrades.toString());
                throw new Exception("Content too small \nLength: " + resultStuff.toString().length());
            }
            if (mygrades.size() == 0) throw new IndexOutOfBoundsException("No grades!");
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, e.getMessage());
            Log.w(TAG, "Grades found: " + mygrades.size());
            if (intent.hasExtra("error")) {
                showToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.error_no_grades), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            new DBHelper(context).cleanDatabase();
            return DB_EMPTY;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            if (intent.hasExtra("error")) {
                showToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return STD_ERROR;
        }

        if (running) {
            Log.w(TAG, "A process is already running");
            Log.w(TAG, "Action:\t" + intent.getAction());
            return STD_ERROR;
        }
        running = true;
        byte[] notes = new byte[1];
        try {
            if (mygrades.size() == 0) throw new Exception("No grades were found!");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(mygrades);
            notes = baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Cannot convert grades to byte array!");
        }
        final int numofnotes = pref.getInt("numberofnotes", 0);
        try {

            pref.edit().putInt("numberofnotes", rawGrades.length()).putLong("last_check", System.currentTimeMillis()).commit();
            if (rawGrades.length() - numofnotes > 0) {
                int i = rawGrades.length() - numofnotes;
                StringBuilder text = new StringBuilder();
                DBHelper db1 = new DBHelper(context);
                for (int i2 = 0; i2 < i; i2++) {
                    text.append(mygrades.get(i2).getNotificationFormat());
                    text.append(", \n");
                    if (!intent.hasExtra("dbupgrade")) db1.insertGrade(mygrades.get(i2));
                }
                text.deleteCharAt(text.length() - 2);
                notifyIfChanged(new int[]{0, pref.getBoolean("vibrate", false) ? 1 : 0, pref.getBoolean("flash", false) ? 1 : 0}, context, eURL, text.toString());
                pref.edit().putString("lastSHA", SHA512(notes)).commit();
                running = false;
                return DONE;
            } else {
                if (!SHA512(notes).equals(pref.getString("lastSHA", "ABCD"))) {
                    pref.edit().putString("lastSHA", SHA512(notes)).commit();
                    notifyIfChanged(new int[]{0, pref.getBoolean("vibrate", false) ? 1 : 0, pref.getBoolean("flash", false) ? 1 : 0}, context, eURL, context.getString(R.string.unknown_change));
                    //If a grade was modified, it's easier to update the whole DB
                    Intent intent2 = new Intent(context, ChangeListener.class);
                    intent2.putExtra("dbupgrade", true);
                    getEkretaGrades(context, intent);
                    running = false;
                    return DONE;
                } else if (intent.hasExtra("error")) {
                    showToast.postAtFrontOfQueue(new Runnable() {
                        public void run() {
                            Toast.makeText(context, context.getString(R.string.no_new_grade) + " " + mygrades.size() + "/" + numofnotes, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                running = false;
                return DONE_NO_CHANGE;
            }
        } catch (Exception e) {
            Log.e(TAG, "Unknown error!");
            if (intent.hasExtra("error")) {
                showToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            e.printStackTrace();
            running = false;
            return STD_ERROR;
        }
    }

    static int getTimetable(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String password = "null";
        String password_crypt = pref.getString("password2", null);
        if (password_crypt != null && password_crypt.length() >= 4) {
            Cryptography cr = new Cryptography();
            password = cr.cryptThreedog(password_crypt, true, pref.getString("username", "null"));
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat importedFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_WEEK, 6);
        JSONArray resultStuff;
        try {
            URL server = new URL(eURL + "/mapi/api/v1/Lesson?fromDate=" + format.format(new Date()) + "&toDate=" + format.format(cal.getTime()));
            HttpsURLConnection request = (HttpsURLConnection) (server.openConnection());
            request.addRequestProperty("Accept", "application/json");
            request.addRequestProperty("Authorization", "Bearer " + getToken(context, pref.getString("username", "null"), password != null ? password : "null", false));
            request.addRequestProperty("HOST", eURL.replace("https://", ""));
            request.addRequestProperty("Connection", "keep-alive");
            request.setRequestMethod("GET");
            request.connect();

            final BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            resultStuff = new JSONArray(sb.toString());
            request.disconnect();
        } catch (IOException e) {
            Log.d(TAG, "Timetable network error");
            e.printStackTrace();
            return NETWORK_RELATED_ERROR;
        } catch (IllegalAccessException e) {
            Log.d(TAG, "Timetable token error");
            e.printStackTrace();
            return TOKEN_ERROR;
        } catch (Exception e) {
            e.printStackTrace();
            return UNKNOWN_ERROR;
        }

        List<Lesson> mylessons = new ArrayList<>();
        Log.d(TAG, resultStuff.toString());
        try {
            for (int i = 0; i < resultStuff.length(); i++) {
                JSONObject item = resultStuff.getJSONObject(i);
                Date when = importedFormat.parse(item.getString("Date"));
                Date from = importedFormat.parse(item.getString("StartTime"));
                Date to = importedFormat.parse(item.getString("EndTime"));
                String subject = item.getString("Subject");
                String group = item.getString("ClassGroup");
                int room = item.getInt("ClassRoom");
                String teacher = item.getString("Teacher");
                String topic = item.getString("Theme");
                Lesson currentLesson = new Lesson(subject, teacher, room, from, to, when, group);
                currentLesson.setTopic(topic != null || topic.length() > 1 ? topic : "");
                mylessons.add(currentLesson);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.d(TAG, "JSON Exception while evaluating lessons");
            return UNKNOWN_ERROR;
        } catch (ParseException e) {
            e.printStackTrace();
            Log.d(TAG, "Date parse error");
            return UNKNOWN_ERROR;
        }
        return DONE;
    }

}
