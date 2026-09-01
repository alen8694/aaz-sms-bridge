package com.aaz.smsbridge;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

/** Recovers inbox messages that arrived while the process or receiver was unavailable. */
final class MissedSmsScanner {
  private static final Uri INBOX=Uri.parse("content://sms/inbox");
  private static final long OVERLAP_MS=60_000L;
  private static final long MAX_LOOKBACK_MS=7L*24*60*60*1000;

  private MissedSmsScanner(){}

  static void scan(Context context){
    Context app=context.getApplicationContext();
    if(!Prefs.enabled(app)||app.checkSelfPermission(Manifest.permission.READ_SMS)!=PackageManager.PERMISSION_GRANTED) return;
    long cursorTime=Prefs.lastInboxScan(app);
    long now=System.currentTimeMillis();
    long from=Math.max(now-MAX_LOOKBACK_MS,Math.max(0,cursorTime-OVERLAP_MS));
    long completedThrough=cursorTime;
    try(Cursor cursor=app.getContentResolver().query(INBOX,new String[]{"address","body","date"},"date > ?",new String[]{Long.toString(from)},"date ASC")){
      if(cursor==null) return;
      while(cursor.moveToNext()){
        String sender=cursor.getString(0);
        String body=cursor.getString(1);
        long receivedAt=cursor.getLong(2);
        if(sender==null||sender.trim().isEmpty()||body==null) continue;
        if(!SmsReceiver.queueOne(app,sender,body,receivedAt,true)) break;
        completedThrough=Math.max(completedThrough,receivedAt);
      }
      // Advancing to now avoids repeatedly querying an empty time range. The one-minute overlap
      // plus deterministic sms_id prevents both late-provider rows and duplicate forwarding.
      Prefs.setLastInboxScan(app,Math.max(completedThrough,now));
    } catch(SecurityException error){
      Log.w("AAZSmsBridge","READ_SMS permission unavailable for catch-up",error);
    } catch(Exception error){
      Log.e("AAZSmsBridge","Missed SMS catch-up failed",error);
      DeliveryLog.add(app,"System","Missed SMS catch-up will retry");
    }
  }
}
