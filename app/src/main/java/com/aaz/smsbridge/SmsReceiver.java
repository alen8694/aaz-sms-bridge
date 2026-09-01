package com.aaz.smsbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SmsReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context,Intent intent){
    if(!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())||!Prefs.enabled(context)) return;
    BridgeServiceController.update(context);
    SmsMessage[] messages=Telephony.Sms.Intents.getMessagesFromIntent(intent);
    if(messages==null||messages.length==0) return;
    PendingResult pendingResult=goAsync();
    Context app=context.getApplicationContext();
    ExecutorService executor=Executors.newSingleThreadExecutor();
    executor.execute(()->{
      try { queueMessages(app,messages); }
      finally { pendingResult.finish(); executor.shutdown(); }
    });
  }

  private static void queueMessages(Context app,SmsMessage[] messages){
    String sender=messages[0].getDisplayOriginatingAddress();
    if(sender==null||sender.trim().isEmpty()) return;
    StringBuilder body=new StringBuilder();
    for(SmsMessage message:messages) body.append(message.getDisplayMessageBody());
    long receivedAt=messages[0].getTimestampMillis();
    try {
      String smsId=BridgeClient.smsId(sender,body.toString(),receivedAt);
      if(DuplicateGuard.isDelivered(app,smsId)) return;
      Prefs.recordSender(app,sender);
      PendingSmsStore store=new PendingSmsStore(app);
      if(store.put(smsId,sender,body.toString(),receivedAt)) DeliveryLog.add(app,sender,"Queued for delivery");
      // Wait only for WorkManager to persist the request, never for network delivery.
      SmsWork.enqueue(app,smsId).getResult().get(8,TimeUnit.SECONDS);
    } catch(Exception e){
      Log.e("AAZSmsBridge","Could not securely queue incoming SMS",e);
      DeliveryLog.add(app,sender,"Failed: could not queue securely");
    }
  }
}
