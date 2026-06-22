package com.trillionloans.customer_portal.util;

import static com.trillionloans.customer_portal.constant.StringConstants.DD_MM_YYYY;
import static com.trillionloans.customer_portal.constant.StringConstants.MMM_D_YYYY;
import static com.trillionloans.customer_portal.constant.StringConstants.YYYY_MM_DD;

import io.netty.util.internal.StringUtil;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class DateTimeUtil {

  private DateTimeUtil() {}

  public static String getDateAsString(LocalDate localDate) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(YYYY_MM_DD);
    return localDate.format(formatter);
  }

  public static String convertDate(String dateStr) {
    if (dateStr != null && !dateStr.isBlank()) {
      DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(MMM_D_YYYY);
      DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(DD_MM_YYYY);
      LocalDate date = LocalDate.parse(dateStr, inputFormatter);
      return date.format(outputFormatter);
    }
    return null;
  }

  public static String formatEpochMilliDate(Long epochMillis, String format) {
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    Date date = new Date(epochMillis);
    return sdf.format(date);
  }

  public static String formatNumber(int number) {
    return String.format("%02d", number);
  }

  public static LocalDate parseDate(String dateStr, String pattern) {
    if (dateStr != null && !dateStr.isBlank()) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
      return LocalDate.parse(dateStr, formatter);
    }
    return null;
  }

  public static String parseDateFromPattern(
      String dateStr, String inputPattern, String outputPattern) {
    if (!CommonUtil.nullOrEmpty(dateStr)
        && !CommonUtil.nullOrEmpty(inputPattern)
        && !CommonUtil.nullOrEmpty(outputPattern)) {
      try {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern(inputPattern);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern(outputPattern);
        LocalDate date = LocalDate.parse(dateStr, inputFormatter);
        return date.format(outputFormatter);
      } catch (DateTimeParseException e) {
        // Parsing failed, return null
        return null;
      }
    }
    return null;
  }

  public static LocalDate parseDate(List<Integer> dateParts) {
    if (CommonUtil.nullOrEmpty(dateParts) || dateParts.size() < 3) {
      return null;
    }
    try {
      return LocalDate.of(dateParts.get(0), dateParts.get(1), dateParts.get(2));
    } catch (DateTimeException | IndexOutOfBoundsException e) {
      return null;
    }
  }

  public static String formatDate(LocalDate localDate, String pattern) {
    if (localDate == null) return null;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    return localDate.format(formatter);
  }

  public static <T> Comparator<T> comparingByParsedDate(
      Function<T, String> dateExtractor, String pattern) {
    return Comparator.comparing(
        item -> {
          LocalDate date = parseDate(dateExtractor.apply(item), pattern);
          return date != null ? date : LocalDate.MAX;
        });
  }

  /** Compares two date strings in descending order (latest first). */
  public static int compareDatesDesc(String dateStr1, String dateStr2, String pattern) {
    LocalDate d1 = parseDate(dateStr1, pattern);
    LocalDate d2 = parseDate(dateStr2, pattern);
    return d2.compareTo(d1);
  }

  /** Returns current date formatted as string in the provided pattern. */
  public static String getCurrentDate(String pattern) {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
    return LocalDate.now().format(formatter);
  }

  /** Validates whether a date string matches the given pattern. */
  public static boolean isValidDate(String dateStr, String pattern) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
      LocalDate.parse(dateStr, formatter);
      return true;
    } catch (DateTimeParseException | IllegalArgumentException e) {
      return false;
    }
  }

  // convert months in number to string format
  public static String getDateAsMonthInString(List<Integer> date) {
    if (date != null) {
      StringBuilder sb = new StringBuilder();
      sb.append(date.get(2))
          .append(StringUtil.SPACE)
          .append(String.valueOf(Month.of(date.get(1))), 0, 3)
          .append(StringUtil.SPACE)
          .append(date.get(0));

      return sb.toString();
    } else {
      return null;
    }
  }
}
