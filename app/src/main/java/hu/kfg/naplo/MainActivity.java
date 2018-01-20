package hu.kfg.naplo;

import android.app.*;
import android.app.job.JobScheduler;
import android.graphics.Point;
import android.os.*;
import android.preference.*;
import android.content.*;
import android.widget.*;
import android.text.*;
import android.view.*;
import android.net.*;

public class MainActivity extends PreferenceActivity
{

	protected static int URL_MIN_LENGTH = 45;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.prefs);
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		final Preference url = findPreference("url");
		final Preference manual_check = findPreference("manual_check");
		final Preference about = findPreference("about");
		final Preference notify = findPreference("notify");
		final Preference interval = findPreference("auto_check_interval");
		final Preference vibrate = findPreference("vibrate");
		final Preference flash = findPreference("flash");
		final Preference open_in_browser = findPreference("open_in_browser");
		final Preference nightmode = findPreference("nightmode");
		final EditTextPreference url2 = (EditTextPreference)url;
		final CheckBoxPreference notify2 = (CheckBoxPreference)notify;
		if (url2.getText()!=null){
		if (url2.getText().length()>=URL_MIN_LENGTH){
			url2.setSummary(getString(R.string.click2edit));
		} else {
			findPreference("grades").setEnabled(false);
		}
		}
		interval.setSummary(String.format(getString(R.string.apprx),interval.getSummary()));
		if (!notify2.isChecked()){
			interval.setEnabled(false);
			vibrate.setEnabled(false);
			flash.setEnabled(false);
			manual_check.setEnabled(false);
			nightmode.setEnabled(false);
		} else {
			JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
			if (jobScheduler.getAllPendingJobs()==null||jobScheduler.getAllPendingJobs().size()<1) {
				jobScheduler.cancelAll();
				JobManagerService.scheduleJob(this,false);
			}
		}
		if (!prefs.getBoolean("inst",false)) {
			AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
			builder1.setMessage(Html.fromHtml(getString(R.string.instructions)));
			builder1.setCancelable(false);

			builder1.setPositiveButton(
					"Ok",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							prefs.edit().putBoolean("inst",true).commit();
							dialog.cancel();
						}
					});
			AlertDialog alert11 = builder1.create();
			alert11.show();
		}
		
		manual_check.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference pref){
				ConnectivityManager cm =
						(ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
				NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
				boolean isConnected = activeNetwork != null &&
						activeNetwork.isConnected();
				if (!isConnected) {
					Toast.makeText(MainActivity.this, R.string.no_network_conn, Toast.LENGTH_SHORT).show();
					return true;
				}
				if (url2.getText()==null){
					Toast.makeText(MainActivity.this,R.string.insert_code, Toast.LENGTH_SHORT).show();
					return true;
				}
				if (url2.getText().length()<URL_MIN_LENGTH){
					Toast.makeText(MainActivity.this,R.string.insert_code, Toast.LENGTH_SHORT).show();
					return true;
				}
				Toast.makeText(MainActivity.this, R.string.checking_now, Toast.LENGTH_SHORT).show();
				if (((ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo()==null
						||!((ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().isConnected()){
					Toast.makeText(MainActivity.this, R.string.cannot_reach_site, Toast.LENGTH_SHORT).show();
				}
				Intent i = new Intent("hu.kfg.naplo.CHECK_NOW");
				i.putExtra("runnomatterwhat",true);
				i.putExtra("error",true);
				sendBroadcast(i);
				return true;
			}
		});
		
		notify2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
			public boolean onPreferenceChange(Preference pref, Object obj){
				if (obj instanceof Boolean){
					interval.setEnabled(((Boolean)obj));
					vibrate.setEnabled(((Boolean)obj));
					flash.setEnabled(((Boolean)obj));
					manual_check.setEnabled(((Boolean)obj));
//					ignore_lessons.setEnabled(((Boolean)obj));
					nightmode.setEnabled(((Boolean)obj));
					if (((Boolean)obj)&&url2.getText()!=null&&url2.getText().length()>=URL_MIN_LENGTH){
						JobScheduler jobScheduler = (JobScheduler) MainActivity.this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
						if (jobScheduler.getAllPendingJobs()!=null&&jobScheduler.getAllPendingJobs().size()>0) {
							jobScheduler.cancelAll();
						}
						JobManagerService.scheduleJob(MainActivity.this, false);
					} else {
						JobScheduler jobScheduler = (JobScheduler) MainActivity.this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
						if (jobScheduler.getAllPendingJobs()!=null&&jobScheduler.getAllPendingJobs().size()>0) {
							jobScheduler.cancelAll();
						}
					}
					
				}
				return true;
			}
		});
		
		url2.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
				public boolean onPreferenceChange(Preference pref, Object obj){
					if (((String)obj).length()<URL_MIN_LENGTH){
						url2.setSummary(getString(R.string.copythelink));
						url2.setText("");
						Point point = new Point();
						getWindowManager().getDefaultDisplay().getSize(point);
						Toast t = Toast.makeText(MainActivity.this, R.string.invalid_url, Toast.LENGTH_SHORT);
						t.setGravity(Gravity.TOP,0,point.y/4);
						t.show();
						findPreference("grades").setEnabled(false);
						return false;
					} else if (((String)obj).startsWith("http://naplo.karinthy.hu/app")||((String)obj).startsWith("https://naplo.karinthy.hu/app")){
						url2.setSummary(getString(R.string.click2edit));
						findPreference("grades").setEnabled(true);
					} else {
						url2.setSummary(getString(R.string.copythelink));
						url2.setText("");
						Toast.makeText(MainActivity.this, R.string.invalid_url, Toast.LENGTH_SHORT).show();
						findPreference("grades").setEnabled(false);
						return false;
					}
					return true;
				}
			});
			
		interval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
				public boolean onPreferenceChange(Preference pref, Object obj){
					ListPreference lp = (ListPreference) pref;
					interval.setSummary(String.format(getString(R.string.apprx),lp.getEntries()[lp.findIndexOfValue((String)obj)]));
					JobScheduler jobScheduler = (JobScheduler) MainActivity.this.getSystemService(Context.JOB_SCHEDULER_SERVICE);
					if (jobScheduler.getAllPendingJobs()!=null&&jobScheduler.getAllPendingJobs().size()>0) {
						jobScheduler.cancelAll();
					}
					new Thread(new Runnable() {
						@Override
						public void run() {
							SystemClock.sleep(1000);
							JobManagerService.scheduleJob(MainActivity.this, false);
						}
					}).start();
					return true;
				}
			});
		
		about.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
				public boolean onPreferenceClick(Preference pref){
					String version = "0.0";
					android.content.pm.PackageInfo pInfo = null;
					try {
						pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
						version = pInfo.versionName;
					} catch (Exception e){}
					AlertDialog.Builder adb =new AlertDialog.Builder(MainActivity.this);
					adb.setTitle(R.string.about);
					adb.setPositiveButton("Ok",null);
					adb.setCancelable(true);
					TextView messageText = new TextView(MainActivity.this);
					messageText.setText(Html.fromHtml(String.format(getString(R.string.about_text), version)));
					messageText.setGravity(Gravity.CENTER_HORIZONTAL);
					messageText.setTextAppearance(MainActivity.this,android.R.style.TextAppearance_Medium);
					adb.setView(messageText);
					adb.show();
					return true;
				}
			});
			
		open_in_browser.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
				public boolean onPreferenceClick(Preference pref){
					if (url2.getText()!=null&&url2.getText().length()>=URL_MIN_LENGTH) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(url2.getText()));
						startActivity(intent);
					} else {
						Toast.makeText(MainActivity.this, R.string.insert_code, Toast.LENGTH_SHORT).show();
					}
					return true;
				}
			});
		findPreference("grades").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference pref){
				Intent intent = new Intent(MainActivity.this, TableViewActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
				startActivity(intent);
				return true;
			}
		});
    }

    /*public static void showLilla(String[] args) {
		String toworkwith = args[1];
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < toworkwith.length(); i++) {
			if (i%2==0) sb.append(toworkwith.charAt(i));
		}
		System.out.println(sb.toString());
	}*/
}
