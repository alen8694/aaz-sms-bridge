package com.aaz.smsbridge;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class SmartFilter {
  enum Action { FORWARD, BLOCK }
  static final class Result {
    final Action action;
    final String message;
    Result(Action action,String message){ this.action=action; this.message=message; }
  }
  private SmartFilter() {}

  static Result apply(String message,String blockMessage,String removeKeywords,String removeSentences,String removeLines){
    String original=message==null?"":message;
    if(matchesAny(original,patterns(keywords(blockMessage)))) return new Result(Action.BLOCK,"");
    List<Pattern> lineRules=patterns(keywords(removeLines));
    List<String> keptLines=new ArrayList<>();
    for(String line:original.split("\\R",-1)) if(!matchesAny(line,lineRules)) keptLines.add(line);
    String filtered=String.join("\n",keptLines);
    List<Pattern> sentenceRules=patterns(keywords(removeSentences));
    if(!sentenceRules.isEmpty()){
      List<String> keptSentences=new ArrayList<>();
      for(String sentence:filtered.split("(?<=[.!?])\\s+",-1)) if(!matchesAny(sentence,sentenceRules)) keptSentences.add(sentence);
      filtered=String.join(" ",keptSentences);
    }
    for(Pattern pattern:patterns(keywords(removeKeywords))) filtered=pattern.matcher(filtered).replaceAll("");
    filtered=filtered.replaceAll("[ \\t]+(?=\\r?$)","").replaceAll("(?m)^[ \\t]+","").trim();
    return filtered.isEmpty()?new Result(Action.BLOCK,""):new Result(Action.FORWARD,filtered);
  }

  private static List<String> keywords(String raw){
    List<String> result=new ArrayList<>();
    if(raw==null) return result;
    for(String item:raw.split("[,\\n]")){ String value=item.trim(); if(!value.isEmpty()) result.add(value); }
    return result;
  }
  private static List<Pattern> patterns(List<String> keywords){
    List<Pattern> result=new ArrayList<>();
    for(String keyword:keywords) result.add(Pattern.compile("(?iu)(?<![\\p{L}\\p{N}])"+Pattern.quote(keyword)+"(?![\\p{L}\\p{N}])"));
    return result;
  }
  private static boolean matchesAny(String value,List<Pattern> patterns){
    for(Pattern pattern:patterns) if(pattern.matcher(value).find()) return true;
    return false;
  }
}
