package com.trillionloans.los.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class DOBDateParserUtil {

  private DOBDateParserUtil() {}

  // Expected date formats
  private static final DateTimeFormatter DD_DASH_MM_DASH_YYYY_FORMATTER =
      DateTimeFormatter.ofPattern("dd-MM-yyyy");
  private static final DateTimeFormatter DD_MM_YYYY_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy");
  private static final DateTimeFormatter MMM_DD_YYYY_FORMATTER =
      DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH);
  private static final DateTimeFormatter MMM_D_YYYY_FORMATTER =
      DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

  private static final List<DateTimeFormatter> DATE_FORMATTERS =
      Arrays.asList(
          DD_DASH_MM_DASH_YYYY_FORMATTER,
          DD_MM_YYYY_FORMATTER,
          MMM_DD_YYYY_FORMATTER,
          MMM_D_YYYY_FORMATTER);

  // Default Day/Month for partial Aadhaar DOB (only year present)
  private static final String DEFAULT_DATE_PREFIX = "01-01-";

  /**
   * Attempts to parse a date string using a list of predefined formatters.
   *
   * @param dateString The date string to parse.
   * @return The parsed LocalDate.
   * @throws DateTimeParseException if no formatter can successfully parse the string.
   */
  public static LocalDate parseDate(String dateString) throws DateTimeParseException {
    for (DateTimeFormatter formatter : DATE_FORMATTERS) {
      try {
        return LocalDate.parse(dateString, formatter);
      } catch (DateTimeParseException e) {
        // Ignore and try the next formatter
      }
    }
    throw new DateTimeParseException(
        "Unable to parse date: " + dateString + ". All formatters failed.", dateString, 0);
  }

  /**
   * Safely parses the Aadhaar date string, accommodating the case where only the year (YYYY) is
   * present. If only a 4-digit year is found, it prepends '01-01-' to default the day and month.
   *
   * @param dobString The Aadhaar DOB string.
   * @return The parsed LocalDate.
   * @throws DateTimeParseException if parsing fails.
   */
  public static LocalDate safeParseAadhaarDate(String dobString) throws DateTimeParseException {
    // 1. Check for YYYY format (Aadhaar may only provide the year)
    if (dobString.matches("^\\d{4}$")) {
      try {
        // Prepend "01-01-" to create a full date string
        String fullDateString = DEFAULT_DATE_PREFIX + dobString;
        // Use the primary date parser for the standard format
        return parseDate(fullDateString);
      } catch (NumberFormatException | DateTimeParseException e) {
        throw new DateTimeParseException("Invalid year format for Aadhaar DOB", dobString, 0, e);
      }
    }

    // 2. If it is not YYYY, assume it is a full date and try standard parsing
    return parseDate(dobString);
  }
}
