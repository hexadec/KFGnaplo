package hu.kfg.naplo;

import android.app.*;
import android.app.job.JobScheduler;
import android.graphics.Point;
import android.os.*;
import android.preference.*;
import android.content.*;
import android.provider.Settings;
import android.util.Log;
import android.widget.*;
import android.text.*;
import android.view.*;
import android.net.*;

import com.evernote.android.job.JobManager;

public class MainActivity extends PreferenceActivity {

    protected static int URL_MIN_LENGTH = 45;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Preference url = findPreference("url");
        final Preference manual_check = findPreference("manual_check");
        final Preference about = findPreference("about");
        //final Preference notify = findPreference("notify");
        final Preference interval = findPreference("auto_check_interval");
        final Preference vibrate = findPreference("vibrate");
        final Preference flash = findPreference("flash");
        final Preference open_in_browser = findPreference("open_in_browser");
        final Preference nightmode = findPreference("nightmode");
        final Preference notification_mode = findPreference("notification_mode");
        final EditTextPreference clas = (EditTextPreference) findPreference("class");
        final EditTextPreference url2 = (EditTextPreference) url;
        //final CheckBoxPreference notify2 = (CheckBoxPreference)notify;
        if (url2.getText() != null) {
            if (url2.getText().length() >= URL_MIN_LENGTH) {
                url2.setSummary(getString(R.string.click2edit));
            } else {
                findPreference("grades").setEnabled(false);
            }
        }
        interval.setSummary(String.format(getString(R.string.apprx), interval.getSummary()));
        if (prefs.getString("notification_mode", "false").equals("false")) {
            interval.setEnabled(false);
            vibrate.setEnabled(false);
            flash.setEnabled(false);
            manual_check.setEnabled(false);
            nightmode.setEnabled(false);
        } else {
            //JobManagerService.scheduleJob(this,false);
            CheckerJob.scheduleJob();
        }
        if (clas.getText() != null && clas.getText().length() > 2) {
            clas.setSummary(clas.getText());
        }
        PowerManager pwm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= 23 && !pwm.isIgnoringBatteryOptimizations("hu.kfg.naplo")) {
            Toast.makeText(this, R.string.battery_opt, Toast.LENGTH_LONG).show();
            if (Build.VERSION.SDK_INT < 26 && android.os.Build.MANUFACTURER.equalsIgnoreCase("huawei") && !prefs.getBoolean("huawei_protected", false)) {
                Intent battOpt = new Intent();
                battOpt.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                try {
                    startActivity(battOpt);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, R.string.battery_opt_err_huawei, Toast.LENGTH_LONG).show();
                } finally {
                    prefs.edit().putBoolean("huawei_protected", true).commit();
                }
            } else {
                Intent battOpt = new Intent();
                battOpt.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                if (battOpt.resolveActivity(getPackageManager()) == null) {
                    Toast.makeText(this, R.string.battery_opt_err, Toast.LENGTH_LONG).show();
                } else {
                    startActivity(battOpt);
                }
            }
        } else if (Build.VERSION.SDK_INT < 26 && android.os.Build.MANUFACTURER.equalsIgnoreCase("huawei") && !prefs.getBoolean("huawei_protected", false)) {
            Intent battOpt = new Intent();
            battOpt.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
            try {
                startActivity(battOpt);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, R.string.battery_opt_err_huawei, Toast.LENGTH_LONG).show();
            } finally {
                prefs.edit().putBoolean("huawei_protected", true).commit();
            }
        }

        if (!prefs.getBoolean("inst", false)) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setMessage(Html.fromHtml(getString(R.string.instructions)));
            builder1.setCancelable(false);

            builder1.setPositiveButton(
                    "Ok",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            prefs.edit().putBoolean("inst", true).commit();
                            dialog.cancel();
                        }
                    });
            AlertDialog alert11 = builder1.create();
            alert11.show();
        }


        manual_check.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                ConnectivityManager cm =
                        (ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null &&
                        activeNetwork.isConnected();
                if (!isConnected) {
                    Toast.makeText(MainActivity.this, R.string.no_network_conn, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (url2.getText() == null) {
                    Toast.makeText(MainActivity.this, R.string.insert_code, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if (url2.getText().length() < URL_MIN_LENGTH) {
                    Toast.makeText(MainActivity.this, R.string.insert_code, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Toast.makeText(MainActivity.this, R.string.checking_now, Toast.LENGTH_SHORT).show();
                if (((ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo() == null
                        || !((ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().isConnected()) {
                    Toast.makeText(MainActivity.this, R.string.cannot_reach_site, Toast.LENGTH_SHORT).show();
                }
                /*Intent i = new Intent("hu.kfg.naplo.CHECK_NOW");
				i.putExtra("runnomatterwhat",true);
				i.putExtra("error",true);
				sendBroadcast(i);*/
                //JobManagerService.scheduleJob(MainActivity.this, false);
                ChangeListener.onRunJob(App.getContext(), new Intent("hu.kfg.naplo.CHECK_NOW").putExtra("runnomatterwhat", true).putExtra("error", true));
                CheckerJob.scheduleJob();
                return true;
            }
        });

        clas.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object obj) {
                if (((String) obj).length() < 3 || !((String) obj).contains(".")) {
                    clas.setSummary("Írd be az osztályodat (nagybetűkkel) pl. 9.AK/10.A");
                } else {
                    clas.setSummary(((String) obj));
                }
                return true;
            }
        });
		
		/*notify2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference pref, Object obj){
				if (obj instanceof Boolean){
					interval.setEnabled(((Boolean)obj));
					vibrate.setEnabled(((Boolean)obj));
					flash.setEnabled(((Boolean)obj));
					manual_check.setEnabled(((Boolean)obj));
//					ignore_lessons.setEnabled(((Boolean)obj));
					nightmode.setEnabled(((Boolean)obj));
					if (((Boolean)obj)&&url2.getText()!=null&&url2.getText().length()>=URL_MIN_LENGTH){
						CheckerJob.scheduleJob();
					} else {
						//JobScheduler jobScheduler = (JobScheduler) MainActivity.this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
						//jobScheduler.cancelAll();
						JobManager.instance().cancelAll();
					}
					
				}
				return true;
			}
		});*/

        url2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object obj) {
                if (((String) obj).length() < URL_MIN_LENGTH) {
                    url2.setSummary(getString(R.string.copythelink));
                    url2.setText("");
                    Point point = new Point();
                    getWindowManager().getDefaultDisplay().getSize(point);
                    Toast t = Toast.makeText(MainActivity.this, R.string.invalid_url, Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.TOP, 0, point.y / 4);
                    t.show();
                    findPreference("grades").setEnabled(false);
                    prefs.edit().putInt("numberofnotes", 0).putString("lastSHA", "AAA").commit();
                    DBHelper db = new DBHelper(MainActivity.this);
                    db.cleanDatabase();
                    return false;
                } else if (((String) obj).startsWith("http://naplo.karinthy.hu/app") || ((String) obj).startsWith("https://naplo.karinthy.hu/app")) {
                    url2.setSummary(getString(R.string.click2edit));
                    findPreference("grades").setEnabled(true);
                } else {
                    url2.setSummary(getString(R.string.copythelink));
                    url2.setText("");
                    Toast.makeText(MainActivity.this, R.string.invalid_url, Toast.LENGTH_SHORT).show();
                    findPreference("grades").setEnabled(false);
                    prefs.edit().putInt("numberofnotes", 0).putString("lastSHA", "AAA").commit();
                    DBHelper db = new DBHelper(MainActivity.this);
                    db.cleanDatabase();
                    return false;
                }
                return true;
            }
        });

        interval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object obj) {
                ListPreference lp = (ListPreference) pref;
                interval.setSummary(String.format(getString(R.string.apprx), lp.getEntries()[lp.findIndexOfValue((String) obj)]));
                new Thread(new Runnable() {
                    public void run() {
                        CheckerJob.scheduleJob();
                    }

                }).start();
                return true;
            }
        });

        notification_mode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object obj) {
                //ListPreference lp = (ListPreference) pref;
                //String value = (lp.getEntries()[lp.findIndexOfValue((String) obj)]).toString();
                //Log.e("E",value);
                switch ((String)obj) {
                    case "true":
                        interval.setEnabled(true);
                        vibrate.setEnabled(true);
                        flash.setEnabled(true);
                        manual_check.setEnabled(true);
                        nightmode.setEnabled(true);
                        clas.setEnabled(true);
                        url.setEnabled(true);
                        break;
                    case "false":
                        interval.setEnabled(false);
                        vibrate.setEnabled(false);
                        flash.setEnabled(false);
                        manual_check.setEnabled(false);
                        nightmode.setEnabled(false);
                        break;
                    case "naplo":
                        clas.setEnabled(false);
                        url.setEnabled(true);
                        break;
                    case "standins":
                        clas.setEnabled(true);
                        url.setEnabled(false);
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

        about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                String version = "0.0";
                android.content.pm.PackageInfo pInfo = null;
                try {
                    pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                    version = pInfo.versionName;
                } catch (Exception e) {
                }
                AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
                adb.setTitle(R.string.about);
                adb.setPositiveButton("Ok", null);
                adb.setCancelable(true);
                TextView messageText = new TextView(MainActivity.this);
                messageText.setText(Html.fromHtml(String.format(getString(R.string.about_text), version)));
                messageText.setGravity(Gravity.CENTER_HORIZONTAL);
                messageText.setTextAppearance(MainActivity.this, android.R.style.TextAppearance_Medium);
                adb.setView(messageText);
                adb.show();
                return true;
            }
        });

        open_in_browser.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                if (url2.getText() != null && url2.getText().length() >= URL_MIN_LENGTH) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(url2.getText()));
                    startActivity(intent);
                } else {
                    Toast.makeText(MainActivity.this, R.string.insert_code, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        findPreference("grades").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                Intent intent = new Intent(MainActivity.this, TableViewActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                startActivity(intent);
                return true;
            }
        });
    }
}
