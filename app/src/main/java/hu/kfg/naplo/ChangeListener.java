package hu.kfg.naplo;

import android.content.*;
import android.preference.*;

import org.apache.http.*;
import org.apache.http.params.*;
import android.net.*;
import android.os.*;
import android.util.*;
import org.apache.http.client.*;
import org.apache.http.client.methods.*;
import org.apache.http.impl.client.*;

import java.security.MessageDigest;
import java.util.*;
import java.io.*;
import android.widget.*;
import android.app.*;

import java.text.*;

public class ChangeListener extends BroadcastReceiver
{
	
	private final Handler showSuccessToast = new Handler() {
		public void handleMessage(Message msg) {
			
		}
	};

	public static final int NIGHTMODE_START = 2230;
	public static final int NIGHTMODE_STOP = 530;

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
			if (System.currentTimeMillis()-pref.getLong("last_check",0)<(Long.valueOf(pref.getString("auto_check_interval","180"))*60000)){
				return;
			}
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
						Toast.makeText(context,"Nem érhető el a \"Napló\"oldal!", Toast.LENGTH_SHORT).show();
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
	
	
	
	public boolean doCheck(final Context context,final Intent intent) {
		final String TAG = "KFGnaplo-check";
		final SharedPreferences pref = PreferenceManager
			.getDefaultSharedPreferences(context);
		String url = pref.getString("url","1");
		if (url.length()<30){
			if (intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, "Másold be a linket!", Toast.LENGTH_SHORT).show();
					}
				});
			}
				return false;
		}
		String kfgserver = url;
		final HttpParams httpParams = new BasicHttpParams();
		HttpConnectionParams.setConnectionTimeout(httpParams, 7000);
		HttpConnectionParams.setSoTimeout(httpParams, 7000);

		HttpResponse response = null;
		HttpClient client = new DefaultHttpClient(httpParams);
		HttpUriRequest request = new HttpGet(kfgserver);
		HttpEntity entity = null;


		try {
			response = client.execute(request);
		} catch (IOException e3) {
			Log.e(TAG,"Cannot load website!");
			if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, "Nem érhető el a \"Napló\" oldala!", Toast.LENGTH_SHORT).show();
					}
				});
			}
			e3.printStackTrace();
			return false;
		}
		StringBuilder sb = new StringBuilder();
		int counter = 0;
		int notesc = 0;
		boolean hasstarted = false;
		boolean hasended = false;
		byte notes[] = new byte[384];
		String subjects[] = new String[384];
		String descriptions[] = new String[384];
		try {
			BufferedReader rd = new BufferedReader
					(new InputStreamReader(response.getEntity().getContent(), "ISO-8859-2"));
			String line;
			notesc = 0;
			counter = 0;
			while ((line = rd.readLine()) != null) {
				if (!hasstarted&&line.contains("<div data-role=\"content\" style=\"padding: 0px;\">")) hasstarted = true;
				if (line.contains("</article>")&&!hasended) {hasended = true; sb.append(line); break;}
				if (line.contains("<div class=\"creditbox\"")) {
					counter=0;
				}
				if ((counter==3||counter==2)&&(line.contains("<div class=\"credit ini_fresh\">")||line.contains("<div class=\"credit ini_credit\">"))) {
					notes[notesc] = (byte)Character.getNumericValue(line.charAt(line.indexOf(">")+1));
					notesc++;
				}
				if ((counter==8||counter==9)&&(line.contains("<div class=\"description\">"))) {
					int i = line.indexOf(">");
					String desc;
					descriptions[notesc-1] = ((desc = line.substring(i+1,line.indexOf("<",i))).length()>13?desc.substring(0,12)+"…":desc);
				}
				if ((counter==11||counter==10)&&(line.contains("<div class=\"creditbox_footer\">"))) {
					int i = line.indexOf(">");
					subjects[notesc-1] = line.substring(i+1,line.indexOf("<",i));
				}
				counter++;
				sb.append(line);
			}
		} catch (Exception e) {
			Log.e(TAG,"Unknown error!");
			if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, "Karinthy Napló hiba!", Toast.LENGTH_SHORT).show();
					}
				});
			}
			e.printStackTrace();
			return false;
		}
		try {
			if (sb.toString().length() < 1000)
				throw new Exception("Content too small \nLength: " + sb.toString().length());
			if (subjects[0]==null||subjects[0].length()<2) throw new Exception("Null or nonexistent content");
		} catch (Exception e) {
			Log.e(TAG,e.getMessage());
			e.printStackTrace();
			if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, "Karinthy Napló hiba!\n Próbáld meg ismét!", Toast.LENGTH_SHORT).show();
					}
				});
			}
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
		if (running) {Log.w(TAG,"A process is already running"); return false;}
		running = true;

		final int numofnotes = pref.getInt("numberofnotes",0);
		final int numofnotes0 = notesc;
		try {

			pref.edit().putInt("numberofnotes",notesc).commit();
			if (numofnotes0-numofnotes>0) {
				int i = numofnotes0-numofnotes;
				String text ="";
				for (int i2 = 0; i2<i;i2++) {
					text+=s[i2] +", \n";
				}
				text = text.substring(0,text.length()-2);
				notifyIfChanged(new int[]{0,pref.getBoolean("vibrate",false)?1:0,pref.getBoolean("flash",false)?1:0},context,url,text);
				pref.edit().putString("lastSHA",SHA512(notes)).commit();
				running = false;
			} else {
				if (!SHA512(notes).equals(pref.getString("lastSHA","ABCD"))) {
					pref.edit().putString("lastSHA",SHA512(notes)).commit();
					notifyIfChanged(new int[]{0,pref.getBoolean("vibrate",false)?1:0,pref.getBoolean("flash",false)?1:0},context,url,"Valami változás történt, talán beírtak egy hiányzást?");
					running = false;
					return true;
				} else
				if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
					showSuccessToast.postAtFrontOfQueue(new Runnable() {
						public void run() {
							Toast.makeText(context,"Nincs új jegyed! "+numofnotes0+"/"+numofnotes, Toast.LENGTH_SHORT).show();
						}
					});
				}
				running = false;
				return false;
			}
		} catch (Exception e) {
			Log.e(TAG,"Unknown error!");
			if (!intent.hasExtra("triggered")&&intent.getAction().equals("hu.kfg.naplo.CHECK_NOW")) {
				showSuccessToast.postAtFrontOfQueue(new Runnable() {
					public void run() {
						Toast.makeText(context, "Karinthy Napló hiba!", Toast.LENGTH_SHORT).show();
					}
				});
			}
			e.printStackTrace();
			running = false;
			return false;
		}
		running = false;
		return true;
	}
	
	public void notifyIfChanged(int[] state,Context context,String url, String subjects){
		Intent intent = new Intent(context, MainActivity.class);
		Intent eintent = new Intent(Intent.ACTION_VIEW);
		eintent.setData(Uri.parse(url));

		final SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String oldtext = oldtext = null;
		if (isNotificationVisible(context)) {
			oldtext = pref.getString("oldtext",null);
		}
		//PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent, 0);
		PendingIntent epIntent = PendingIntent.getActivity(context, 0, eintent, 0);
		Notification.Builder n  = new Notification.Builder(context)
			.setContentTitle("Karinthy Napló")
			.setContentText("Új jegyet kaptál!")
			.setSmallIcon(R.drawable.number_blocks_mod)
			//.setContentIntent(pIntent)
			.setAutoCancel(true);
			if (state[1]==1){
				n.setVibrate(new long[]{0,60,100,70,100,60});
			}
			if (state[2]==1){
				n.setLights(0xff00FF88,350,3000);
			}
			if (Build.VERSION.SDK_INT>=21){
				n.setVisibility(Notification.VISIBILITY_PUBLIC);
			}
		NotificationManager notificationManager = 
			(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		n.addAction(android.R.drawable.ic_menu_view,"Megnyitás", epIntent);
		Notification notification = new Notification.BigTextStyle(n)
            .bigText(("Új jegyet kaptál!\n" + subjects + (oldtext==null?"":oldtext))).build();
//				notification.number = numberoflessons;
		notificationManager.notify(0, notification);
		pref.edit().putString("oldtext",subjects.length()>100? subjects.substring(0,subjects.indexOf(",",90))+"…":subjects).commit();
	}

	private boolean isNotificationVisible(Context context) {
		Intent notificationIntent = new Intent(context, MainActivity.class);
		PendingIntent test = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_NO_CREATE);
		return test != null;
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
