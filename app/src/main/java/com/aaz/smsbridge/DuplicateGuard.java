package com.aaz.smsbridge;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class DuplicateGuard {
  private static final String PREFS = "aaz_sms_bridge_delivered";
  private static final long RETENTION_MS = 7L * 24 * 60 * 60 * 1000;
  private static final Set<String> IN_FLIGHT = new HashSet<>();

  private DuplicateGuard() {}

  static synchronized boolean isDelivered(Context context,String smsId){
    SharedPreferences prefs=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    prune(prefs,System.currentTimeMillis());
    return prefs.contains(smsId);
  }

  static synchronized boolean begin(Context context, String smsId) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    prune(prefs, System.currentTimeMillis());
    if (prefs.contains(smsId) || IN_FLIGHT.contains(smsId)) return false;
    IN_FLIGHT.add(smsId);
    return true;
  }

  static synchronized void finish(Context context, String smsId, boolean delivered) {
    IN_FLIGHT.remove(smsId);
    if (delivered) {
      context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
          .putLong(smsId, System.currentTimeMillis()).apply();
    }
  }

  private static void prune(SharedPreferences prefs, long now) {
    SharedPreferences.Editor editor = null;
    for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
      Object value = entry.getValue();
      if (!(value instanceof Long) || now - (Long) value > RETENTION_MS) {
        if (editor == null) editor = prefs.edit();
        editor.remove(entry.getKey());
      }
    }
    if (editor != null) editor.apply();
  }
}
