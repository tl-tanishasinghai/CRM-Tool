package com.trillionloans.customer_portal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;
import org.junit.jupiter.api.Test;

class DateTimeUtilTest {

  @Test
  void testGetDateAsString() {
    LocalDate localDate = LocalDate.of(2025, 2, 10);
    String formattedDate = DateTimeUtil.getDateAsString(localDate);
    assertEquals("2025-02-10", formattedDate);
  }

  @Test
  void testConvertDate() {
    String dateStr = "Feb 10, 2025";
    String convertedDate = DateTimeUtil.convertDate(dateStr);
    assertEquals("10/02/2025", convertedDate);
  }

  @Test
  void testConvertDateNullInput() {
    String dateStr = null;
    String convertedDate = DateTimeUtil.convertDate(dateStr);
    assertNull(convertedDate);
  }

  @Test
  void testConvertDateEmptyInput() {
    String dateStr = "";
    String convertedDate = DateTimeUtil.convertDate(dateStr);
    assertNull(convertedDate);
  }

  @Test
  void testFormatEpochMilliDate() {
    Long epochMilli = System.currentTimeMillis();
    String format = "yyyy-MM-dd HH:mm:ss";
    String formattedDate = DateTimeUtil.formatEpochMilliDate(epochMilli, format);

    // Validate the formatted date using SimpleDateFormat (manual check)
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    Date date = new Date(epochMilli);
    String expectedFormattedDate = sdf.format(date);

    assertEquals(expectedFormattedDate, formattedDate);
  }

  @Test
  void testFormatEpochMilliDateWithCustomFormat() {
    Long epochMilli = 1676008800000L;
    String format = "yyyy/MM/dd HH:mm:ss";
    String formattedDate = DateTimeUtil.formatEpochMilliDate(epochMilli, format);

    SimpleDateFormat sdf = new SimpleDateFormat(format);
    Date date = new Date(epochMilli);
    String expectedFormattedDate = sdf.format(date);

    assertEquals(expectedFormattedDate, formattedDate);
  }
}
