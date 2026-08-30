package com.aaz.smsbridge;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.io.IOException;

public final class SmsForwardWorker extends Worker {
  static final String KEY_SMS_ID="sms_id";
  private static final int MAX_ATTEMPTS=10;
  public SmsForwardWorker(@NonNull Context context,@NonNull WorkerParameters params){ super(context,params); }

  @NonNull @Override public Result doWork(){
    Context context=getApplicationContext();
    String id=getInputData().getString(KEY_SMS_ID);
    if(id==null||id.isEmpty()) return Result.failure();
    PendingSmsStore store=new PendingSmsStore(context);
    PendingSmsStore.PendingSms sms;
    try { sms=store.get(id); }
    catch(Exception e){
      store.delete(id);
      DeliveryLog.add(context,"Unknown","Failed: encrypted queue unreadable");
      return Result.failure();
    }
    if(sms==null) return Result.success();
    if(!Prefs.enabled(context)){ store.delete(id); DeliveryLog.add(context,sms.sender,"Cancelled: forwarding disabled"); return Result.success(); }
    try {
      boolean keywordMatched=KeywordForwardRules.matches(sms.body,Prefs.keywordForwardRules(context));
      BridgeClient.SenderSyncResult sync=BridgeClient.syncSender(context,sms.sender);
      if(!sync.routed&&!keywordMatched){ store.delete(id); DeliveryLog.add(context,sms.sender,"Sender/keyword not routed"); return Result.success(); }
      String outgoing=sms.body;
      if(Prefs.smartFilterEnabled(context)){
        boolean senderSpecific=SenderRules.hasRulesForSender(sms.sender,Prefs.senderRules(context));
        boolean keywordSpecific=KeywordRules.hasRulesForMessage(sms.body,Prefs.keywordFilterRules(context));
        SmartFilter.Result filtered=new SmartFilter.Result(SmartFilter.Action.FORWARD,outgoing);
        if(senderSpecific) filtered=SenderRules.apply(sms.sender,filtered.message,Prefs.senderRules(context));
        if(filtered.action!=SmartFilter.Action.BLOCK&&keywordSpecific)
          filtered=KeywordRules.apply(sms.body,filtered.message,Prefs.keywordFilterRules(context));
        if(!senderSpecific&&!keywordSpecific)
          filtered=SmartFilter.apply(outgoing,Prefs.blockMessageKeywords(context),Prefs.removeKeywords(context),Prefs.removeSentenceKeywords(context),Prefs.removeLineKeywords(context));
        if(filtered.action==SmartFilter.Action.BLOCK){ store.delete(id); DeliveryLog.add(context,sms.sender,"Blocked by local filter"); return Result.success(); }
        outgoing=filtered.message;
      }
      BridgeClient.sendInbox(context,sms.sender,outgoing,sms.receivedAt,id);
      store.delete(id);
      DuplicateGuard.finish(context,id,true);
      DeliveryLog.add(context,sms.sender,keywordMatched&&!sync.routed?"SMS forwarded by keyword":"SMS forwarded");
      return Result.success();
    } catch(Exception error){
      if(!retryable(error)||getRunAttemptCount()+1>=MAX_ATTEMPTS){
        store.delete(id);
        DuplicateGuard.finish(context,id,false);
        DeliveryLog.add(context,sms.sender,statusFor(error));
        return Result.failure();
      }
      DeliveryLog.add(context,sms.sender,"Waiting to retry (attempt "+(getRunAttemptCount()+1)+")");
      return Result.retry();
    }
  }

  private static boolean retryable(Exception error){
    if(error instanceof BridgeClient.HttpStatusException){
      int code=((BridgeClient.HttpStatusException)error).statusCode;
      return code==408||code==429||code>=500;
    }
    return error instanceof IOException&& !"Invalid server response".equals(error.getMessage());
  }
  private static String statusFor(Exception error){
    if(error instanceof BridgeClient.HttpStatusException){
      int code=((BridgeClient.HttpStatusException)error).statusCode;
      if(code==401||code==403) return "Failed: authentication";
      return "Failed: server HTTP "+code;
    }
    if("Invalid server response".equals(error.getMessage())) return "Failed: invalid server response";
    return "Failed after retries";
  }
}
