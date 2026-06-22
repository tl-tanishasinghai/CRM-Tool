package com.trillionloans.los.util;

import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

@Slf4j
public class Util {
  private final Environment env;

  public Util(Environment environment) {
    this.env = environment;
  }

  /** Casts the provided object to Map<String, Object>. */
  public static Map<String, Object> castToMap(Object object, String errorMessage) {
    if (!(object instanceof Map)) {
      throw new IllegalArgumentException(errorMessage);
    }
    return (Map<String, Object>) object;
  }

  public static Integer safeParseInt(String value) {
    if (StringUtils.isBlank(value)) {
      return null;
    }
    try {
      return Integer.parseInt(value.trim());
    } catch (NumberFormatException e) {
      log.error("Failed to parse ID to Integer for Redis: {}", value);
      return null;
    }
  }

  public Map<String, String> getPropertiesByPrefix(String prefix) {
    Map<String, String> properties = new HashMap<>();
    if (env instanceof ConfigurableEnvironment configurableEnvironment) {
      for (PropertySource<?> propertySource : configurableEnvironment.getPropertySources()) {
        if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource) {
          for (String key : enumerablePropertySource.getPropertyNames()) {
            getPropertyValue(key, prefix, properties, propertySource);
          }
        }
      }
    }
    return properties;
  }

  private void getPropertyValue(
      String key, String prefix, Map<String, String> properties, PropertySource<?> propertySource) {
    if (key == null || prefix == null || properties == null || propertySource == null) {
      log.error("[Warning] Null input detected. Aborting property retrieval.");
      return;
    }
    if (!key.startsWith(prefix)) {
      return;
    }
    try {
      String keyName = key.replaceFirst("^" + prefix, "");
      Object value = propertySource.getProperty(key);
      if (value != null) properties.put(keyName, value.toString());
    } catch (IllegalStateException | ClassCastException ex) {
      log.error("[Error] error while setting properties", ex.getMessage());
    }
  }
}
