package com.trillionloans.crm.integration;

public final class ProductNameMapper {

  private ProductNameMapper() {}

  public static String resolveProductName(String productCode, Number productId) {
    return resolvePartnerName(productCode, productId);
  }

  /** LSP (Lending Service Provider) — partner brand, not the NBFC / office name. */
  public static String resolveLspName(String productCode, Number productId) {
    return resolvePartnerName(productCode, productId);
  }

  private static String resolvePartnerName(String productCode, Number productId) {
    if (productCode != null && !productCode.isBlank()) {
      return switch (productCode.trim().toLowerCase()) {
        case "bharatpe", "bharatpe_ml", "bharatpe-ml" -> "BharatPe ML";
        case "mobikwik" -> "Mobikwik";
        case "moneyview", "money_view" -> "MoneyView";
        default -> titleCase(productCode.replace('_', ' '));
      };
    }
    if (productId != null) {
      return switch (productId.intValue()) {
        case 1 -> "BharatPe ML";
        case 2 -> "Mobikwik";
        case 3 -> "MoneyView";
        default -> "Loan product " + productId;
      };
    }
    return "Loan";
  }

  private static String titleCase(String value) {
    String[] parts = value.split("\\s+");
    StringBuilder builder = new StringBuilder();
    for (String part : parts) {
      if (part.isBlank()) {
        continue;
      }
      if (!builder.isEmpty()) {
        builder.append(' ');
      }
      builder
          .append(Character.toUpperCase(part.charAt(0)))
          .append(part.substring(1).toLowerCase());
    }
    return builder.isEmpty() ? "Loan" : builder.toString();
  }
}
