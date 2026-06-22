package com.trillionloans.customer_portal.util;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class CommonUtil {

  private CommonUtil() {}

  public static boolean nullOrEmpty(String string) {
    return string == null || string.isEmpty();
  }

  public static boolean nullOrEmpty(List<?> list) {
    return list == null || list.isEmpty();
  }

  public static boolean nullOrEmpty(Object object) {
    return object == null || nullOrEmpty(object.toString());
  }
}
