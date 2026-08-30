package com.aaz.smsbridge;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class HealthCheckWorker extends Worker {
  public HealthCheckWorker(@NonNull Context context,@NonNull WorkerParameters params){ super(context,params); }
  @NonNull @Override public Result doWork(){
    if(Prefs.healthCheckEnabled(getApplicationContext())) InactivityLog.checkNow(getApplicationContext());
    return Result.success();
  }
}
