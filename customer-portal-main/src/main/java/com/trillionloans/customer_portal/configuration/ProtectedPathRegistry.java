package com.trillionloans.customer_portal.configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

/**
 * Registry that scans all controller endpoints at application startup and registers those annotated
 * with {@link ProtectedPath}.
 *
 * <p>This allows dynamic detection of protected routes without hardcoding URL patterns. The
 * registered patterns are later used in filters (e.g., {@code DynamicPathVariableFilter}) to apply
 * path-based access validation.
 *
 * <p>NOTE: This relies on Spring's {@link RequestMappingHandlerMapping} to introspect controller
 * request mappings.
 */
@Slf4j
@Component
public class ProtectedPathRegistry {

  private final RequestMappingHandlerMapping handlerMapping;

  // Holds parsed PathPattern objects for all protected endpoints
  @Getter private List<PathPattern> protectedPatterns = new ArrayList<>();

  public ProtectedPathRegistry(RequestMappingHandlerMapping handlerMapping) {
    this.handlerMapping = handlerMapping;
  }

  /**
   * Initializes the protected path registry by scanning all mapped handler methods and extracting
   * those annotated with {@code @ProtectedPath}.
   *
   * <p>This runs once after Spring context initialization.
   */
  @PostConstruct
  public void init() {
    // Retrieve all controller handler methods mapped by Spring WebFlux
    Map<RequestMappingInfo, HandlerMethod> mappings = handlerMapping.getHandlerMethods();

    // Filter and extract URL patterns from methods or classes marked with @ProtectedPath
    protectedPatterns =
        mappings.entrySet().stream()
            .filter(
                entry ->
                    AnnotatedElementUtils.hasAnnotation(
                            entry.getValue().getMethod(), ProtectedPath.class)
                        || AnnotatedElementUtils.hasAnnotation(
                            entry.getValue().getBeanType(), ProtectedPath.class))
            .flatMap(entry -> entry.getKey().getPatternsCondition().getPatterns().stream())
            .toList();

    log.info(
        "[ProtectedPathRegistry] Found {} protected patterns:\n{}",
        protectedPatterns.size(),
        protectedPatterns.stream()
            .map(PathPattern::getPatternString)
            .map(p -> " - " + p)
            .collect(Collectors.joining("\n")));
  }
}
