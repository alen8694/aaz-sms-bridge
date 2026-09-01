package com.aaz.smsbridge;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

final class BridgeServiceController {
  private BridgeServiceController(){}
  static void update(Context context){
    Context app=context.getApplicationContext();
    Intent service=new Intent(app,BridgeForegroundService.class);
    if(!Prefs.enabled(app)||!Prefs.alwaysActiveEnabled(app)){
      app.stopService(service);
      return;
    }
    try {
      if(Build.VERSION.SDK_INT>=26) app.startForegroundService(service); else app.startService(service);
    } catch(Exception error){
      Log.w("AAZSmsBridge","Always-active service could not start",error);
      DeliveryLog.add(app,"System","Always-active service start delayed by Android");
    }
  }
}
