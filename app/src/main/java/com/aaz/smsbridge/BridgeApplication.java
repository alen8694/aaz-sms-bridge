package com.aaz.smsbridge;

import android.app.Application;
public final class BridgeApplication extends Application {
  @Override public void onCreate(){
    super.onCreate();
    HealthMonitor.update(this);
  }
}
