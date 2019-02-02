package hu.kfg.naplo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class AppNotificationManager {

    private static final String TAG = "KFG Notifications";
    private static final int NIGHTMODE_START = 2200;
    private static final int NIGHTMODE_STOP = 600;
    private static final long[] VIBR_PATTERN = new long[]{50, 60, 100, 70, 100, 60};
    private static final int LED_COLOR = 0xff00FF88;
    private static final int STANDINS_ID = 100;

    static final String CHANNEL_STANDINS = "standins";
    static final String CHANNEL_GRADES = "grades";
    static final String CHANNEL_ABSENCES = "absences";
    static final String CHANNEL_NIGHT = "night";

    static void notifyIfChanged(int[] state, Context context, String url, String subjects) {
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
                .setContentText(state[0] == 0 ? context.getString(R.string.new_grade)
                        : state[0] == 1 ? context.getString(R.string.wrong_username_not) + (state.length == 4 ? " (" + state[3] + ")" : "")
                        : context.getString(R.string.ekreta_new))
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
            n.setContentIntent(pIntent);
        } else {
            n.addAction(android.R.drawable.ic_menu_edit, context.getString(R.string.open_app), mainIntent);
            n.setContentIntent(mainIntent);
        }
        Notification notification = new Notification.BigTextStyle(n)
                .bigText(state[0] == 0 ? (context.getString(R.string.new_grade) + "\n" + subjects + oldtext) : state[0] == 1 ? context.getString(R.string.wrong_username_not) + (state.length == 4 ? " (" + state[3] + ")"
                        : "") : context.getString(R.string.ekreta_new)).build();
        notificationManager.notify(state[0], notification);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
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

    static void notifyIfStandinsChanged(int[] state, Context context, String classs, String subjects, int numberoflessons) {
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
        n.setContentIntent(epIntent);
        Notification notification = new Notification.BigTextStyle(n)
                .bigText((state[0] == 0 ? context.getString(R.string.new_substitution2) + " (" + classs + ")" + subjects : context.getString(R.string.no_new_substitution2) + " (" + classs + ")")).build();
        notification.number = numberoflessons;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notificationManager.notify(STANDINS_ID, notification);
    }
}
