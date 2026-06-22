package com.trillionloans.los.config.aspect;

import static com.trillionloans.los.constant.StringConstants.REQUEST_LOG;

import com.google.gson.Gson;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Aspect class for logging the method details from controller classes The request body is also
 * printed using Aspect features like JointPoint, Advice
 */
@Aspect
@Slf4j
@Component
@AllArgsConstructor
public class LoggingAspectConfiguration {
  private final Gson gson;

  @Before(
      "@annotation(org.springframework.web.bind.annotation.PostMapping) "
          + "|| @annotation(org.springframework.web.bind.annotation.PutMapping)")
  public void logBeforePostOrPutMapping(JoinPoint joinPoint) {
    Object[] args = joinPoint.getArgs();
    if (includeLoggingForRequest(joinPoint)) {
      int argIndex = 0;
      for (Object arg : args) {
        if (arg != null && hasRequestBodyAnnotation(argIndex, joinPoint)) {
          log.info(
              "[{}] method: {}, body: {}",
              REQUEST_LOG,
              ((MethodSignature) joinPoint.getSignature()).getMethod().getName(),
              gson.toJson(arg));
        }
        ++argIndex;
      }
    }
  }

  private boolean includeLoggingForRequest(JoinPoint joinPoint) {
    List<Annotation> appliedAnnotations =
        Arrays.stream(
                ((MethodSignature) joinPoint.getSignature()).getMethod().getDeclaredAnnotations())
            .toList();
    for (Annotation annotation : appliedAnnotations) {
      if (annotation instanceof PostMapping postMappingAnnotation) {
        return Arrays.stream(postMappingAnnotation.value()).noneMatch(this::excludeUriForLogging);
      }
      if (annotation instanceof PutMapping putMappingAnnotation) {
        return Arrays.stream(putMappingAnnotation.value()).noneMatch(this::excludeUriForLogging);
      }
    }
    return true;
  }

  private boolean excludeUriForLogging(String uri) {
    return uri.contains("image") || uri.contains("upload");
  }

  private boolean hasRequestBodyAnnotation(int argIndex, JoinPoint joinPoint) {
    Annotation[][] parameterAnnotations =
        ((MethodSignature) joinPoint.getSignature()).getMethod().getParameterAnnotations();
    int annotationsArrayIndex = 0;
    for (Annotation[] annotationsArray : parameterAnnotations) {
      if (Arrays.stream(annotationsArray)
              .anyMatch(annotation -> annotation.annotationType().equals(RequestBody.class))
          && annotationsArrayIndex == argIndex) {
        return true;
      }
      ++annotationsArrayIndex;
    }
    return false;
  }
}
