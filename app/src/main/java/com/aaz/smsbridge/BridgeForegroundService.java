package com.aaz.smsbridge;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class BridgeForegroundService extends Service {
  private static final String CHANNEL_ID="aaz_bridge_active";
  private static final int NOTIFICATION_ID=4201;
  private static final long HEARTBEAT_MINUTES=5;
  private ScheduledExecutorService scheduler;
  private boolean monitoringStarted;

  @Override public void onCreate(){
    super.onCreate();
    createChannel();
    scheduler=Executors.newSingleThreadScheduledExecutor(r->{
      Thread thread=new Thread(r,"aaz-bridge-monitor");
      thread.setDaemon(true);
      return thread;
    });
  }

  @Override public int onStartCommand(Intent intent,int flags,int startId){
    startForeground(NOTIFICATION_ID,notification());
    if(!Prefs.enabled(this)||!Prefs.alwaysActiveEnabled(this)){
      stopForeground(true);
      stopSelf();
      return START_NOT_STICKY;
    }
    startMonitoring();
    return START_STICKY;
  }

  private synchronized void startMonitoring(){
    if(monitoringStarted||scheduler==null||scheduler.isShutdown()) return;
    monitoringStarted=true;
    scheduler.scheduleWithFixedDelay(()->{
      if(!Prefs.enabled(this)) return;
      MissedSmsScanner.scan(this);
      try { BridgeClient.sendHeartbeat(this); }
      catch(Exception ignored){ /* The next heartbeat retries; SMS delivery remains independent. */ }
    },0,HEARTBEAT_MINUTES,TimeUnit.MINUTES);
  }

  @Override public void onDestroy(){
    if(scheduler!=null) scheduler.shutdownNow();
    super.onDestroy();
  }

  @Override public IBinder onBind(Intent intent){ return null; }

  private Notification notification(){
    Intent open=new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
    PendingIntent pending=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL_ID):new Notification.Builder(this);
    return builder.setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle("AAZ SMS Bridge active")
        .setContentText("Waiting for incoming SMS").setOngoing(true).setCategory(Notification.CATEGORY_SERVICE)
        .setContentIntent(pending).build();
  }

  private void createChannel(){
    if(Build.VERSION.SDK_INT<26) return;
    NotificationChannel channel=new NotificationChannel(CHANNEL_ID,"Bridge active",NotificationManager.IMPORTANCE_LOW);
    channel.setDescription("Keeps SMS forwarding ready in the background");
    channel.setShowBadge(false);
    ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
  }
}
