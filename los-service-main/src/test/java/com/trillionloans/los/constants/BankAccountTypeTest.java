package com.trillionloans.los.constants;

import static org.junit.jupiter.api.Assertions.*;

import com.trillionloans.los.constant.BankAccountType;
import org.junit.jupiter.api.Test;

class BankAccountTypeTest {

  @Test
  void testGetId() {
    assertEquals(1, BankAccountType.SAVINGS.getId());
    assertEquals(2, BankAccountType.CURRENT.getId());
    assertEquals(3, BankAccountType.OD.getId());
    assertEquals(4, BankAccountType.CC.getId());
    assertEquals(5, BankAccountType.OTHER.getId());
  }

  @Test
  void testGetValue() {
    assertEquals("Savings Account", BankAccountType.SAVINGS.getValue());
    assertEquals("Current Account", BankAccountType.CURRENT.getValue());
    assertEquals("OD Account", BankAccountType.OD.getValue());
    assertEquals("CC Account", BankAccountType.CC.getValue());
    assertEquals("OTHER", BankAccountType.OTHER.getValue());
  }

  @Test
  void testEnumValues() {
    BankAccountType[] values = BankAccountType.values();
    assertEquals(5, values.length);
    assertEquals(BankAccountType.SAVINGS, values[0]);
    assertEquals(BankAccountType.CURRENT, values[1]);
    assertEquals(BankAccountType.OD, values[2]);
    assertEquals(BankAccountType.CC, values[3]);
    assertEquals(BankAccountType.OTHER, values[4]);
  }

  @Test
  void testEnumValueOf() {
    assertEquals(BankAccountType.SAVINGS, BankAccountType.valueOf("SAVINGS"));
    assertEquals(BankAccountType.CURRENT, BankAccountType.valueOf("CURRENT"));
    assertEquals(BankAccountType.OD, BankAccountType.valueOf("OD"));
    assertEquals(BankAccountType.CC, BankAccountType.valueOf("CC"));
    assertEquals(BankAccountType.OTHER, BankAccountType.valueOf("OTHER"));
  }
}
