package com.aaz.smsbridge;

import android.content.Context;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

final class HealthMonitor {
  private static final String WORK_NAME="aaz-health-check";
  private HealthMonitor(){}
  static void update(Context context){
    WorkManager manager=WorkManager.getInstance(context);
    if(!Prefs.healthCheckEnabled(context)){
      manager.cancelUniqueWork(WORK_NAME);
      InactivityLog.resetBaseline(context);
      return;
    }
    InactivityLog.checkNow(context);
    PeriodicWorkRequest health=new PeriodicWorkRequest.Builder(HealthCheckWorker.class,15,TimeUnit.MINUTES).build();
    manager.enqueueUniquePeriodicWork(WORK_NAME,ExistingPeriodicWorkPolicy.KEEP,health);
  }
}
