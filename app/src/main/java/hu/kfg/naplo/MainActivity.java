package hu.kfg.naplo;

import android.app.*;
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

    protected static final int URL_MIN_LENGTH = 45;
    protected static final int CLASS_MIN_LENGTH = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        final Preference url = findPreference("url");
        final Preference manual_check = findPreference("manual_check");
        final Preference about = findPreference("about");
        final Preference interval = findPreference("auto_check_interval");
        final Preference vibrate = findPreference("vibrate");
        final Preference flash = findPreference("flash");
        final Preference open_in_browser = findPreference("open_in_browser");
        final Preference nightmode = findPreference("nightmode");
        final ListPreference notification_mode = (ListPreference) findPreference("notification_mode");
        final EditTextPreference clas = (EditTextPreference) findPreference("class");
        final EditTextPreference url2 = (EditTextPreference) url;
        if (url2.getText() != null) {
            if (url2.getText().length() >= URL_MIN_LENGTH) {
                url2.setSummary(getString(R.string.click2edit));
            } else {
                findPreference("grades").setEnabled(false);
            }
        }
        final boolean not_disabled = prefs.getString("notification_mode", "false").equals("false");
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_USER_PRESENT) && !not_disabled) {
                    SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    long checked = p.getLong("last_check", 0L);
                    if (System.currentTimeMillis() - checked > (long) (Long.valueOf(
                            p.getString("auto_check_interval", "300")) * 1.2 * CheckerJob.MINUTE)) {
                        CheckerJob.runJobImmediately();
                        Log.w(ChangeListener.TAG, "USER_PRESENT forced check...");
                    }
                }
            }
        }, new IntentFilter(Intent.ACTION_USER_PRESENT));
        interval.setSummary(String.format(getString(R.string.apprx), interval.getSummary()));
        //SWITCH
        switch (prefs.getString("notification_mode", "false")) {
            case "false":
                interval.setEnabled(false);
                vibrate.setEnabled(false);
                flash.setEnabled(false);
                manual_check.setEnabled(false);
                nightmode.setEnabled(false);
                clas.setEnabled(false);
                url.setEnabled(false);
                JobManager.instance().cancelAll();
                break;
            case "naplo":
                clas.setEnabled(false);
                CheckerJob.runJobImmediately();
                break;
            case "teacher":
                InputFilter filter = new InputFilter() {

                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

                        if (source != null && "0123456789".contains(("" + source))) {
                            return "";
                        }
                        return null;
                    }
                };
                clas.getEditText().setFilters(new InputFilter[]{filter});
                clas.setSummary(R.string.teacher_hint);
                clas.setTitle(R.string.teacher_name);
                clas.getEditText().setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9)
                            return true;
                        return false;
                    }
                });
                url.setEnabled(false);
                CheckerJob.scheduleJob();
                break;
            case "standins":
                url.setEnabled(false);
            default:
                InputFilter fil = new InputFilter() {

                    @Override
                    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

                        if (source != null && !"0129.ABCDEIK+".contains(("" + source))) {
                            return "";
                        }
                        return null;
                    }
                };
                clas.getEditText().setFilters(new InputFilter[]{fil});
                clas.setSummary(R.string.yourclass_sum);
                clas.setTitle(R.string.yourclass);
                clas.getEditText().setOnKeyListener(new View.OnKeyListener() {
                    @Override
                    public boolean onKey(View v, int keyCode, KeyEvent event) {
                        return false;
                    }
                });
                CheckerJob.scheduleJob();
        }
        if (clas.getText() != null && clas.getText().length() > 2) {
            clas.setSummary(clas.getText());
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
                            showOptimizationDialog(prefs);
                            dialog.dismiss();
                        }
                    });
            builder1.setNegativeButton(
                    R.string.next_time,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            showOptimizationDialog(prefs);
                            dialog.cancel();
                        }
                    });
            AlertDialog alert11 = builder1.create();
            alert11.show();
        } else {
            showOptimizationDialog(prefs);
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
                if ((url2.getText() == null || url2.getText().length() < URL_MIN_LENGTH) && (notification_mode.getValue().equals("naplo") || notification_mode.getValue().equals("true"))) {
                    Toast.makeText(MainActivity.this, R.string.insert_code, Toast.LENGTH_SHORT).show();
                    return true;
                }
                if ((clas.getText() == null || clas.getText().length() < CLASS_MIN_LENGTH) && (notification_mode.getValue().equals("standins") || notification_mode.getValue().equals("true"))) {
                    Toast.makeText(MainActivity.this, R.string.insert_class, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Toast.makeText(MainActivity.this, R.string.checking_now, Toast.LENGTH_SHORT).show();

                ChangeListener.onRunJob(App.getContext(), new Intent("hu.kfg.naplo.CHECK_NOW").putExtra("runnomatterwhat", true).putExtra("error", true));
                CheckerJob.scheduleJob();
                return true;
            }
        });

        clas.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object obj) {
                if (((String) obj).length() < 3 || !((String) obj).contains(".")) {
                    clas.setSummary(prefs.getString("notification_mode",ChangeListener.MODE_FALSE).equals(ChangeListener.MODE_TEACHER)? R.string.teacher_hint : R.string.insert_class);
                } else {
                    clas.setSummary(((String) obj));
                }
                return true;
            }
        });

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
                if (notification_mode.getValue().equals("teacher")) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SystemClock.sleep(1000);
                            finish();
                        }
                    }).start();
                }
                switch ((String) obj) {
                    case "true":
                        interval.setEnabled(true);
                        vibrate.setEnabled(true);
                        flash.setEnabled(true);
                        manual_check.setEnabled(true);
                        nightmode.setEnabled(true);
                        clas.setEnabled(true);
                        url.setEnabled(true);
                        CheckerJob.scheduleJob();
                        break;
                    case "false":
                        interval.setEnabled(false);
                        vibrate.setEnabled(false);
                        flash.setEnabled(false);
                        manual_check.setEnabled(false);
                        nightmode.setEnabled(false);
                        JobManager.instance().cancelAll();
                        break;
                    case "naplo":
                        clas.setEnabled(false);
                        url.setEnabled(true);
                        CheckerJob.scheduleJob();
                        break;
                    case "standins":
                        clas.setEnabled(true);
                        url.setEnabled(false);
                        CheckerJob.scheduleJob();
                        break;
                    case "teacher":
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                SystemClock.sleep(1000);
                                finish();
                            }
                        }).start();
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
                if (Build.VERSION.SDK_INT >= 21) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                }
                startActivity(intent);
                return true;
            }
        });
    }

    void showOptimizationDialog(final SharedPreferences prefs) {
        if (prefs.getBoolean("never_show_opt_dialog", false)) {
            return;
        }
        PowerManager pwm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= 23 && !pwm.isIgnoringBatteryOptimizations("hu.kfg.naplo")) {
            //Toast.makeText(this, R.string.battery_opt, Toast.LENGTH_LONG).show();
            if (Build.VERSION.SDK_INT < 26 && android.os.Build.MANUFACTURER.equalsIgnoreCase("huawei") && !prefs.getBoolean("huawei_protected", false)) {
                optimizationDialogWithOnClickListener(R.string.battery_opt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent battOpt = new Intent();
                        battOpt.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                        try {
                            startActivity(battOpt);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, R.string.battery_opt_err_huawei, Toast.LENGTH_LONG).show();
                        } finally {
                            prefs.edit().putBoolean("huawei_protected", true).commit();
                        }
                    }
                });
            } else {
                optimizationDialogWithOnClickListener(R.string.battery_opt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent battOpt = new Intent();
                        battOpt.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        if (battOpt.resolveActivity(getPackageManager()) == null) {
                            Toast.makeText(MainActivity.this, R.string.battery_opt_err, Toast.LENGTH_LONG).show();
                        } else {
                            startActivity(battOpt);
                        }
                    }
                });
            }
        } else if (Build.VERSION.SDK_INT < 26 && android.os.Build.MANUFACTURER.equalsIgnoreCase("huawei") && !prefs.getBoolean("huawei_protected", false)) {
            optimizationDialogWithOnClickListener(R.string.battery_opt, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent battOpt = new Intent();
                    battOpt.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"));
                    try {
                        startActivity(battOpt);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, R.string.battery_opt_err_huawei, Toast.LENGTH_LONG).show();
                    } finally {
                        prefs.edit().putBoolean("huawei_protected", true).commit();
                    }
                }
            });

        }
    }

    void optimizationDialogWithOnClickListener(int textResid, DialogInterface.OnClickListener runnable) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(textResid);
        builder1.setCancelable(false);

        builder1.setPositiveButton("OK", runnable);
        builder1.setNegativeButton(R.string.hide_forever,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("never_show_opt_dialog", true).apply();
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
}
