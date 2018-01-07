package hu.kfg.naplo;

import android.app.*;
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
		}
		}
		if (!notify2.isChecked()){
			interval.setEnabled(false);
			vibrate.setEnabled(false);
			flash.setEnabled(false);
			manual_check.setEnabled(false);
			nightmode.setEnabled(false);
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
					AlarmManager alarmManager=(AlarmManager) getSystemService(Context.ALARM_SERVICE);
					Intent intente = new Intent(MainActivity.this, ChangeListener.class);
					intente.putExtra("triggered",true);
					PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intente,PendingIntent.FLAG_UPDATE_CURRENT );
					if (((Boolean)obj)){
						alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+5000,Long.valueOf(prefs.getString("auto_check_interval","180"))*60000,pendingIntent);
					} else {
						alarmManager.cancel(pendingIntent);
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
						return false;
					} else if (((String)obj).startsWith("http://naplo.karinthy.hu/app")||((String)obj).startsWith("https://naplo.karinthy.hu/app")){
						url2.setSummary(getString(R.string.click2edit));
					} else {
						url2.setSummary(getString(R.string.copythelink));
						url2.setText("");
						Toast.makeText(MainActivity.this, R.string.invalid_url, Toast.LENGTH_SHORT).show();
						return false;
					}
					return true;
				}
			});
			
		interval.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
				public boolean onPreferenceChange(Preference pref, Object obj){
					AlarmManager alarmManager=(AlarmManager) getSystemService(Context.ALARM_SERVICE);
					Intent intente = new Intent(MainActivity.this, ChangeListener.class);
					intente.putExtra("triggered",true);
					PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intente,PendingIntent.FLAG_UPDATE_CURRENT );
					alarmManager.cancel(pendingIntent);
					alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+5000,Long.valueOf((String)obj)*60000,pendingIntent);
					
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
