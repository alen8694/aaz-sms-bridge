package com.aaz.smsbridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class KeywordRules {
  static final class Rule {
    final String matchKeyword;
    final String action;
    final String value;
    Rule(String matchKeyword,String action,String value){ this.matchKeyword=matchKeyword; this.action=action; this.value=value; }
    String display(){ return matchKeyword+" — "+action.replace('_',' ')+" — "+value; }
  }
  private KeywordRules() {}

  static boolean hasRulesForMessage(String message,String rawRules){
    for(Rule rule:parse(rawRules)) if(contains(message,rule.matchKeyword)) return true;
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
      result.append(rule.matchKeyword).append(" | ").append(rule.action).append(" | ").append(rule.value);
    }
    return result.toString();
  }

  static SmartFilter.Result apply(String originalMessage,String message,String rawRules){
    String filtered=message==null?"":message;
    for(Rule rule:parse(rawRules)){
      if(!contains(originalMessage,rule.matchKeyword)) continue;
      SmartFilter.Result result;
      switch(rule.action){
        case "block": result=SmartFilter.apply(filtered,rule.value,"","",""); break;
        case "remove_keyword": result=SmartFilter.apply(filtered,"",rule.value,"",""); break;
        case "remove_sentence": result=SmartFilter.apply(filtered,"","",rule.value,""); break;
        case "remove_line": result=SmartFilter.apply(filtered,"","","",rule.value); break;
        case "remove_range": result=SenderRules.removeRangeForRule(filtered,rule.value); break;
        default: continue;
      }
      if(result.action==SmartFilter.Action.BLOCK) return result;
      filtered=result.message;
    }
    return filtered.trim().isEmpty()?new SmartFilter.Result(SmartFilter.Action.BLOCK,""):new SmartFilter.Result(SmartFilter.Action.FORWARD,filtered.trim());
  }

  private static boolean contains(String message,String keyword){
    return message!=null&&keyword!=null&&!keyword.trim().isEmpty()&&message.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
  }
}
