package com.aaz.smsbridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {
  @Override public void onReceive(Context context,Intent intent){
    if(!Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())||!Prefs.enabled(context)) return;
    SmsMessage[] messages=Telephony.Sms.Intents.getMessagesFromIntent(intent);
    if(messages==null||messages.length==0) return;
    String sender=messages[0].getDisplayOriginatingAddress();
    if(sender==null||sender.trim().isEmpty()) return;
    StringBuilder body=new StringBuilder();
    for(SmsMessage message:messages) body.append(message.getDisplayMessageBody());
    long receivedAt=messages[0].getTimestampMillis();
    Context app=context.getApplicationContext();
    try {
      String smsId=BridgeClient.smsId(sender,body.toString(),receivedAt);
      if(DuplicateGuard.isDelivered(app,smsId)) return;
      Prefs.recordSender(app,sender);
      PendingSmsStore store=new PendingSmsStore(app);
      if(store.put(smsId,sender,body.toString(),receivedAt)) DeliveryLog.add(app,sender,"Queued for delivery");
      SmsWork.enqueue(app,smsId);
    } catch(Exception e){
      Log.e("AAZSmsBridge","Could not securely queue incoming SMS");
      DeliveryLog.add(app,sender,"Failed: could not queue securely");
    }
  }
}
