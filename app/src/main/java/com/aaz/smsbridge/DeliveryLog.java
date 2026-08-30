package com.aaz.smsbridge;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

final class DeliveryLog {
  private static final String PREFS="aaz_sms_delivery_log";
  private static final String KEY="entries";
  private static final int MAX=20;
  private DeliveryLog(){}

  static synchronized void add(Context context,String sender,String status){
    JSONArray old=readArray(context);
    JSONArray updated=new JSONArray();
    JSONObject entry=new JSONObject();
    try {
      entry.put("time",System.currentTimeMillis());
      entry.put("sender",sender==null?"Unknown":sender);
      entry.put("status",status);
      updated.put(entry);
      for(int i=0;i<old.length()&&updated.length()<MAX;i++) updated.put(old.getJSONObject(i));
    } catch(Exception ignored){}
    prefs(context).edit().putString(KEY,updated.toString()).apply();
  }

  static synchronized List<String> entries(Context context){
    List<String> result=new ArrayList<>();
    JSONArray array=readArray(context);
    DateFormat format=DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT);
    for(int i=0;i<array.length();i++) try {
      JSONObject item=array.getJSONObject(i);
      result.add(format.format(new Date(item.optLong("time")))+" · "+item.optString("sender")+" · "+item.optString("status"));
    } catch(Exception ignored){}
    return result;
  }

  static synchronized void clear(Context context){ prefs(context).edit().remove(KEY).apply(); }
  private static SharedPreferences prefs(Context context){ return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE); }
  private static JSONArray readArray(Context context){
    try { return new JSONArray(prefs(context).getString(KEY,"[]")); }
    catch(Exception ignored){ return new JSONArray(); }
  }
}
