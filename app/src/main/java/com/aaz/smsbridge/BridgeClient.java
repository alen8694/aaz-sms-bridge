package com.aaz.smsbridge;

import android.content.Context;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import android.os.Build;

public final class BridgeClient {
  static final class SenderSyncResult {
    final boolean routed;
    SenderSyncResult(boolean routed){ this.routed=routed; }
  }

  static final class HttpStatusException extends IOException {
    final int statusCode;
    HttpStatusException(int statusCode){ super("HTTP "+statusCode); this.statusCode=statusCode; }
  }

  public static SenderSyncResult syncSender(Context context,String sender) throws Exception {
    JSONObject payload=new JSONObject();
    payload.put("sender",sender);
    payload.put("device_id",Prefs.deviceId(context));
    String response=post(context,ApiUrls.senderSync(Prefs.baseApiUrl(context)),payload);
    JSONObject json;
    try { json=new JSONObject(response); }
    catch(Exception e){ throw new IOException("Invalid server response",e); }
    if(!json.optBoolean("success",false) || !json.has("routed") || !(json.opt("routed") instanceof Boolean))
      throw new IOException("Invalid server response");
    return new SenderSyncResult(json.optBoolean("routed",false));
  }

  public static void sendInbox(Context context,String sender,String body,long receivedAt,String smsId) throws Exception {
    JSONObject payload=new JSONObject();
    payload.put("sender",sender);
    payload.put("message",body);
    payload.put("received_at",receivedAt);
    payload.put("device_id",Prefs.deviceId(context));
    payload.put("sms_id",smsId);
    post(context,ApiUrls.inbox(Prefs.baseApiUrl(context)),payload);
  }

  public static void sendHeartbeat(Context context) throws Exception {
    JSONObject payload=new JSONObject();
    payload.put("device_id",Prefs.deviceId(context));
    payload.put("app_version",context.getPackageManager().getPackageInfo(context.getPackageName(),0).versionName);
    payload.put("phone_model",Build.MANUFACTURER+" "+Build.MODEL);
    payload.put("android_version",Build.VERSION.RELEASE);
    payload.put("forwarding_enabled",Prefs.enabled(context));
    payload.put("always_active",Prefs.alwaysActiveEnabled(context));
    payload.put("sent_at",System.currentTimeMillis());
    post(context,ApiUrls.heartbeat(Prefs.baseApiUrl(context)),payload);
  }

  private static String post(Context context,String endpoint,JSONObject payload) throws Exception {
    String secret=Prefs.secret(context);
    if(secret.isEmpty()) throw new IllegalStateException("Missing bridge secret");
    URL url=new URL(endpoint);
    if(!"https".equalsIgnoreCase(url.getProtocol())) throw new IllegalArgumentException("Endpoint must use HTTPS");
    HttpURLConnection connection=(HttpURLConnection)url.openConnection();
    try {
      connection.setRequestMethod("POST");
      connection.setConnectTimeout(15000);
      connection.setReadTimeout(20000);
      connection.setDoOutput(true);
      connection.setRequestProperty("Content-Type","application/json; charset=utf-8");
      connection.setRequestProperty("X-AAZ-Bridge-Key",secret);
      byte[] output=payload.toString().getBytes(StandardCharsets.UTF_8);
      try(OutputStream stream=connection.getOutputStream()){ stream.write(output); }
      int code=connection.getResponseCode();
      InputStream input=code>=200&&code<300?connection.getInputStream():connection.getErrorStream();
      String response=read(input);
      if(code<200||code>=300) throw new HttpStatusException(code);
      return response;
    } finally { connection.disconnect(); }
  }

  private static String read(InputStream input) throws IOException {
    if(input==null) return "";
    try(BufferedReader reader=new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8))){
      String line;
      StringBuilder result=new StringBuilder();
      while((line=reader.readLine())!=null) result.append(line);
      return result.toString();
    }
  }

  static String smsId(String sender,String body,long receivedAt) throws Exception { return sha256(sender+"|"+receivedAt+"|"+body); }
  private static String sha256(String value) throws Exception {
    MessageDigest digest=MessageDigest.getInstance("SHA-256");
    byte[] bytes=digest.digest(value.getBytes(StandardCharsets.UTF_8));
    StringBuilder result=new StringBuilder();
    for(byte item:bytes) result.append(String.format("%02x",item));
    return result.toString();
  }
}
