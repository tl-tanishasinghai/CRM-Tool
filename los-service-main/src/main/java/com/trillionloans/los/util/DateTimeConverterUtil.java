package com.trillionloans.los.util;

import static com.trillionloans.los.constant.StringConstants.ASIA_KOLKATA;
import static com.trillionloans.los.constant.StringConstants.DD_MM_YYYY;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeConverterUtil {

  private DateTimeConverterUtil() {}

  public static String getTodayDate() {
    LocalDate date = LocalDate.now(ZoneId.of(ASIA_KOLKATA));
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DD_MM_YYYY);
    return date.format(formatter);
  }

  public static String convertToGivenDateFormat(
      String dateStr, String inputFormat, String outputFormat) {
    DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputFormat);
    DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputFormat);
    LocalDate date = LocalDate.parse(dateStr, inputFormatter);
    return date.format(outputFormatter);
  }

  public static String convertEpochMilliToIst(long epochMilli) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    sdf.setTimeZone(TimeZone.getTimeZone(ASIA_KOLKATA));
    return sdf.format(new Date(epochMilli));
  }

  public static String convertEpochToFormattedIst(long epochMilli) {
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm");
    simpleDateFormat.setTimeZone(TimeZone.getTimeZone(ASIA_KOLKATA));
    return simpleDateFormat.format(new Date(epochMilli));
  }
}
