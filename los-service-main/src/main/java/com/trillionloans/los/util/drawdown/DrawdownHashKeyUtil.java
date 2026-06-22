package com.trillionloans.los.util.drawdown;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public final class DrawdownHashKeyUtil {

  private static final String DELIMITER = "|";

  private DrawdownHashKeyUtil() {}

  public static String invoiceHash(
      String partnerId,
      String anchorId,
      String invoiceNumber,
      LocalDate invoiceDate,
      String uniqueKey) {
    String canonical =
        String.join(
            DELIMITER,
            safe(partnerId),
            safe(anchorId),
            safe(invoiceNumber),
            safe(invoiceDate),
            safe(uniqueKey));

    return sha256Hex(canonical);
  }

  private static String safe(Object value) {
    if (value == null) return StringUtils.EMPTY;
    if (value instanceof BigDecimal bd) {
      return bd.stripTrailingZeros().toPlainString();
    }
    return value.toString().trim();
  }

  private static String sha256Hex(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to generate SHA-256 hash", e);
    }
  }
}
