package com.aaz.smsbridge;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ApiUrlsTest {
  @Test public void migratesLegacyInboxUrl(){
    String old="https://oms.aazbd.com/wp-json/aaz-sms-bridge/v1/inbox";
    assertEquals("https://oms.aazbd.com/wp-json/aaz-sms-bridge/v1",ApiUrls.normalizeBase(old));
  }

  @Test public void derivesBothEndpoints(){
    String base="https://example.com/wp-json/aaz-sms-bridge/v1/";
    assertEquals("https://example.com/wp-json/aaz-sms-bridge/v1/sender-sync",ApiUrls.senderSync(base));
    assertEquals("https://example.com/wp-json/aaz-sms-bridge/v1/inbox",ApiUrls.inbox(base));
  }
}
