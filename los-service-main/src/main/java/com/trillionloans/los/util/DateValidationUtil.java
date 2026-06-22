package com.trillionloans.los.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateValidationUtil {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

  public static void validateDateOrder(
      String firstField, String firstDateStr, String secondField, String secondDateStr) {

    if (firstField == null
        || firstField.isBlank()
        || secondField == null
        || secondField.isBlank()) {
      return;
    }

    try {
      LocalDate firstDate = LocalDate.parse(firstField, FORMATTER);
      LocalDate secondDate = LocalDate.parse(secondField, FORMATTER);

      if (secondDate.isBefore(firstDate)) {
        throw new IllegalArgumentException(
            String.format(
                "[%s] %s must not be before [%s] %s",
                secondField, secondDateStr, firstField, firstDateStr));
      }

    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid date format for [%s] or [%s]. Expected dd-MM-yyyy.",
              firstField, secondField));
    }
  }
}
