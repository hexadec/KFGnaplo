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
//		final Preference ignore_lessons = findPreference("ignore_lessons");
		final Preference nightmode = findPreference("nightmode");
		final EditTextPreference url2 = (EditTextPreference)url;
		final CheckBoxPreference notify2 = (CheckBoxPreference)notify;
		final ListPreference interval2 = (ListPreference)interval;
		if (url2.getText()!=null){
		if (url2.getText().length()>=45){
			url2.setSummary("Kattints ide a szerkesztéshez");
		}
		}
		if (!notify2.isChecked()){
			interval.setEnabled(false);
			vibrate.setEnabled(false);
			flash.setEnabled(false);
			manual_check.setEnabled(false);
//			ignore_lessons.setEnabled(false);
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
				if (url2.getText()==null){
					Toast.makeText(MainActivity.this,"Másold be a GYIA-linket!",Toast.LENGTH_SHORT).show();
					return true;
				}
				if (url2.getText().length()<30){
					Toast.makeText(MainActivity.this,"Másold be a GYIA-linket!",Toast.LENGTH_SHORT).show();
					return true;
				}
				Toast.makeText(MainActivity.this,"Ellenőrzés...",Toast.LENGTH_SHORT).show();
				Intent i = new Intent("hu.kfg.naplo.CHECK_NOW");
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
					if (((String)obj).length()<45){
						url2.setSummary("Másold be a GYIA kódban található linket! (Katt ide)");
						url2.setText("");
						Point point = new Point();
						getWindowManager().getDefaultDisplay().getSize(point);
						Toast t = Toast.makeText(MainActivity.this,"Hibás URL!",Toast.LENGTH_SHORT);
						t.setGravity(Gravity.TOP,0,point.y/4);
						t.show();
						return false;
					} else if (((String)obj).startsWith("http://naplo.karinthy.hu/app")||((String)obj).startsWith("https://naplo.karinthy.hu/app")){
						url2.setSummary("Kattints ide a szerkesztéshez");
					} else {
						url2.setSummary("Másold be a GYIA kódban található linket! (Katt ide)");
						url2.setText("");
						Toast.makeText(MainActivity.this,"Érvénytelen URL!",Toast.LENGTH_SHORT).show();
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
					android.content.pm.PackageInfo pInfo = null;
					try {
						pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
					} catch (Exception e){}
					String version = pInfo.versionName;
					AlertDialog.Builder adb =new AlertDialog.Builder(MainActivity.this);
					adb.setTitle("Névjegy");
					adb.setPositiveButton("Ok",null);
					adb.setCancelable(true);
					TextView messageText = new TextView(MainActivity.this);
					messageText.setText(Html.fromHtml("<b>Fejlesztő:</b><br/>Cseh András<br/><b>App verziója:</b><br/>"+version+"<br/>Értesítés ikonja: Freepik<br/>Alkalmazás ikonja: ToffeeNut"));
					messageText.setGravity(Gravity.CENTER_HORIZONTAL);
					messageText.setTextAppearance(MainActivity.this,android.R.style.TextAppearance_Medium);
					adb.setView(messageText);
					adb.show();
					return true;
				}
			});
			
		open_in_browser.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
				public boolean onPreferenceClick(Preference pref){
					if (url2.getText()!=null&&url2.getText().length()>=45) {
						Intent intent = new Intent(Intent.ACTION_VIEW);
						intent.setData(Uri.parse(url2.getText()));
						startActivity(intent);
					} else {
						Toast.makeText(MainActivity.this,"Másold be a GYIA-linket!",Toast.LENGTH_SHORT).show();
					}
					return true;
				}
			});
    }
}
