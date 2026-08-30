package com.aaz.smsbridge;

final class ApiUrls {
  private ApiUrls() {}

  static String normalizeBase(String value){
    String url=value==null?"":value.trim();
    while(url.endsWith("/")) url=url.substring(0,url.length()-1);
    if(url.endsWith("/inbox")) url=url.substring(0,url.length()-6);
    else if(url.endsWith("/sender-sync")) url=url.substring(0,url.length()-12);
    while(url.endsWith("/")) url=url.substring(0,url.length()-1);
    return url;
  }

  static String senderSync(String base){ return normalizeBase(base)+"/sender-sync"; }
  static String inbox(String base){ return normalizeBase(base)+"/inbox"; }
}
