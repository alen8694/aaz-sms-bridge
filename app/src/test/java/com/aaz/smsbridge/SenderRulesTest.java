package com.aaz.smsbridge;

import static org.junit.Assert.*;
import org.junit.Test;

public class SenderRulesTest {
  @Test public void appliesOnlyToExactNormalizedSender(){
    String rules="bKash | block | OTP";
    assertEquals(SmartFilter.Action.BLOCK,SenderRules.apply("BKASH","Your OTP is 1234",rules).action);
    assertEquals(SmartFilter.Action.FORWARD,SenderRules.apply("bKash Info","Your OTP is 1234",rules).action);
    assertTrue(SenderRules.hasRulesForSender("BKASH",rules));
    assertFalse(SenderRules.hasRulesForSender("bKash Info",rules));
  }

  @Test public void removesConfiguredBalanceRange(){
    String message="Tk150.00 received from A/C:***981 Fee:Tk0, Your A/C Balance: Tk7,535.52 TxnId:6889987071 Date:29-AUG-26";
    String rules="BankABC | remove_range | Your A/C Balance: => TxnId:";
    SmartFilter.Result result=SenderRules.apply("BankABC",message,rules);
    assertEquals(SmartFilter.Action.FORWARD,result.action);
    assertEquals("Tk150.00 received from A/C:***981 Fee:Tk0, TxnId:6889987071 Date:29-AUG-26",result.message);
  }

  @Test public void supportsDifferentModesPerSender(){
    String rules="bKash | remove_sentence | Balance\nNAGAD | remove_line | Balance";
    String single="Payment received. Balance Tk 50.00. TrxID ABC.";
    assertEquals("Payment received. TrxID ABC.",SenderRules.apply("bKash",single,rules).message);
    String lines="Payment received\nBalance Tk 50.00\nTrxID ABC";
    assertEquals("Payment received\nTrxID ABC",SenderRules.apply("NAGAD",lines,rules).message);
  }

  @Test public void supportsMultipleRulesForOneSenderAndRoundTrip(){
    java.util.List<SenderRules.Rule> rules=new java.util.ArrayList<>();
    rules.add(new SenderRules.Rule("bKash","block","OTP"));
    rules.add(new SenderRules.Rule("bKash","block","PIN"));
    rules.add(new SenderRules.Rule("bKash","remove_sentence","Balance"));
    String serialized=SenderRules.serialize(rules);
    assertEquals(3,SenderRules.parse(serialized).size());
    assertEquals(SmartFilter.Action.BLOCK,SenderRules.apply("bKash","Your PIN is 1234",serialized).action);
    assertEquals("Payment received. TrxID ABC.",SenderRules.apply("bKash","Payment received. Balance Tk 50.00. TrxID ABC.",serialized).message);
  }

  @Test public void removesLogicalBalanceFieldWhenSmsHasNoRealLineBreaks(){
    String message="Add Money from Bank is Successful. From: City Bank PLC Amount: Tk 50.0 TxnID: 75WOGURA Balance: Tk 37444.05 30/08/2026 21:39";
    SmartFilter.Result result=SenderRules.apply("NAGAD",message,"NAGAD | remove_line | Balance");
    assertEquals(SmartFilter.Action.FORWARD,result.action);
    assertEquals("Add Money from Bank is Successful. From: City Bank PLC Amount: Tk 50.0 TxnID: 75WOGURA 30/08/2026 21:39",result.message);
  }
}
