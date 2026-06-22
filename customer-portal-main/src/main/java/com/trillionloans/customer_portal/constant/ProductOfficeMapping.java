package com.trillionloans.customer_portal.constant;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

@Getter
public enum ProductOfficeMapping {
  ;

  private static final Map<Integer, String> officeMapping = new HashMap<>();

  public static void setOfficeMapping(Map<Integer, String> officeMappings) {
    officeMapping.clear();
    officeMapping.putAll(officeMappings);
  }

  public static String getOfficeNameByProductId(int productId) {
    return officeMapping.getOrDefault(productId, "NO OFFICE FOR THIS PRODUCT");
  }
}
