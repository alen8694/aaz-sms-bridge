package com.aaz.smsbridge;

import static org.junit.Assert.*;
import org.junit.Test;

public class KeywordRulesTest {
  @Test public void appliesRuleOnlyWhenMatchKeywordIsPresent(){
    String rules="Pathao | remove_sentence | Balance";
    assertEquals("Pathao payment received. TrxID ABC.",KeywordRules.apply("Pathao payment received. Balance Tk 50. TrxID ABC.","Pathao payment received. Balance Tk 50. TrxID ABC.",rules).message);
    assertEquals("Other payment. Balance Tk 50.",KeywordRules.apply("Other payment. Balance Tk 50.","Other payment. Balance Tk 50.",rules).message);
  }

  @Test public void supportsMultipleRulesAndCaseInsensitiveMatch(){
    String rules="pathao | remove_keyword | secret\nPATHAO | remove_line | Balance";
    SmartFilter.Result result=KeywordRules.apply("[PATHAO] secret\nBalance Tk 50\nTrxID ABC","[PATHAO] secret\nBalance Tk 50\nTrxID ABC",rules);
    assertEquals(SmartFilter.Action.FORWARD,result.action);
    assertEquals("[PATHAO]\nTrxID ABC",result.message);
    assertTrue(KeywordRules.hasRulesForMessage("Pathao update",rules));
  }
}
