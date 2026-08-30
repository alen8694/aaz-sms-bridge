package com.aaz.smsbridge;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SenderRules {
  static final class Rule {
    final String sender;
    final String action;
    final String value;
    Rule(String sender,String action,String value){ this.sender=sender; this.action=action; this.value=value; }
    String display(){ return sender+" — "+action.replace('_',' ')+" — "+value; }
  }
  private SenderRules() {}

  static boolean hasRulesForSender(String sender,String rawRules){
    for(Rule rule:parse(rawRules)) if(sameSender(sender,rule.sender)) return true;
    return false;
  }

  static List<Rule> parse(String rawRules){
    List<Rule> result=new ArrayList<>();
    if(rawRules==null) return result;
    for(String rawLine:rawRules.split("\\R")){
      String line=rawLine.trim();
      if(line.isEmpty()||line.startsWith("#")) continue;
      String[] parts=line.split("\\|",3);
      if(parts.length==3&&!parts[0].trim().isEmpty()&&!parts[1].trim().isEmpty()&&!parts[2].trim().isEmpty())
        result.add(new Rule(parts[0].trim(),parts[1].trim().toLowerCase(Locale.ROOT),parts[2].trim()));
    }
    return result;
  }

  static String serialize(List<Rule> rules){
    StringBuilder result=new StringBuilder();
    for(Rule rule:rules){
      if(result.length()>0) result.append('\n');
      result.append(rule.sender).append(" | ").append(rule.action).append(" | ").append(rule.value);
    }
    return result.toString();
  }

  static SmartFilter.Result apply(String sender,String message,String rawRules){
    String filtered=message==null?"":message;
    if(rawRules==null||rawRules.trim().isEmpty()) return new SmartFilter.Result(SmartFilter.Action.FORWARD,filtered);
    for(Rule rule:parse(rawRules)){
      if(!sameSender(sender,rule.sender)) continue;
      String action=rule.action;
      String value=rule.value;
      SmartFilter.Result result;
      switch(action){
        case "block": result=SmartFilter.apply(filtered,value,"","",""); break;
        case "remove_keyword": result=SmartFilter.apply(filtered,"",value,"",""); break;
        case "remove_sentence": result=SmartFilter.apply(filtered,"","",value,""); break;
        case "remove_line": result=SmartFilter.apply(filtered,"","","",value); break;
        case "remove_range": result=removeRange(filtered,value); break;
        default: continue;
      }
      if(result.action==SmartFilter.Action.BLOCK) return result;
      filtered=result.message;
    }
    return filtered.trim().isEmpty()?new SmartFilter.Result(SmartFilter.Action.BLOCK,""):new SmartFilter.Result(SmartFilter.Action.FORWARD,filtered.trim());
  }

  private static SmartFilter.Result removeRange(String message,String value){
    String[] markers=value.split("=>",2);
    if(markers.length!=2||markers[0].trim().isEmpty()||markers[1].trim().isEmpty())
      return new SmartFilter.Result(SmartFilter.Action.FORWARD,message);
    Pattern startPattern=Pattern.compile(Pattern.quote(markers[0].trim()),Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    Pattern endPattern=Pattern.compile(Pattern.quote(markers[1].trim()),Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE);
    Matcher start=startPattern.matcher(message);
    if(!start.find()) return new SmartFilter.Result(SmartFilter.Action.FORWARD,message);
    Matcher end=endPattern.matcher(message);
    if(!end.find(start.end())) return new SmartFilter.Result(SmartFilter.Action.FORWARD,message);
    String result=(message.substring(0,start.start())+message.substring(end.start())).replaceAll("[ \\t]{2,}"," ").trim();
    return result.isEmpty()?new SmartFilter.Result(SmartFilter.Action.BLOCK,""):new SmartFilter.Result(SmartFilter.Action.FORWARD,result);
  }

  private static boolean sameSender(String actual,String configured){
    return normalize(actual).equals(normalize(configured));
  }
  private static String normalize(String value){
    return value==null?"":value.replaceAll("[^A-Za-z0-9+]","").toLowerCase(Locale.ROOT);
  }
}
