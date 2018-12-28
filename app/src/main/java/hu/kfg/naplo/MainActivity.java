package hu.kfg.naplo;

import android.app.*;
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

import java.io.FileNotFoundException;

import hu.hexadec.killerwhale.OrcaManager;
import hu.hexadec.textsecure.Cryptography;


public class MainActivity extends PreferenceActivity {

    protected static final int CLASS_MIN_LENGTH = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Apply chosen theme on startup
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("light_theme_mode", false)) {
            setTheme(R.style.AppThemeLight);
        }
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        final Preference manual_check = findPreference("manual_check");
        final Preference about = findPreference("about");
        final Preference interval = findPreference("auto_check_interval");
        final Preference vibrate = findPreference("vibrate");
        final Preference flash = findPreference("flash");
        final Preference ngrades = findPreference("not_grades");
        final Preference nstandins = findPreference("not_standins");
        final Preference nightmode = findPreference("nightmode");
        final Preference open_in_browser = findPreference("open_in_browser");
        final Preference grades = findPreference("grades");
        final Preference timetable = findPreference("timetable");
        final Preference autoignore = findPreference("timetable_autoignore");
        final Preference absences = findPreference("absences");
        final Preference common = findPreference("common_settings");
        final ListPreference notification_mode = (ListPreference) findPreference("notification_mode");
        final SwitchPreference lightTheme = (SwitchPreference) findPreference("light_theme_mode");


        if (prefs.getString("url", null) != null) {
            new GradesDB(MainActivity.this).cleanDatabase();
            prefs.edit().remove("url").commit();
        }

        //On Android 8.0+ (API 26, O) notification channels have to be used that are incompatible with older versions
        //Handle these changes here
        PreferenceCategory cat = (PreferenceCategory) findPreference("other");
        getListView().setDivider(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cat.removePreference(vibrate);
            cat.removePreference(flash);
            try {
                AppNotificationManager.setUpNotificationChannels(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ngrades.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, AppNotificationManager.CHANNEL_GRADES);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                    return true;
                }
            });
            nstandins.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, AppNotificationManager.CHANNEL_STANDINS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                    return true;
                }
            });
        } else {
            cat.removePreference(ngrades);
            cat.removePreference(nstandins);
        }

        //Filter unnecessary characters in teacher name input
        final InputFilter teacherFilter = new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String notAllowed = "0123456789?!()_";
                if (source != null && notAllowed.contains(source)) {
                    return "";
                }
                return null;
            }
        };

        //Filter unnecessary input in class input (needed: 0129.ABCDEKN)
        final InputFilter classFilter = new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String notAllowed = "345678?!:,_-+abcdefghijklmnopqrstuwvxzyFGHJLMNOPQRSTUWVXZY ";
                if (source != null && notAllowed.contains(source)) {
                    return "";
                }
                return null;
            }
        };

        //Set the correct summary of the interval field
        interval.setSummary(String.format(getString(R.string.apprx), interval.getSummary()));

        //Apply mode specific changes (hide unnecessary fields)
        switch (prefs.getString("notification_mode", "false")) {
            case ChangeListener.MODE_FALSE:
                interval.setEnabled(false);
                vibrate.setEnabled(false);
                flash.setEnabled(false);
                nightmode.setEnabled(false);
                autoignore.setEnabled(false);
                ngrades.setEnabled(false);
                nstandins.setEnabled(false);
                JobManager.instance().cancelAll();
                break;
            case ChangeListener.MODE_NAPLO:
                autoignore.setEnabled(false);
                nstandins.setEnabled(false);
                CheckerJob.scheduleJob();
                break;
            case ChangeListener.MODE_TEACHER:
                autoignore.setEnabled(false);
                ngrades.setEnabled(false);
                grades.setEnabled(false);
                timetable.setEnabled(false);
                absences.setEnabled(false);
                CheckerJob.scheduleJob();
                break;
            case ChangeListener.MODE_STANDINS:
                autoignore.setEnabled(false);
                ngrades.setEnabled(false);
            default:
                CheckerJob.scheduleJob();
        }

        if (!prefs.getBoolean("inst_kreta", false) && !Intent.ACTION_SEND.equals(getIntent().getAction())) {
            showWelcomeDialog();
        }

        //Show timetable window
        timetable.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(MainActivity.this, TimetableActivity.class));
                return false;
            }
        });

        //Open E-Kréta link in browser
        open_in_browser.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(ChangeListener.eURL));
                startActivity(intent);
                return true;
            }
        });

        //Show absences window
        absences.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent intent = new Intent(MainActivity.this, AbsencesActivity.class);
                if (Build.VERSION.SDK_INT >= 21) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                }
                startActivity(intent);
                return false;
            }
        });

        //Check for changes, but before check if the conditions are okay (network available, credentials available)
        manual_check.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                ConnectivityManager cm =
                        (ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
                if (cm == null || cm.getActiveNetworkInfo() == null) {
                    Toast.makeText(MainActivity.this, R.string.no_network_conn, Toast.LENGTH_SHORT).show();
                    return true;
                }
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                boolean isConnected = activeNetwork != null &&
                        activeNetwork.isConnected();
                if (!isConnected) {
                    Toast.makeText(MainActivity.this, R.string.no_network_conn, Toast.LENGTH_SHORT).show();
                    return true;
                }
                String clas = prefs.getString("class", "null");
                String username = prefs.getString("username", "null");
                String refresh_token = prefs.getString("refresh_token", "null");
                String password = null;
                String password_crypt = prefs.getString("password2", null);
                if (password_crypt != null && password_crypt.length() >= 4) {
                    Cryptography cr = new Cryptography();
                    password = cr.cryptThreedog(password_crypt, true, username);
                }
                if (refresh_token.length() < 2) {
                    if ((username.length() < 2 || password == null || password.length() < 2)
                            && (notification_mode.getValue().equals(ChangeListener.MODE_TRUE) || notification_mode.getValue().equals(ChangeListener.MODE_NAPLO))) {
                        Toast.makeText(MainActivity.this, R.string.incorrect_credentials, Toast.LENGTH_SHORT).show();
                        return true;

                    }
                }
                if ((clas.length() < CLASS_MIN_LENGTH) && (notification_mode.getValue().equals("standins") || notification_mode.getValue().equals("true"))) {
                    Toast.makeText(MainActivity.this, R.string.insert_class, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Toast.makeText(MainActivity.this, R.string.checking_now, Toast.LENGTH_SHORT).show();

                ChangeListener.onRunJob(App.getContext(), new Intent("hu.kfg.naplo.CHECK_NOW")
                        .putExtra("forced", true)
                        .putExtra("error", true)
                        .putExtra("show_anyway", true));
                CheckerJob.scheduleJob();
                return true;
            }
        });

        //Common login interface (class, plus E-Kréta)
        common.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle(R.string.login_credentials);
                dialog.setNegativeButton(R.string.cancel, null);
                dialog.setPositiveButton("Ok", null);
                LayoutInflater layoutInflater = (LayoutInflater) MainActivity.this.getSystemService(LAYOUT_INFLATER_SERVICE);
                final LinearLayout view;
                try {
                    view = (LinearLayout) layoutInflater.inflate(R.layout.login_dialog, null, false);
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                dialog.setView(view);
                final AlertDialog d = dialog.create();
                try {
                    d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                final EditText passwordField = view.findViewById(R.id.passwordField);
                final EditText usernameField = view.findViewById(R.id.usernameField);
                final EditText classField = view.findViewById(R.id.classField);
                if (notification_mode.getValue().equals(ChangeListener.MODE_TEACHER)) {
                    passwordField.setEnabled(false);
                    usernameField.setEnabled(false);
                    classField.setFilters(new InputFilter[]{teacherFilter});
                    ((TextView) view.findViewById(R.id.textView3)).setText(R.string.teacher_name);
                } else {
                    classField.setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    classField.setFilters(new InputFilter[]{classFilter, new InputFilter.LengthFilter(5)});
                    classField.setHint("9.AK / 12.IB / 11.D");
                }
                classField.setText(prefs.getString("class", ""));
                d.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(final DialogInterface dialog) {
                        Button action = d.getButton(AlertDialog.BUTTON_POSITIVE);
                        action.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ConnectivityManager cm =
                                        (ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
                                if (cm == null || cm.getActiveNetworkInfo() == null) {
                                    Toast t = Toast.makeText(MainActivity.this, R.string.no_network_conn, Toast.LENGTH_SHORT);
                                    t.setGravity(Gravity.CENTER, 0, 0);
                                    t.show();
                                    return;
                                }
                                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                                boolean isConnected = activeNetwork != null &&
                                        activeNetwork.isConnected();
                                if (!isConnected) {
                                    Toast t = Toast.makeText(MainActivity.this, R.string.no_network_conn, Toast.LENGTH_SHORT);
                                    t.setGravity(Gravity.CENTER, 0, 0);
                                    t.show();
                                    return;
                                }
                                final String cls = classField.getText().toString();
                                final String uname = usernameField.getText().toString();
                                final String pwd = passwordField.getText().toString();
                                if (notification_mode.getValue().equals(ChangeListener.MODE_TEACHER)) {
                                    if (cls.length() > 3) {
                                        prefs.edit().putString("class", cls).commit();
                                    } else {
                                        Toast t = Toast.makeText(MainActivity.this, R.string.teacher_hint, Toast.LENGTH_LONG);
                                        t.setGravity(Gravity.CENTER, 0, 0);
                                        t.show();
                                    }
                                    return;
                                }
                                if ((cls.length() < CLASS_MIN_LENGTH || !cls.contains("."))) {
                                    Toast.makeText(MainActivity.this, R.string.incorrect_class, Toast.LENGTH_SHORT).show();
                                    return;
                                } else if ((uname.length() < 3 || pwd.length() < 3)) {
                                    Toast.makeText(MainActivity.this, R.string.incorrect_credentials, Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                prefs.edit().putString("class", cls).commit();
                                Thread t = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            //Check validity by requesting the tokens
                                            ChangeListener.getToken(MainActivity.this, uname, pwd, true);
                                        } catch (java.net.UnknownHostException une) {
                                            une.printStackTrace();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    //Notify the user that the credentials are correct
                                                    Toast t = Toast.makeText(MainActivity.this, R.string.no_network_conn, Toast.LENGTH_LONG);
                                                    t.setGravity(Gravity.CENTER, 0, 0);
                                                    t.show();
                                                }
                                            });
                                            return;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    //Notify the user that the credentials are correct
                                                    Toast t = Toast.makeText(MainActivity.this, R.string.incorrect_credentials, Toast.LENGTH_LONG);
                                                    t.setGravity(Gravity.CENTER, 0, 0);
                                                    t.show();
                                                }
                                            });
                                            return;
                                        }
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                //Notify user that the credentials are incorrect
                                                Toast t = Toast.makeText(MainActivity.this, R.string.correct_credentials, Toast.LENGTH_LONG);
                                                t.setGravity(Gravity.CENTER, 0, 0);
                                                t.show();
                                            }
                                        });
                                        new AbsencesDB(MainActivity.this).cleanDatabase();
                                        new GradesDB(MainActivity.this).cleanDatabase();
                                        new TimetableDB(MainActivity.this).cleanDatabase();
                                        CheckerJob.scheduleJob();
                                    }
                                });
                                t.start();
                                dialog.dismiss();
                            }
                        });
                    }
                });
                d.show();
                if (notification_mode.getValue().equals(ChangeListener.MODE_TEACHER)) {
                    Toast t = Toast.makeText(MainActivity.this, R.string.teacher_hint, Toast.LENGTH_LONG);
                    t.setGravity(Gravity.CENTER, 0, 0);
                    t.show();
                } else {
                    AlertDialog.Builder warnPw = new AlertDialog.Builder(MainActivity.this);
                    warnPw.setMessage(R.string.change_password);
                    warnPw.setIcon(android.R.drawable.ic_dialog_alert);
                    warnPw.setTitle(R.string.warning);
                    warnPw.setPositiveButton("OK", null);
                    warnPw.setCancelable(false);
                    warnPw.show();
                }
                return false;
            }
        });

        //Again, restart app to apply changes, but give time to save changes
        lightTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                setTheme(((Boolean) newValue) ? R.style.AppThemeLight : R.style.AppTheme);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SystemClock.sleep(350);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                MainActivity.super.recreate();
                            }
                        });
                    }
                }).start();
                return true;
            }
        });

        //Handle the change of the interval preference setting
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

        //Restart the app, but give the system time to save the changes
        notification_mode.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object obj) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        SystemClock.sleep(250);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onCreate(null);
                            }
                        });
                    }
                }).start();
                return true;
            }
        });

        //Show general information dialog
        about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference pref) {
                String version = BuildConfig.VERSION_NAME;
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

        //Open grades table
        grades.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
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

    //Warn the user of vendor specific optimizations (stock AOSP system opt. should be removed)
    void showOptimizationDialog(final SharedPreferences preferences) {
        if (preferences.getBoolean("never_show_opt_dialog", false)) {
            return;
        }
        final SharedPreferences.Editor editor = preferences.edit();
        PowerManager pwm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            if (Build.VERSION.SDK_INT < 26 && android.os.Build.MANUFACTURER.equalsIgnoreCase("huawei") && !preferences.getBoolean("huawei_protected", false)) {
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
                            editor.putBoolean("huawei_protected", true).apply();
                        }

                    }
                }, preferences);
            } else {
                if (Build.VERSION.SDK_INT >= 26 && android.os.Build.MANUFACTURER.equalsIgnoreCase("huawei") && !preferences.getBoolean("huawei_protected", false)) {
                    optimizationDialogWithOnClickListener(R.string.battery_opt_huawei_26, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent battOpt = new Intent();
                            battOpt.setComponent(new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"));
                            try {
                                startActivity(battOpt);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, R.string.battery_opt_err_huawei_26, Toast.LENGTH_LONG).show();
                            } finally {
                                editor.putBoolean("huawei_protected", true).apply();
                            }

                        }
                    }, preferences);
                }
            }

            if (pwm != null && !pwm.isIgnoringBatteryOptimizations("hu.kfg.naplo")) {
                optimizationDialogWithOnClickListener(R.string.battery_opt, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent battOpt = new Intent();
                        battOpt.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        try {
                            if (battOpt.resolveActivity(getPackageManager()) == null) {
                                Toast.makeText(MainActivity.this, R.string.battery_opt_err, Toast.LENGTH_LONG).show();
                            } else {
                                startActivity(battOpt);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this, R.string.battery_opt_err, Toast.LENGTH_LONG).show();
                        }
                    }
                }, preferences);
            }

        } else if (Build.VERSION.SDK_INT < 23 && android.os.Build.MANUFACTURER.equalsIgnoreCase("huawei") && !preferences.getBoolean("huawei_protected", false)) {
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
                        editor.putBoolean("huawei_protected", true).apply();
                    }

                }
            }, preferences);

        }
        {
            //Additional vendor specific optimizations, mainly untested
            try {
                final OrcaManager orcaManager = new OrcaManager(this, new String[]{"Huawei"});
                Log.d("Vendor opt", "" + orcaManager.hasVendorOptimization());
                if (orcaManager.hasVendorOptimization() && !preferences.getBoolean("vendor_optimization_dialog", false)) {
                    optimizationDialogWithOnClickListener(R.string.battery_opt_vendor_specific, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (orcaManager.startEachIntent() == 0) {
                                Toast.makeText(MainActivity.this, R.string.battery_opt_err_vendor_specific, Toast.LENGTH_LONG).show();
                            }
                        }
                    }, preferences);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, R.string.battery_opt_err_vendor_specific, Toast.LENGTH_LONG).show();
            } finally {
                editor.putBoolean("vendor_optimization_dialog", true).apply();
            }
        }

    }

    //Separate method for saving space
    void optimizationDialogWithOnClickListener(int textResid, DialogInterface.OnClickListener runnable, final SharedPreferences prefs) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(textResid);
        builder1.setCancelable(false);

        builder1.setPositiveButton("OK", runnable);
        builder1.setNegativeButton(R.string.hide_forever,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        prefs.edit().putBoolean("never_show_opt_dialog", true).commit();
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

    //Tell the user how the program works (basically)
    void showWelcomeDialog() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(Html.fromHtml(getString(R.string.instructions)));
        builder1.setCancelable(false);

        builder1.setPositiveButton(
                "Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("inst_kreta", true).apply();
                        showOptimizationDialog(getSharedPreferences("optimization_preferences", MODE_PRIVATE));
                        dialog.dismiss();
                    }
                });
        builder1.setNegativeButton(
                R.string.next_time,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        showOptimizationDialog(getSharedPreferences("optimization_preferences", MODE_PRIVATE));
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
}
