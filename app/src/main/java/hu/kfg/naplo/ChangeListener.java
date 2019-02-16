package hu.kfg.naplo;

import android.content.*;
import android.preference.*;

import android.net.*;
import android.os.*;
import android.text.Html;
import android.util.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.text.ParseException;
import java.util.*;
import java.io.*;

import android.widget.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.*;

import javax.net.ssl.HttpsURLConnection;

import hu.hexadec.textsecure.Cryptography;

public class ChangeListener {


    static final String MODE_TEACHER = "teacher";
    static final String MODE_TRUE = "true";
    static final String MODE_STANDINS = "standins";
    static final String MODE_NAPLO = "naplo";
    static final String MODE_FALSE = "false";

    private static final String TAG = "KFGnaplo-check";

    static final int DB_EMPTY = 4;
    static final int UPGRADE_DONE = 3;
    static final int UPGRADE_FAILED = 5;
    static final int DONE = 0;
    static final int DONE_NO_CHANGE = 1;

    static final int TOKEN_ERROR = -10;
    static final int UNKNOWN_ERROR = -1, STD_ERROR = -1;
    static final int NETWORK_RELATED_ERROR = -2;
    static final int CREDENTIALS_ERROR = -7;

    public static final String eURL = "https://klik035252001.e-kreta.hu";
    public static final String eCODE = "klik035252001";

    public static void onRunJob(final Context context, final Intent intent) {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        final String mode = pref.getString("notification_mode", MODE_TRUE);
        if (mode.equals(MODE_FALSE) && !intent.hasExtra("forced")) {
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
                        try {
                            getEkretaGrades(context, intent);
                        } catch (JSONException je) {
                            je.printStackTrace();
                            Log.e(TAG, "JSON Processing error!");
                        }
                        doStandinsCheck(context, intent);
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
        if (classs.length() < MainActivity.CLASS_MIN_LENGTH) {
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
        }

        int day = 0;
        List<Substitution> subs = new ArrayList<>();
        String tomorrowFormat = "";
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
                if (day == 2 && line.contains("<caption>")) {
                    tomorrowFormat = line.substring(line.indexOf(">") + 1, line.lastIndexOf("<"));
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
                        String line2 = line.replaceAll("\\D+", "");
                        period = Integer.valueOf(line2);
                    } catch (Exception e) {
                    }
                    sub.setTime(period, day == 1);
                }
                if (line.contains("\"room\"") && counter == 4) {
                    int room = 0;
                    try {
                        String line2 = line.replaceAll("\\D+", "");
                        room = Integer.valueOf(line2);
                    } catch (Exception e) {
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

        boolean autoignore = pref.getBoolean("timetable_autoignore", false);
        TimetableDB db = new TimetableDB(context);
        Date tomorrow;
        try {
            tomorrow = new SimpleDateFormat("yyyy. MMMM dd.", new Locale("hu")).parse(tomorrowFormat.split(",")[0]);
        } catch (Exception e) {
            e.printStackTrace();
            tomorrow = new Date();
        }
        List<Lesson> lessonsToday = db.getLessonsOnDay(new Date());
        List<Lesson> lessonsTomorrow = db.getLessonsOnDay(tomorrow);

        if (mode.equals(MODE_TEACHER)) {
            for (Substitution sub : subs) {
                for (String cla : cls) {
                    if (cla.equals(sub.getTeacher()) && !sub.isOver()) {
                        text.append("\n");
                        text.append(sub.toString("PPDD. SS: GG C9 RR MT"));
                        //Log.d(TAG, sub.toString("PPDD. SS: GG C9 RR MT"));
                        numoflessons++;
                    }
                }
            }
        } else {
            for (Substitution sub : subs) {
                if (sub.isMemberOfClasses(classs) && !sub.isOver()) {
                    if (!autoignore || isRelevant(sub, sub.isToday() ? lessonsToday : lessonsTomorrow)) {
                        text.append("\n");
                        text.append(sub.toString("PPDD. SS: TE C9 RR, GG"));
                    }
                    //Log.d(TAG, sub.toString("PPDD. SS: TE C9 RR, GG"));
                    numoflessons++;
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
                AppNotificationManager.notifyIfStandinsChanged(new int[]{0, pref.getBoolean("vibrate", false) ? 1 : 0, pref.getBoolean("flash", false) ? 1 : 0}, context, classs, text.toString(), numoflessons);
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

    private static boolean isRelevant(Substitution substitution, List<Lesson> lessons) {
        try {
            //Log.i(TAG, substitution.toString("DDPP-SS:MT->TE C9"));
            if (lessons == null || lessons.size() == 0) {
                Log.i(TAG, "No lessons on day: ?");
                return true; //Since we don't know if an error occurred, or no lessons
            }
            for (Lesson l : lessons) {
                if (l.period == substitution.getPeriod()) {
                    //Subject check
                    if (substitution.getSubject().length() < 2) {
                        return true;
                    }
                    if (l.subjectCat.equalsIgnoreCase(substitution.getSubject()) ||
                            l.subject.equalsIgnoreCase(substitution.getSubject())) {
                        return true;
                    } //These two checks ensure that no relevant lessons will be filtered
                    else if (l.subject.substring(0, l.subject.length() >= 3 ? 3 : l.subject.length())
                            .equalsIgnoreCase(substitution.getSubject()
                                    .substring(0, l.subject.length() >= 3 ? 3 : l.subject.length()))) {
                        return true;
                    } else if (l.subjectCat.substring(0, l.subjectCat.length() >= 3 ? 3 : l.subjectCat.length())
                            .equalsIgnoreCase(substitution.getSubject()
                                    .substring(0, l.subjectCat.length() >= 3 ? 3 : l.subjectCat.length()))) {
                        return true;
                    } else {
                        return false;
                    }
                    //TODO Teacher check
                    //TODO Substituting teacher check
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Unknown error when checking relevance!");
            e.printStackTrace();
            return true;
        }
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

    static String getToken(final Context context, String username, String password, boolean forceCreate) throws Exception {
        final Handler showToast = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message message) {
            }
        };

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        //If the access token is younger than 10 minutes, use that one
        if (System.currentTimeMillis() - prefs.getLong("token_created", 0) < prefs.getInt("expires_in", 600) * 1000 && !forceCreate) {
            if (prefs.getString("access_token", null) != null) {
                Log.d(TAG, "Using previously generated token...");
                return prefs.getString("access_token", "");
            }
        }
        URL url = new URL(ChangeListener.eURL + "/idp/api/v1/Token");

        String refToken = prefs.getString("refresh_token", "");
        HttpsURLConnection request = (HttpsURLConnection) (url.openConnection());
        String post = "institute_code=" + ChangeListener.eCODE + "&userName=" + username + "&password=" + password + "&grant_type=password&client_id=919e0c1c-76a2-4646-a2fb-7085bbbf3c56";
        String postRefresh = "institute_code=" + ChangeListener.eCODE + "&refresh_token=" + refToken + "&grant_type=refresh_token&client_id=919e0c1c-76a2-4646-a2fb-7085bbbf3c56";

        if (refToken == null || refToken.length() < MainActivity.TOKENS_MIN_LENGTH) {
            if (username == null || username.length() < MainActivity.CREDS_MIN_LENGTH
                    || password == null || password.length() < MainActivity.CREDS_MIN_LENGTH) {
                showToast.postAtFrontOfQueue(new Runnable() {
                    public void run() {
                        Toast.makeText(context, R.string.incorrect_credentials, Toast.LENGTH_SHORT).show();
                    }
                });
                Log.e(TAG, "No credentials");
                AppNotificationManager.notifyIfChanged(new int[]{1, 1, 1, 0}, context, eURL, "");
                throw new IllegalAccessException("No credentials");
            }
        } else if (!forceCreate) {
            post = postRefresh;
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

        if (request.getResponseCode() >= 400 && request.getResponseCode() < 500) {
            showToast.postAtFrontOfQueue(new Runnable() {
                public void run() {
                    Toast.makeText(context, R.string.incorrect_credentials, Toast.LENGTH_SHORT).show();
                }
            });
            Log.e(TAG, "Invalid credentials");
            AppNotificationManager.notifyIfChanged(new int[]{1, 1, 1, request.getResponseCode()}, context, eURL, "");
            if (post.equals(postRefresh)) {
                prefs.edit().remove("refresh_token").remove("access_token")
                        .remove("password2").remove("username").apply();
            }
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
                    .putInt("expires_in", jObject.getInt("expires_in") - 100) //Subtract 1 minute to avoid using expired token accidentally
                    .putLong("token_created", System.currentTimeMillis())
                    .putString("refresh_token", jObject.getString("refresh_token"))
                    .putString("password2", "null").putString("username", "null").commit();
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
            AppNotificationManager.notifyIfChanged(new int[]{2, 1, 1}, context, eURL, "");
            return CREDENTIALS_ERROR;
        }
        JSONObject resultStuff;
        String password = "null";
        String password_crypt = pref.getString("password2", null);
        if (password_crypt != null && password_crypt.length() >= MainActivity.CREDS_MIN_LENGTH) {
            Cryptography cr = new Cryptography();
            password = cr.cryptThreedog(password_crypt, true, pref.getString("username", "null"));
        }
        try {
            //Convert grading system URL from String to URL
            URL url = new URL(eURL + "/mapi/api/v1/Student");
            //Create connection
            HttpsURLConnection request = (HttpsURLConnection) (url.openConnection());
            //Provide credentials and connection properties
            request.addRequestProperty("Accept", "application/json");
            request.addRequestProperty("Authorization", "Bearer " +
                    getToken(context, pref.getString("username", "null"),
                            password != null ? password : "null", false));
            request.addRequestProperty("HOST", eURL.replace("https://", ""));
            request.addRequestProperty("Connection", "keep-alive");
            request.setRequestMethod("GET");
            request.connect();
            //Read server response
            final BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream(), "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            //Create a JSON object from the result
            resultStuff = new JSONObject(sb.toString());
            request.disconnect();
            //If there is an exception, return the corresponding error message
        } catch (IllegalAccessException | FileNotFoundException e) {
            //getToken() throws this if the token cannot be obtained
            e.printStackTrace();
            return TOKEN_ERROR;
        } catch (IOException e) {
            //Server is down, or other problems
            e.printStackTrace();
            return NETWORK_RELATED_ERROR;
        } catch (Exception e) {
            //In case any different error occurs, catch them
            e.printStackTrace();
            return UNKNOWN_ERROR;
        }

        final List<Grade> mygrades = new ArrayList<>();
        JSONArray rawGrades = resultStuff.getJSONArray("Evaluations");
        Log.d(TAG, "Grades: " + rawGrades.length());
        int effectiveGradesLength = rawGrades.length();
        for (int i = 0; i < rawGrades.length(); i++) {
            JSONObject currentItem = rawGrades.getJSONObject(i);
            //Log.e(TAG, currentItem.toString());
            SimpleDateFormat importedFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            int value = currentItem.getInt("NumberValue");
            if (value == 0) {
                effectiveGradesLength--;
                continue;
            }
            Grade grade = new Grade((byte) value);
            grade.addSubject(currentItem.getString("Subject"));
            grade.addTeacher(currentItem.getString("Teacher"));
            String date = currentItem.getString("Date");
            String createTime = currentItem.getString("CreatingTime");
            grade.addMode(currentItem.getString("Type"));
            try {
                date = dateFormat.format(importedFormat.parse(date));
                createTime = createTime.replace("T", " ").substring(0, 19);
            } catch (Exception e) {
                e.printStackTrace();
                date = dateFormat.format(new Date(0));
                createTime = dateFormat.format(new Date(0));
            }
            grade.addDate(date);
            grade.addSaveDate(createTime);
            grade.addDescription(currentItem.getString("Theme") + " - " + currentItem.getString("Mode"));
            grade.setRegular(currentItem.getString("Form").equalsIgnoreCase("Mark"));
            //Log.e(TAG, grade.save_date + "--" + createTime.length());
            mygrades.add(grade);
        }
        //Use a separate try-block for the Absences not to affect the grade retrieval
        final List<Absence> myabsences = new ArrayList<>();
        try {
            JSONArray rawAbsences = resultStuff.getJSONArray("Absences");
            Log.d(TAG, "Absences: " + rawAbsences.length());
            for (int i = 0; i < rawAbsences.length(); i++) {
                JSONObject currentItem = rawAbsences.getJSONObject(i);
                String mode = currentItem.getString("ModeName");
                String subject = currentItem.getString("Subject");
                String teacher = currentItem.getString("Teacher");
                String justState = currentItem.getString("JustificationStateName");
                String dateOfAbsence = currentItem.getString("LessonStartTime");
                String dateOfRegister = currentItem.getString("CreatingTime");
                short late_minutes = (short) currentItem.getInt("DelayTimeMinutes");
                byte period = (byte) currentItem.getInt("NumberOfLessons");
                myabsences.add(new Absence(mode, subject, teacher, justState, dateOfAbsence, dateOfRegister, period).addLateMinutes(late_minutes));
            }
            AbsencesDB adb = new AbsencesDB(context);
            adb.upgradeDatabase(myabsences);


        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error while retrieving absences...");
        }
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
        if (intent.hasExtra("dbupgrade")) {
            Log.i("Grades", "Size: " + mygrades.size());
            pref.edit().putInt("numberofnotes", effectiveGradesLength)
                    .putLong("last_check", System.currentTimeMillis()).commit();
            try {
                pref.edit().putString("lastSHA", SHA512(notes)).commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mygrades.size() < 1) {
                new GradesDB(context).cleanDatabase();
                return DB_EMPTY;
            }
            if (new GradesDB(context).upgradeDatabase(mygrades)) return UPGRADE_DONE;
            else return UPGRADE_FAILED;
        }
        try {
            if (resultStuff.toString().length() < 300) {
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
            new GradesDB(context).cleanDatabase();
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
        final int numofnotes = pref.getInt("numberofnotes", 0);
        try {

            pref.edit().putInt("numberofnotes", effectiveGradesLength).putLong("last_check", System.currentTimeMillis()).commit();
            if (effectiveGradesLength - numofnotes > 0 && !SHA512(notes).equals(pref.getString("lastSHA", "ABCD"))) {
                int i = effectiveGradesLength - numofnotes;
                StringBuilder text = new StringBuilder();
                GradesDB db1 = new GradesDB(context);
                for (int i2 = 0; i2 < i; i2++) {
                    text.append(mygrades.get(i2).getNotificationFormat());
                    text.append(", \n");
                    if (!intent.hasExtra("dbupgrade")) db1.insertGrade(mygrades.get(i2));
                }
                text.replace(text.length() - 3, text.length(), "");
                AppNotificationManager.notifyIfChanged(new int[]{0, pref.getBoolean("vibrate", false) ? 1 : 0, pref.getBoolean("flash", false) ? 1 : 0}, context, eURL, text.toString());
                pref.edit().putString("lastSHA", SHA512(notes)).commit();
                return DONE;
            } else {
                if (!SHA512(notes).equals(pref.getString("lastSHA", "ABCD"))) {
                    pref.edit().putString("lastSHA", SHA512(notes)).commit();
                    AppNotificationManager.notifyIfChanged(new int[]{0, pref.getBoolean("vibrate", false) ? 1 : 0, pref.getBoolean("flash", false) ? 1 : 0}, context, eURL, context.getString(R.string.unknown_change));
                    //If a grade was modified, it's easier to update the whole DB
                    try {
                        if (new GradesDB(context).upgradeDatabase(mygrades)) return UPGRADE_DONE;
                    } catch (Exception e) {
                        Log.e(TAG, "Automatically initiated update failed");
                        e.printStackTrace();
                    }
                    return DONE;
                } else if (intent.hasExtra("error")) {
                    showToast.postAtFrontOfQueue(new Runnable() {
                        public void run() {
                            Toast.makeText(context, context.getString(R.string.no_new_grade) + " " + mygrades.size() + "/" + numofnotes, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
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
            return STD_ERROR;
        }
    }

    static int getTimetable(Context context, Date from, Date to, boolean removeExisting) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String password = "null";
        String password_crypt = pref.getString("password2", null);
        if (password_crypt != null && password_crypt.length() >= MainActivity.CREDS_MIN_LENGTH) {
            Cryptography cr = new Cryptography();
            password = cr.cryptThreedog(password_crypt, true, pref.getString("username", "null"));
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat importedFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        JSONArray resultStuff;
        try {
            URL server = new URL(eURL + "/mapi/api/v1/Lesson?fromDate=" + format.format(from) + "&toDate=" + format.format(to));
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

        TimetableDB db = new TimetableDB(context);
        if (removeExisting) db.cleanDatabase();
        try {
            for (int i = 0; i < resultStuff.length(); i++) {
                JSONObject item = resultStuff.getJSONObject(i);
                Date lFrom = importedFormat.parse(item.getString("StartTime"));
                Date lTo = importedFormat.parse(item.getString("EndTime"));
                String subject = item.getString("Subject");
                String subjectCat = item.getString("SubjectCategoryName");
                String group = item.getString("ClassGroup");
                int room = item.getInt("ClassRoom");
                String teacher = item.getString("Teacher");
                String topic = item.getString("Theme");
                byte period = (byte) item.getInt("Count");
                Lesson currentLesson = new Lesson(subject, teacher, room, lFrom, lTo, group, period);
                currentLesson.setTopic(topic != null && topic.length() > 1 ? topic : "");
                currentLesson.setSubjectCat(subjectCat, false);
                //mylessons.add(currentLesson);
                db.insertLesson(currentLesson);
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

    static List<Event> doEventsCheck(final Context context, Date date) {
        final String TAG = "KFGevents-check";
        Log.i(TAG, "Started");
        final SharedPreferences pref = PreferenceManager
                .getDefaultSharedPreferences(context);
        final String year = new SimpleDateFormat("yyyy", Locale.getDefault()).format(date);

        HttpURLConnection urlConnection;
        try {
            URL url = new URL(String.format(TimetableActivity.EVENTS_URL, year));
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(true);
        } catch (IOException e) {
            Log.e(TAG, "Cannot load website!");
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unknown error!");
            e.printStackTrace();
            return null;
        }
        List<Event> events = new ArrayList<>();
        String line = "";
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader
                    (new InputStreamReader(urlConnection.getInputStream(), "iso-8859-2"));
            //NOTE the special space (nbsp??) between month & day!!!
            SimpleDateFormat monthDay = new SimpleDateFormat("yyyy-MMMM dd", new Locale("hu"));
            SimpleDateFormat time = new SimpleDateFormat("HH:mm", Locale.getDefault());
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                if (!line.contains("Date")) {
                    continue;
                }
                String tempDate = Html.fromHtml(line.substring(line.indexOf("Date\">") + "Date\">".length(), line.indexOf(".</TD>"))).toString();
                String tempTime = line.substring(line.indexOf("Time\">") + "Time\">".length(), line.indexOf("</TD>", line.indexOf(".</TD>") + 3)).replace(".", "");
                String tempInfo = Html.fromHtml(line.substring(line.indexOf("Info\">") + "Info\">".length(), line.lastIndexOf("</TD>"))).toString();
                //Log.i(TAG, tempDate + "-" + tempTime + "-" + tempInfo);
                if (tempTime.length() > 0) {
                    tempInfo = '(' + tempTime + ") " + tempInfo;
                }
                Date startDate = monthDay.parse(year + "-" + tempDate);
                Date endDate;
                if (tempTime.length() < 2 || tempTime.contains("szombat")) {
                    endDate = startDate;
                } else if (!tempTime.contains(":") && tempTime.contains("-")) {
                    String endDay = tempTime.split("-")[1];
                    endDate = monthDay.parse(year + "-" + tempDate.split(" ")[0] + " " + endDay);
                } else {
                    endDate = startDate;
                }
                events.add(new Event(tempInfo, startDate, endDate));
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, line);
            return null;
        }
        Log.i(TAG, "Events: " + events.size());
        return events;
    }
}
