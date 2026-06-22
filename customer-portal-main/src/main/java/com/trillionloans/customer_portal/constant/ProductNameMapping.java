package com.trillionloans.customer_portal.constant;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Getter
@Slf4j
public enum ProductNameMapping {
  ;

  private static final Map<String, String> productNameMapping = new HashMap<>();

  public static void setProductNameMapping(Map<String, String> mappings) {
    productNameMapping.clear();
    productNameMapping.putAll(mappings);
  }

  public static String getProductNameByProductId(String productId) {
    if (productId == null || productId.isBlank()) {
      return StringUtils.EMPTY;
    }

    try {
      String productName = productNameMapping.get(productId);
      if (productName == null) {
        log.warn("No product name found for product code: {}", productId);
        return StringUtils.EMPTY;
      }
      return productName;

    } catch (IllegalArgumentException e) {
      log.error("Invalid argument while fetching product name for productId={}", productId, e);
      return StringUtils.EMPTY;

    } catch (NullPointerException e) {
      log.error(
          "Null value encountered while fetching product name for productId={}", productId, e);
      return StringUtils.EMPTY;

    } catch (RuntimeException e) {
      // Catches only unexpected runtime issues, not all exceptions
      log.error(
          "Unexpected runtime error while fetching product name for productId={}", productId, e);
      return StringUtils.EMPTY;
    }
  }
}
