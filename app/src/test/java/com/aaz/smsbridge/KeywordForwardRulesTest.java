package com.aaz.smsbridge;

import static org.junit.Assert.*;
import org.junit.Test;

public class KeywordForwardRulesTest {
  @Test public void matchesAnyKeywordCaseInsensitively(){
    assertTrue(KeywordForwardRules.matches("Your Pathao parcel is ready","[Pathao], Pathao"));
    assertTrue(KeywordForwardRules.matches("[PATHAO] delivery update","[Pathao], Pathao"));
    assertFalse(KeywordForwardRules.matches("Other delivery update","[Pathao], Pathao"));
  }

  @Test public void emptyRulesDenyKeywordForwarding(){
    assertFalse(KeywordForwardRules.matches("Pathao delivery","  , \n"));
  }
}
