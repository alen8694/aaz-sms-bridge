package com.aaz.smsbridge;

import static org.junit.Assert.*;
import org.junit.Test;

public class BridgeClientTest {
  @Test public void smsIdIsStableAndSensitiveToInput() throws Exception {
    String first=BridgeClient.smsId("sender", "body", 123L);
    assertEquals(first, BridgeClient.smsId("sender", "body", 123L));
    assertNotEquals(first, BridgeClient.smsId("sender", "changed", 123L));
    assertEquals(64, first.length());
  }
}
