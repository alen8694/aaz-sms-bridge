package com.aaz.smsbridge;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class KeywordForwardRules {
  private KeywordForwardRules() {}

  static List<String> parse(String raw){
    Set<String> unique=new LinkedHashSet<>();
    if(raw!=null) for(String value:raw.split("[,\\r\\n]+")){
      String keyword=value.trim();
      if(!keyword.isEmpty()) unique.add(keyword);
    }
    return new ArrayList<>(unique);
  }

  static boolean matches(String message,String raw){
    String haystack=message==null?"":message.toLowerCase(Locale.ROOT);
    for(String keyword:parse(raw))
      if(haystack.contains(keyword.toLowerCase(Locale.ROOT))) return true;
    return false;
  }
}
