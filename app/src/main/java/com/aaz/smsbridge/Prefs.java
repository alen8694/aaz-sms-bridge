package com.aaz.smsbridge;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.*;

public final class Prefs {
  private static SharedPreferences p(Context c){ return c.getSharedPreferences("aaz_sms_bridge", Context.MODE_PRIVATE); }
  public static String baseApiUrl(Context c){ return ApiUrls.normalizeBase(p(c).getString("endpoint", "https://oms.aazbd.com/wp-json/aaz-sms-bridge/v1")); }
  public static String secret(Context c){ return p(c).getString("secret", ""); }
  public static String deviceId(Context c){ return p(c).getString("device_id", "office-sim-01"); }
  public static boolean enabled(Context c){ return p(c).getBoolean("enabled", false); }
  public static boolean smartFilterEnabled(Context c){ return p(c).getBoolean("smart_filter_enabled", false); }
  public static String blockMessageKeywords(Context c){ return p(c).getString("block_message_keywords", "OTP, PIN, password"); }
  public static String removeKeywords(Context c){ return p(c).getString("remove_keywords", ""); }
  public static String removeSentenceKeywords(Context c){ return p(c).getString("remove_sentence_keywords", ""); }
  public static String removeLineKeywords(Context c){ return p(c).getString("remove_line_keywords", ""); }
  public static String senderRules(Context c){ return p(c).getString("sender_filter_rules", ""); }
  public static String keywordForwardRules(Context c){ return p(c).getString("keyword_forward_rules", ""); }
  public static void recordSender(Context c,String sender){
    if(sender==null||sender.trim().isEmpty()) return;
    Set<String> senders=new HashSet<>(p(c).getStringSet("known_senders",Collections.emptySet()));
    if(senders.add(sender.trim())) p(c).edit().putStringSet("known_senders",senders).apply();
  }
  public static List<String> knownSenders(Context c){
    List<String> senders=new ArrayList<>(p(c).getStringSet("known_senders",Collections.emptySet()));
    senders.sort(String.CASE_INSENSITIVE_ORDER);
    return senders;
  }
  public static void save(Context c,String baseUrl,String secret,String deviceId,boolean enabled,boolean filterEnabled,String block,String remove,String removeSentences,String removeLines,String senderRules,String keywordForwardRules){
    p(c).edit().putString("endpoint",ApiUrls.normalizeBase(baseUrl)).putString("secret",secret.trim()).remove("group_id")
      .remove("whitelist").putString("device_id",deviceId.trim()).putBoolean("enabled",enabled)
      .putBoolean("smart_filter_enabled",filterEnabled).putString("block_message_keywords",block.trim())
      .putString("remove_keywords",remove.trim()).putString("remove_sentence_keywords",removeSentences.trim())
      .putString("remove_line_keywords",removeLines.trim()).putString("sender_filter_rules",senderRules.trim())
      .putString("keyword_forward_rules",keywordForwardRules.trim()).apply();
  }
}
