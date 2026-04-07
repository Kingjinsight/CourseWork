package uk.ac.ed.inf.eventsapp.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import uk.ac.ed.inf.eventsapp.model.EventType;

/**
 * Shared parsing helpers for user-entered text values.
 *
 * <p>
 * These helpers centralise the text-to-value conversions used by the controllers so that input
 * validation rules stay consistent across use cases.
 */
public final class InputParsers {
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
  private static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final Pattern MONEY_PATTERN = Pattern.compile("^(?:0|[1-9]\\d*)(?:\\.\\d{1,2})?$");

  private InputParsers() {}

  /**
   * Parses a user-supplied event type.
   *
   * @param rawType the raw event type text
   * @return the parsed event type, or {@code null} if the input is invalid
   */
  public static EventType parseEventType(String rawType) {
    if (rawType == null) {
      return null;
    }

    String normalised = rawType.trim().toUpperCase().replace(' ', '_');
    if (normalised.isEmpty()) {
      return null;
    }

    if ("THEATER".equals(normalised)) {
      normalised = "THEATRE";
    }

    try {
      return EventType.valueOf(normalised);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }

  /**
   * Checks whether a string is a syntactically valid email address.
   *
   * @param email the candidate email address
   * @return {@code true} if the address matches the accepted pattern
   */
  public static boolean isValidEmail(String email) {
    return email != null && !email.isBlank() && EMAIL_PATTERN.matcher(email.trim()).matches();
  }

  /**
   * Parses a yes/no-style value used by the text UI.
   *
   * @param rawBoolean the raw user input
   * @return the parsed boolean, or {@code null} if the input is invalid
   */
  public static Boolean parseBoolean(String rawBoolean) {
    if (rawBoolean == null) {
      return null;
    }

    String normalised = rawBoolean.trim().toLowerCase();
    return switch (normalised) {
      case "y", "yes", "true", "ticketed", "outdoors", "smoking" -> Boolean.TRUE;
      case "n", "no", "false", "non-ticketed", "indoors", "non-smoking" -> Boolean.FALSE;
      default -> null;
    };
  }

  /**
   * Parses a strictly positive integer.
   *
   * @param rawInteger the raw user input
   * @return the parsed integer, or {@code null} if the input is invalid
   */
  public static Integer parsePositiveInteger(String rawInteger) {
    Integer parsed = parseNonNegativeInteger(rawInteger);
    return parsed != null && parsed > 0 ? parsed : null;
  }

  /**
   * Parses a strictly positive long integer.
   *
   * @param rawLong the raw user input
   * @return the parsed long, or {@code null} if the input is invalid
   */
  public static Long parsePositiveLong(String rawLong) {
    if (rawLong == null) {
      return null;
    }

    try {
      long parsed = Long.parseLong(rawLong.trim());
      return parsed > 0 ? parsed : null;
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  /**
   * Parses a non-negative integer.
   *
   * @param rawInteger the raw user input
   * @return the parsed integer, or {@code null} if the input is invalid
   */
  public static Integer parseNonNegativeInteger(String rawInteger) {
    if (rawInteger == null) {
      return null;
    }

    try {
      int parsed = Integer.parseInt(rawInteger.trim());
      return parsed >= 0 ? parsed : null;
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  /**
   * Parses an integer phone number.
   *
   * @param rawPhoneNumber the raw phone-number text
   * @return the parsed phone number, or {@code null} if the input is invalid
   */
  public static Integer parsePhoneNumber(String rawPhoneNumber) {
    if (rawPhoneNumber == null) {
      return null;
    }

    try {
      return Integer.valueOf(rawPhoneNumber.trim());
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  /**
   * Parses a non-negative money value with at most two decimal places.
   *
   * @param rawDouble the raw user input
   * @return the parsed double, or {@code null} if the input is invalid
   */
  public static Double parseNonNegativeDouble(String rawDouble) {
    if (rawDouble == null) {
      return null;
    }

    String trimmedValue = rawDouble.trim();
    if (!MONEY_PATTERN.matcher(trimmedValue).matches()) {
      return null;
    }

    try {
      double parsed = Double.parseDouble(trimmedValue);
      return parsed >= 0.0 ? parsed : null;
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  /**
   * Parses a date/time in {@code yyyy-MM-dd HH:mm} format.
   *
   * @param rawDateTime the raw user input
   * @return the parsed date/time, or {@code null} if the input is invalid
   */
  public static LocalDateTime parseDateTime(String rawDateTime) {
    if (rawDateTime == null) {
      return null;
    }

    try {
      return LocalDateTime.parse(rawDateTime.trim(), DATE_TIME_FORMATTER);
    } catch (DateTimeParseException exception) {
      return null;
    }
  }

  /**
   * Parses a date in {@code yyyy-MM-dd} format.
   *
   * @param rawDate the raw user input
   * @return the parsed date, or {@code null} if the input is invalid
   */
  public static LocalDate parseDate(String rawDate) {
    if (rawDate == null) {
      return null;
    }

    try {
      return LocalDate.parse(rawDate.trim(), DATE_FORMATTER);
    } catch (DateTimeParseException exception) {
      return null;
    }
  }

  /**
   * Splits a comma-separated list into trimmed non-empty values.
   *
   * @param rawValues the raw comma-separated text
   * @return the parsed values
   */
  public static List<String> parseCommaSeparatedValues(String rawValues) {
    List<String> values = new ArrayList<>();
    if (rawValues == null) {
      return values;
    }

    for (String rawValue : rawValues.split(",")) {
      String trimmedValue = rawValue.trim();
      if (!trimmedValue.isEmpty()) {
        values.add(trimmedValue);
      }
    }

    return values;
  }
}
