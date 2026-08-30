package com.aaz.smsbridge;

import static org.junit.Assert.*;
import org.junit.Test;

public class SmartFilterTest {
  @Test public void blocksEntireMessageCaseInsensitively(){
    assertEquals(SmartFilter.Action.BLOCK,SmartFilter.apply("Your OTP is 123456","OTP, PIN, password","","","").action);
  }
  @Test public void avoidsPartialWordFalsePositive(){
    assertEquals(SmartFilter.Action.FORWARD,SmartFilter.apply("Shopping total is 50","PIN","","","").action);
  }
  @Test public void removesKeywordOnly(){
    SmartFilter.Result result=SmartFilter.apply("Secret token ABC approved","","secret, token","","");
    assertEquals(SmartFilter.Action.FORWARD,result.action);
    assertEquals("ABC approved",result.message);
  }
  @Test public void removesMatchingLine(){
    SmartFilter.Result result=SmartFilter.apply("Your OTP is 123456\nTotal Amount: 5000\nPaid successfully","","","","OTP");
    assertEquals("Total Amount: 5000\nPaid successfully",result.message);
  }
  @Test public void blocksWhenNothingRemains(){
    assertEquals(SmartFilter.Action.BLOCK,SmartFilter.apply("Password: abc","","","","password").action);
  }

  @Test public void removesBalanceSentenceWithoutBreakingDecimalAmounts(){
    String message="You have received payment Tk 10.00 from 0178XXXX694. Fee Tk 0.00. Balance Tk 3,242.00. TrxID DHT8W42DDY at 29/08/2026 16:34";
    SmartFilter.Result result=SmartFilter.apply(message,"","","Balance","");
    assertEquals(SmartFilter.Action.FORWARD,result.action);
    assertEquals("You have received payment Tk 10.00 from 0178XXXX694. Fee Tk 0.00. TrxID DHT8W42DDY at 29/08/2026 16:34",result.message);
  }
}
