package hu.kfg.naplo;

import android.accounts.Account;
import android.accounts.AccountManager;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import hu.hexadec.killerwhale.OrcaManager;


public class MainActivity extends PreferenceActivity {

    protected static final int URL_MIN_LENGTH = 45;
    protected static final int CLASS_MIN_LENGTH = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //final Preference url = findPreference("url");
        final Preference manual_check = findPreference("manual_check");
        final Preference about = findPreference("about");
        final Preference interval = findPreference("auto_check_interval");
        final Preference vibrate = findPreference("vibrate");
        final Preference flash = findPreference("flash");
        final Preference ngrades = findPreference("not_grades");
        final Preference nstandins = findPreference("not_standins");
        final Preference nightmode = findPreference("nightmode");
        final Preference ignore = findPreference("ignore_lessons");
        final Preference grades = findPreference("grades");
        final EditTextPreference username = (EditTextPreference) findPreference("username");
        final ListPreference notification_mode = (ListPreference) findPreference("notification_mode");
        final EditTextPreference clas = (EditTextPreference) findPreference("class");
        if (username.getText() != null && username.getText().length() >= 1) {
            username.setTitle(getString(R.string.username) + ": " + username.getText());
        }

        PreferenceCategory cat = (PreferenceCategory) findPreference("main");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            cat.removePreference(vibrate);
            cat.removePreference(flash);
            try {
                ChangeListener.setUpNotificationChannels(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ngrades.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, ChangeListener.CHANNEL_GRADES);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                    return true;
                }
            });
            nstandins.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
                    intent.putExtra(Settings.EXTRA_CHANNEL_ID, ChangeListener.CHANNEL_STANDINS);
                    intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
                    startActivity(intent);
                    return true;
                }
            });
        } else {
            cat.removePreference(ngrades);
            cat.removePreference(nstandins);
        }

        final InputFilter teacherFilter = new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String notAllowed = "0123456789?!";
                if (source != null && notAllowed.contains(source)) {
                    return "";
                }
                return null;
            }
        };


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

        interval.setSummary(String.format(getString(R.string.apprx), interval.getSummary()));

        switch (prefs.getString("notification_mode", "false")) {
            case ChangeListener.MODE_FALSE:
                interval.setEnabled(false);
                vibrate.setEnabled(false);
                flash.setEnabled(false);
                manual_check.setEnabled(false);
                nightmode.setEnabled(false);
                clas.setEnabled(false);
                //url.setEnabled(false);
                ignore.setEnabled(false);
                ngrades.setEnabled(false);
                nstandins.setEnabled(false);
                JobManager.instance().cancelAll();
                break;
            case ChangeListener.MODE_NAPLO:
                clas.setEnabled(false);
                ignore.setEnabled(false);
                nstandins.setEnabled(false);
                CheckerJob.runJobImmediately();
                break;
            case ChangeListener.MODE_TEACHER:
                clas.getEditText().setFilters(new InputFilter[]{teacherFilter});
                clas.setSummary(R.string.teacher_hint);
                clas.setTitle(R.string.teacher_name);
//                url.setEnabled(false);
                ignore.setEnabled(false);
                ngrades.setEnabled(false);
                grades.setEnabled(false);
                CheckerJob.scheduleJob();
                break;
            case ChangeListener.MODE_STANDINS:
//                url.setEnabled(false);
                ngrades.setEnabled(false);
            default:
                clas.getEditText().setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                clas.getEditText().setFilters(new InputFilter[]{classFilter, new InputFilter.LengthFilter(5)});
                clas.setSummary(R.string.yourclass_sum);
                clas.setTitle(R.string.yourclass);
                CheckerJob.scheduleJob();
        }
        if (clas.getText() != null && clas.getText().length() > 2) {
            clas.setSummary(clas.getText());
        }

        if (!prefs.getBoolean("inst", false) && !Intent.ACTION_SEND.equals(getIntent().getAction())) {
            showWelcomeDialog();
        } /* else {
            if (Intent.ACTION_SEND.equals(getIntent().getAction()) && getIntent().hasExtra(Intent.EXTRA_TEXT)) {
                if (!getIntent().getStringExtra(Intent.EXTRA_TEXT)
                        .startsWith("https://naplo.karinthy.hu/app/interface.php?view=v_slip")) {
                    Log.w("MainActivity share err", getIntent().toString());
                    Toast t = Toast.makeText(this, R.string.only_gyia_url, Toast.LENGTH_LONG);
                    t.setGravity(Gravity.CENTER, 0, 0);
                    t.show();
                    finish();
                } else {
                    AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
                    adb.setIcon(android.R.drawable.ic_dialog_alert);
                    adb.setMessage(R.string.override_gyia);
                    adb.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            prefs.edit().putString("url", getIntent().getStringExtra(Intent.EXTRA_TEXT)).commit();
                            Toast.makeText(MainActivity.this, R.string.url_updated, Toast.LENGTH_SHORT).show();
                            setIntent(new Intent(Intent.ACTION_MAIN));
                            onCreate(null);
                        }
                    });
                    adb.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    adb.show();
                }
            } else {
                showOptimizationDialog(getSharedPreferences("optimization_preferences", MODE_PRIVATE));
            }
        }*/


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
                if ((clas.getText() == null || clas.getText().length() < CLASS_MIN_LENGTH) && (notification_mode.getValue().equals("standins") || notification_mode.getValue().equals("true"))) {
                    Toast.makeText(MainActivity.this, R.string.insert_class, Toast.LENGTH_SHORT).show();
                    return true;
                }
                Toast.makeText(MainActivity.this, R.string.checking_now, Toast.LENGTH_SHORT).show();

                ChangeListener.onRunJob(App.getContext(), new Intent("hu.kfg.naplo.CHECK_NOW").putExtra("error", true).putExtra("show_anyway", true));
                CheckerJob.scheduleJob();
                return true;
            }
        });

        clas.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object obj) {
                String mode = prefs.getString("notification_mode", ChangeListener.MODE_FALSE);
                if (((String) obj).length() < 3 || (!((String) obj).contains(".") && !mode.equals(ChangeListener.MODE_TEACHER))) {
                    clas.setSummary(mode.equals(ChangeListener.MODE_TEACHER) ? R.string.teacher_hint : R.string.insert_class);
                } else {
                    clas.setSummary(((String) obj));
                }
                return true;
            }
        });

        /*url2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            public boolean onPreferenceChange(Preference pref, Object obj) {
                if (((String) obj).length() < URL_MIN_LENGTH) {
                    url2.setSummary(getString(R.string.copythelink));
                    url2.setText("");
                    Point point = new Point();
                    getWindowManager().getDefaultDisplay().getSize(point);
                    Toast t = Toast.makeText(MainActivity.this, R.string.invalid_url, Toast.LENGTH_SHORT);
                    t.setGravity(Gravity.TOP, 0, point.y / 4);
                    t.show();
                    grades.setEnabled(false);
                    prefs.edit().putInt("numberofnotes", 0).putString("lastSHA", "AAA").commit();
                    DBHelper db = new DBHelper(MainActivity.this);
                    db.cleanDatabase();
                    return false;
                } else if (((String) obj).startsWith("http://naplo.karinthy.hu/app") || ((String) obj).startsWith("https://naplo.karinthy.hu/app")) {
                    url2.setSummary(getString(R.string.click2edit));
                    grades.setEnabled(true);
                } else {
                    url2.setSummary(getString(R.string.copythelink));
                    url2.setText("");
                    Toast.makeText(MainActivity.this, R.string.invalid_url, Toast.LENGTH_SHORT).show();
                    grades.setEnabled(false);
                    prefs.edit().putInt("numberofnotes", 0).putString("lastSHA", "AAA").commit();
                    DBHelper db = new DBHelper(MainActivity.this);
                    db.cleanDatabase();
                    return false;
                }
                return true;
            }
        });*/

        username.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final String uname = (String) newValue;
                if (uname == null || uname.length() <= 1) {
                    grades.setEnabled(false);
                    return false;
                }
                preference.setTitle(getString(R.string.username) + ": " + uname);
                final EditText pwdfield = new EditText(MainActivity.this);
                pwdfield.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                AlertDialog.Builder ad = new AlertDialog.Builder(MainActivity.this);
                ad.setTitle(R.string.enter_pwd);
                ad.setView(pwdfield);
                ad.setPositiveButton(R.string.change_pwd, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //Remove old account
                        AccountManager accountManager = AccountManager.get(MainActivity.this);
                        Account account = new Account(username.getText(), getString(R.string.account_type));
                        accountManager.removeAccount(account, null, null);
                        //Create new one
                        accountManager = AccountManager.get(MainActivity.this);
                        account = new Account(uname, getString(R.string.account_type));
                        boolean success = accountManager.addAccountExplicitly(account, pwdfield.getText().toString(), null);
                        if (success) {
                            Log.d("KFGNaplo", "Account created");
                        } else {
                            Toast.makeText(MainActivity.this, R.string.unknown_error, Toast.LENGTH_SHORT).show();
                            Log.d("KFGNaplo", "Account creation failed");
                        }
                    }
                });
                if (uname.equals(username.getText())) {
                    ad.setNegativeButton(R.string.keep_pwd, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AccountManager accountManager = AccountManager.get(MainActivity.this);
                            Account account = new Account(username.getText(), getString(R.string.account_type));
                            accountManager.setPassword(account, pwdfield.getText().toString());
                        }
                    });
                }
                ad.show();
                grades.setEnabled(true);
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

    void showOptimizationDialog(final SharedPreferences preferences) {
        if (preferences.getBoolean("never_show_opt_dialog", false)) {
            return;
        }
        final SharedPreferences.Editor editor = preferences.edit();
        PowerManager pwm = (PowerManager) getSystemService(POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= 23) {
            //Toast.makeText(this, R.string.battery_opt, Toast.LENGTH_LONG).show();
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
                        if (battOpt.resolveActivity(getPackageManager()) == null) {
                            Toast.makeText(MainActivity.this, R.string.battery_opt_err, Toast.LENGTH_LONG).show();
                        } else {
                            startActivity(battOpt);
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

    void showWelcomeDialog() {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage(Html.fromHtml(getString(R.string.instructions)));
        builder1.setCancelable(false);

        builder1.setPositiveButton(
                "Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean("inst", true).apply();
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
