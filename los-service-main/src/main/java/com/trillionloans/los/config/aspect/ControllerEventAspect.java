package com.trillionloans.los.config.aspect;

import com.trillionloans.los.config.annotations.ControllerEvent;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ControllerEventAspect {

  private final KafkaEventProducerService eventProducerService;

  @Pointcut("@annotation( com.trillionloans.los.config.annotations.ControllerEvent)")
  public void eventMappingMethods() {}

  @Before("eventMappingMethods()")
  public void captureEvent(JoinPoint joinPoint) {
    try {
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();
      Method method = signature.getMethod();

      ControllerEvent eventMapping = method.getAnnotation(ControllerEvent.class);
      Event event = eventMapping.event();

      Object requestBody = extractRequestBody(joinPoint.getArgs(), method);
      EventContext eventContext = new EventContext(event);
      log.info("Publishing controller event: {}, body: {}", event, requestBody);
      publishEventKafkaAsync(
          () -> eventProducerService.publishEvent(eventContext, null, requestBody));
    } catch (Exception e) {
      log.error("Error occured when publishing event", e);
    }
  }

  private Object extractRequestBody(Object[] args, Method method) {
    Annotation[][] paramAnnotations = method.getParameterAnnotations();

    for (int i = 0; i < args.length; i++) {
      for (Annotation annotation : paramAnnotations[i]) {
        if (annotation.annotationType().equals(RequestBody.class)) {
          return args[i];
        }
      }
    }
    return null;
  }

  private void publishEventKafkaAsync(Runnable eventTask) {
    Mono.fromRunnable(eventTask)
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(null, error -> log.error("Error while publishing the event", error));
  }
}
