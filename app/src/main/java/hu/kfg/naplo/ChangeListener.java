package hu.kfg.naplo;

import android.content.*;
import android.preference.*;

import android.net.*;
import android.os.*;
import android.service.notification.StatusBarNotification;
import android.util.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;
import java.io.*;
import android.widget.*;
import android.app.*;

import java.text.*;

public class ChangeListener extends BroadcastReceiver
{
	
	private static final Handler showSuccessToast = new Handler() {
		public void handleMessage(Message msg) {
			
		}
	};

	public static final int NIGHTMODE_START = 2230;
	public static final int NIGHTMODE_STOP = 530;
	final static String TAG = "KFGnaplo-check";

	static boolean running = false;
	
	@Override
	public void onReceive(final Context context, final Intent intent){
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		if (!pref.getBoolean("notify",false)) {
			AlarmManager alarmManager=(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent intente = new Intent(context, ChangeListener.class);
			intente.putExtra("triggered",true);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intente, PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.cancel(pendingIntent);
			return;
		}
		if (intent.hasExtra("triggered")){
			if (pref.getBoolean("nightmode",false)) {
				SimpleDateFormat sdf = new SimpleDateFormat("HHmm");
				int time = Integer.valueOf(sdf.format(new Date()));
			if (time < NIGHTMODE_STOP || time > NIGHTMODE_START) {
				KeyguardManager mKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
				if( mKM.inKeyguardRestrictedInputMode() ) {
					return;
				} else {
					PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
					if (!powerManager.isScreenOn()){ return; }
				}
			}
			}
		} else {
		if (intent.getAction().equals("android.net.conn.CONNECTIVITY_CHANGE")||intent.getAction().equals("hu.kfg.wifimanager.LOGGED_IN")){
			if (System.currentTimeMillis()-pref.getLong("last_check",0)<(Long.valueOf(pref.getString("auto_check_interval","300"))*60000)){
				return;
			}
			Log.w(TAG,""+(System.currentTimeMillis()+"//"+pref.getLong("last_check",0)));
		}
		if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)&&!pref.getBoolean("nightmode",false)) {
			return;
		} else if (!intent.getAction().equals(Intent.ACTION_USER_PRESENT)&&pref.getBoolean("nightmode",false)) {
			SimpleDateFormat sdf = new SimpleDateFormat("HHmm",Locale.US);
			int time = Integer.valueOf(sdf.format(new Date()));
			if (time < NIGHTMODE_STOP || time > NIGHTMODE_START) {
				KeyguardManager mKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
				if( mKM.inKeyguardRestrictedInputMode() ) {
					return;
				} else {
					PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
					if (!powerManager.isScreenOn()){ return; }
				}
			}
			if (!intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")&&System.currentTimeMillis()-pref.getLong("last_check",0)<(Long.valueOf(pref.getString("auto_check_interval","180"))*60000)){
				return;
			}
		} else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)&&pref.getBoolean("nightmode",false)) {
			if (System.currentTimeMillis()-pref.getLong("last_check",0)<(Long.valueOf(pref.getString("auto_check_interval","180"))*60000/2)){
				return;
			}
			SimpleDateFormat sdf = new SimpleDateFormat("HHmm",Locale.US);
			int time = Integer.valueOf(sdf.format(new Date()));
			if (!(time < NIGHTMODE_STOP || time > NIGHTMODE_START)) {
				return;
			}
		}
		if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")){
			AlarmManager alarmManager=(AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			Intent intente = new Intent(context, ChangeListener.class);
			intente.putExtra("triggered",true);
			PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intente, PendingIntent.FLAG_UPDATE_CURRENT);
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+5000,Long.valueOf(pref.getString("auto_check_interval","180"))*60000,pendingIntent);
			return;
		}
		}
		if (((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo()==null){
			if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")){
			showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, R.string.cannot_reach_site, Toast.LENGTH_SHORT).show();
					}
				});
				}
			return;
		}
		if (!((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo().isConnected()){
			return;
		}
		new Thread(new Runnable() {
			public void run(){
				doCheck(context,intent);
			}
			
		}).start();
		
		
	}
	
	
	
	public static int doCheck(final Context context,final Intent intent) {
		final SharedPreferences pref = PreferenceManager
			.getDefaultSharedPreferences(context);
		String kfgserver = pref.getString("url","1");
		if (kfgserver.length() < MainActivity.URL_MIN_LENGTH){
			if (intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, R.string.insert_code, Toast.LENGTH_SHORT).show();
					}
				});
			}
				return -1;
		}

		String version = "0.0";
		android.content.pm.PackageInfo pInfo = null;
		try {
			pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
			version = pInfo.versionName;
		} catch (Exception e){}

		HttpURLConnection urlConnection;
		try {
			URL url = new URL(kfgserver);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestProperty("User-Agent","Mozilla/5.0 (Linux; Android " + Build.VERSION.RELEASE + "; Karinthy Naplo v"+ version + ")");
			urlConnection.setInstanceFollowRedirects(true);
		} catch (IOException e) {
			Log.e(TAG,"Cannot load website!");
			if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, R.string.cannot_reach_site, Toast.LENGTH_SHORT).show();
					}
				});
			}
			e.printStackTrace();
			return -1;
		} catch (Exception e) {
			Log.e(TAG,"Unknown error!");
			if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
					}
				});
			}
			e.printStackTrace();
			return -1;
		}

        StringBuilder sb = new StringBuilder();
		int counter = 0;
		int notesc = 0;
		boolean hasstarted = false;
		boolean hasended = false;
		byte notes[] = new byte[512];
		String subjects[] = new String[512];
		String descriptions[] = new String[512];
		try {
			if (urlConnection.getResponseCode()%300<100) {
				notifyIfChanged(new int[]{1,0,0}, context, "https://naplo.karinthy.hu/", context.getString(R.string.gyia_expired_not));
				Log.w(TAG,urlConnection.getResponseCode() + "/" + urlConnection.getContentLength());
				if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
					showSuccessToast.postAtFrontOfQueue(new Runnable() {
						public void run() {
							Toast.makeText(context, R.string.gyia_expired_or_faulty, Toast.LENGTH_SHORT).show();
						}
					});
				}
				return -1;
			}
			BufferedReader rd = new BufferedReader
					(new InputStreamReader(urlConnection.getInputStream(), "ISO-8859-2"));
			String line;
			notesc = 0;
			counter = 0;
			List<Grade> mygrades = new ArrayList<>();
			Grade grade = new Grade((byte)0);
			while ((line = rd.readLine()) != null) {
				if (!hasstarted&&line.contains("<div data-role=\"content\" style=\"padding: 0px;\">")) hasstarted = true;
				if (line.contains("</article>")&&!hasended) {hasended = true; sb.append(line); break;}
				if (line.contains("<div class=\"creditbox\"")) {
					counter=0;
				}
				if ((counter==3||counter==2)&&(line.contains("<div class=\"credit ini_fresh\">")||line.contains("<div class=\"credit ini_credit\">"))) {
					grade = new Grade(notes[notesc] = (byte)Character.getNumericValue(line.charAt(line.indexOf(">")+1)));
					notesc++;
				}
				if ((counter==4||counter==3)&&(line.contains("<div class=\"teacher\">"))) {
					int i = line.indexOf(">");
					grade.addTeacher(line.substring(i+1,line.indexOf("<",i)));
				}
				if ((counter==6||counter==7)&&(line.contains("<span id=\"stamp_correct_"))) {
					int i = line.indexOf(">");
					grade.addDate(line.substring(i+1,line.indexOf("<",i)));
				}
				if ((counter==8||counter==9)&&(line.contains("<div class=\"description\">"))) {
					int i = line.indexOf(">");
					String desc;
					descriptions[notesc-1] = ((desc = line.substring(i+1,line.indexOf("<",i))).length()>21?desc.substring(0,20)+"…":desc);
					grade.addDescription(desc);
				}
				if ((counter==11||counter==10)&&(line.contains("<div class=\"creditbox_footer\">"))) {
					int i = line.indexOf(">");
					grade.addSubject(subjects[notesc-1] = line.substring(i+1,line.indexOf("<",i)));
					mygrades.add(grade);
					//Log.i("Grades",grade.toString());
				}
				counter++;
				sb.append(line);
			}
			if (intent.hasExtra("dbupgrade")) {
				Log.i("Grades","Size: " + mygrades.size());
				if (mygrades.size()<1) return 4;
				if (new DBHelper(context).upgradeDatabase(mygrades)==true) return 3;
				else return 5;
			}
		} catch (Exception e) {
			Log.e(TAG,"Unknown error!");
			if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
					}
				});
			}
			e.printStackTrace();
			return -1;
		}
		try {
			if (sb.toString().length() < 1000) {
				Log.w(TAG, sb.toString());
				throw new Exception("Content too small \nLength: " + sb.toString().length());
			}
			if (subjects[0] == null || subjects[0].length() < 2)
				throw new IndexOutOfBoundsException("Content too small \nLength: " + sb.toString());
		} catch (IndexOutOfBoundsException e) {
			Log.e(TAG, e.getMessage());
			Log.w(TAG, ""+(subjects[0]==null));
			if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, context.getString(R.string.error_no_grades), Toast.LENGTH_SHORT).show();
					}
				});
			}
			return -1;
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
			if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
					}
				});
			}
			return -1;
		}
		String[] s = new String[notesc];
		for (int i = 0;i<notesc;i++) {
			s[i] = subjects[i] + ": " + notes[i] + (descriptions[i].length()<1?"":"  ("+ descriptions[i] +")");
		}/*
		final int[] notesf = notes;
		showSuccessToast.postAtFrontOfQueue(new Runnable() {
			public void run() {
				Toast.makeText(context,"Nincs új jegyed! "+notesf[0], Toast.LENGTH_SHORT).show();
			}
		});*/
		if (running) {Log.w(TAG,"A process is already running"); return -1;}
		running = true;

		final int numofnotes = pref.getInt("numberofnotes",0);
		final int numofnotes0 = notesc;
		try {

			pref.edit().putInt("numberofnotes",notesc).commit();
            pref.edit().putLong("last_check",System.currentTimeMillis()).commit();
			if (numofnotes0-numofnotes>0) {
				int i = numofnotes0-numofnotes;
				String text ="";
				for (int i2 = 0; i2<i;i2++) {
					text+=s[i2] +", \n";
				}
				text = text.substring(0,text.length()-2);
				notifyIfChanged(new int[]{0,pref.getBoolean("vibrate",false)?1:0,pref.getBoolean("flash",false)?1:0},context,kfgserver,text);
				pref.edit().putString("lastSHA",SHA512(notes)).commit();
				running = false;
			} else {
				if (!SHA512(notes).equals(pref.getString("lastSHA","ABCD"))) {
					pref.edit().putString("lastSHA",SHA512(notes)).commit();
					notifyIfChanged(new int[]{0,pref.getBoolean("vibrate",false)?1:0,pref.getBoolean("flash",false)?1:0},context,kfgserver, context.getString(R.string.unknown_change));
					running = false;
					return 0;
				} else
				if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
					showSuccessToast.postAtFrontOfQueue(new Runnable() {
						public void run() {
							Toast.makeText(context, context.getString(R.string.no_new_grade) + " "+numofnotes0+"/"+numofnotes, Toast.LENGTH_SHORT).show();
						}
					});
				}
				running = false;
				return -1;
			}
		} catch (Exception e) {
			Log.e(TAG,"Unknown error!");
			if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, context.getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
					}
				});
			}
			e.printStackTrace();
			running = false;
			return -1;
		}
		running = false;
		return 0;
	}
	
	public static void notifyIfChanged(int[] state,Context context,String url, String subjects){
		Intent intent = new Intent(context, MainActivity.class);
		Intent eintent = new Intent(Intent.ACTION_VIEW);
		eintent.setData(Uri.parse(url));

		final SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String oldtext = oldtext = null;
		NotificationManager notificationManager =
				(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		if (state[0]==0) {
			notificationManager.cancel(1);
			if (isNotificationVisible(context)) {
				oldtext = pref.getString("oldtext",null);
			}
		}
		//PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
		PendingIntent epIntent = PendingIntent.getActivity(context, 0, eintent, 0);
		Notification.Builder n  = new Notification.Builder(context)
			.setContentTitle(context.getString(R.string.app_name))
			.setContentText(state[0]==0?context.getString(R.string.new_grade):context.getString(R.string.gyia_expired_not))
			.setAutoCancel(true);
		if (Build.VERSION.SDK_INT >= 21) {
			n.setSmallIcon(R.drawable.number_blocks);
		} else {
			n.setSmallIcon(R.drawable.number_blocks_mod);
		}
			//.setContentIntent(pIntent)
			if (state[1]==1){
				n.setVibrate(new long[]{0,60,100,70,100,60});
			}
			if (state[2]==1){
				n.setLights(0xff00FF88,350,3000);
			}
			if (Build.VERSION.SDK_INT>=21){
				n.setVisibility(Notification.VISIBILITY_PUBLIC);
			}
		n.addAction(android.R.drawable.ic_menu_view, context.getString(R.string.open), epIntent);
		Notification notification = new Notification.BigTextStyle(n)
            .bigText(((state[0]==0?context.getString(R.string.new_grade)+"\n":"") + subjects + (oldtext==null?"":"\n"+oldtext))).build();
//				notification.number = numberoflessons;
		notificationManager.notify(state[0], notification);
		pref.edit().putString("oldtext",subjects.length()>100? subjects.substring(0,subjects.indexOf(",",90))+"…":subjects).commit();
	}

	private static boolean isNotificationVisible(Context context) {
		if (Build.VERSION.SDK_INT < 23) return false;
		StatusBarNotification[] notifications = ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).getActiveNotifications();
		if (notifications.length == 0) return false;
		for (StatusBarNotification s: notifications) {
			if (s.getId() == 0) return true;
		}
		return false;
	}

	public static String SHA512(byte[] data) throws Exception {
		if (data==null) throw new Exception();
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		md.update(data);
		StringBuffer sb = new StringBuffer();
		byte[] byteData = md.digest();
		for (int i = 0; i < byteData.length; i++) {
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}
	
}
