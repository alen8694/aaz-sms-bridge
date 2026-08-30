package com.aaz.smsbridge;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.provider.Settings;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class InactivityLog {
  private static final String PREFS="aaz_sms_health";
  private static final String ENTRIES="inactive_entries";
  private static final String LAST_CHECK="last_health_check";
  private static final String LAST_BOOT="last_boot_count";
  private static final int MAX=20;
  private static final long INACTIVE_AFTER_MS=30*60*1000L;
  private InactivityLog(){}

  static synchronized void checkNow(Context context){
    long now=System.currentTimeMillis();
    SharedPreferences prefs=prefs(context);
    long previous=prefs.getLong(LAST_CHECK,0L);
    int boot=bootCount(context),previousBoot=prefs.getInt(LAST_BOOT,boot);
    if(previous>0&&now>previous&&now-previous>INACTIVE_AFTER_MS) add(context,previous,now,reason(context,boot!=previousBoot));
    prefs.edit().putLong(LAST_CHECK,now).putInt(LAST_BOOT,boot).apply();
  }

  static synchronized List<String> entries(Context context){
    List<String> result=new ArrayList<>();
    JSONArray array=readArray(context);
    DateFormat format=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
    for(int i=0;i<array.length();i++) try {
      JSONObject item=array.getJSONObject(i);
      long start=item.optLong("start"),end=item.optLong("end");
      result.add(format.format(new Date(start))+" → "+format.format(new Date(end))+" · approximately "+duration(end-start)+"\nPossible reason: "+item.optString("reason","Unknown"));
    } catch(Exception ignored){}
    return result;
  }

  static synchronized void clear(Context context){ prefs(context).edit().remove(ENTRIES).apply(); }
  static synchronized void resetBaseline(Context context){ prefs(context).edit().remove(LAST_CHECK).remove(LAST_BOOT).apply(); }

  private static void add(Context context,long start,long end,String reason){
    JSONArray old=readArray(context),updated=new JSONArray();
    try {
      updated.put(new JSONObject().put("start",start).put("end",end).put("reason",reason));
      for(int i=0;i<old.length()&&updated.length()<MAX;i++) updated.put(old.getJSONObject(i));
    } catch(Exception ignored){}
    prefs(context).edit().putString(ENTRIES,updated.toString()).apply();
  }

  private static String duration(long millis){
    long minutes=Math.max(1,millis/60000L);
    long hours=minutes/60,remaining=minutes%60;
    return hours==0?minutes+" min":hours+" hr "+remaining+" min";
  }
  private static int bootCount(Context context){
    try { return Settings.Global.getInt(context.getContentResolver(),Settings.Global.BOOT_COUNT); }
    catch(Exception ignored){ return -1; }
  }
  private static String reason(Context context,boolean rebooted){
    if(rebooted) return "Device restarted";
    PowerManager power=(PowerManager)context.getSystemService(Context.POWER_SERVICE);
    if(power!=null&&!power.isIgnoringBatteryOptimizations(context.getPackageName())) return "Battery optimization / Doze likely";
    return "Android/OEM background restriction or app process suspension likely";
  }
  private static SharedPreferences prefs(Context context){ return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE); }
  private static JSONArray readArray(Context context){
    try { return new JSONArray(prefs(context).getString(ENTRIES,"[]")); }
    catch(Exception ignored){ return new JSONArray(); }
  }
}
