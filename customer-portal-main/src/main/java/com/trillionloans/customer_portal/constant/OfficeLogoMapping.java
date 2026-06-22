package com.trillionloans.customer_portal.constant;

import java.util.HashMap;
import java.util.Map;

public enum OfficeLogoMapping {
  ;

  private static final Map<String, String> urlMapping = new HashMap<>();

  public static void setUrlMapping(Map<String, String> urlMappings) {
    urlMapping.clear();
    urlMapping.putAll(urlMappings);
  }

  public static String getUrlByOfficeName(String officeName) {
    return urlMapping.getOrDefault(officeName, "NO LOGO FOR THIS OFFICE");
  }
}
