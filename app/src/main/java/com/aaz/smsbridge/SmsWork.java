package com.aaz.smsbridge;

import android.content.Context;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

final class SmsWork {
  private SmsWork(){}
  static androidx.work.Operation enqueue(Context context,String smsId){
    Constraints constraints=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    Data input=new Data.Builder().putString(SmsForwardWorker.KEY_SMS_ID,smsId).build();
    OneTimeWorkRequest request=new OneTimeWorkRequest.Builder(SmsForwardWorker.class).setInputData(input)
        .setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,10,TimeUnit.SECONDS)
        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build();
    return WorkManager.getInstance(context).enqueueUniqueWork("aaz-sms-"+smsId,ExistingWorkPolicy.KEEP,request);
  }
}
