package com.trillionloans.lms.exception;

import static com.trillionloans.lms.constant.ResponseStatus.*;
import static com.trillionloans.lms.constant.StringConstants.PRODUCT_CODE;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.lms.model.dto.M2pErrorResponseDTO;
import com.trillionloans.lms.model.dto.ResponseDTO;
import jakarta.validation.ConstraintViolationException;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@Configuration
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final String TRACE_ID = "traceId";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<?> handleNotFoundException(NotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ResponseDTO.builder()
                .status(FAIL)
                .traceId(MDC.get(TRACE_ID))
                .message(ex.getMessage())
                .build());
  }

  @ExceptionHandler(MethodNotAllowedException.class)
  public ResponseEntity<?> handleMethodNotAllowedException(MethodNotAllowedException ex) {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(
            ResponseDTO.builder()
                .status(ERROR)
                .traceId(MDC.get(TRACE_ID))
                .message(ex.getMessage())
                .build());
  }

  @ExceptionHandler(TimeoutException.class)
  public Mono<ResponseEntity<?>> handleTimeoutException(TimeoutException ex) {
    log.error("[Error] [TimeoutException] Downstream service timeout occurred");
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(
                        ResponseDTO.builder()
                            .status(ERROR)
                            .message("timeout exception occurred while calling downstream services")
                            .traceId(context.get(TRACE_ID))
                            .data(null)
                            .build())));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public Mono<ResponseEntity<?>> constraintViolationException(ConstraintViolationException ex) {
    log.error("[Error] [ConstraintViolationException] {}", ex.getMessage());
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(
                        ResponseDTO.builder()
                            .status(FAIL)
                            .message(ex.getMessage())
                            .data(parseClientResponse(ex.getMessage()))
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  @ExceptionHandler(ServerErrorException.class)
  public ResponseEntity<?> handleServerErrorException(ServerErrorException ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ResponseDTO.builder()
                .status(ERROR)
                .traceId(MDC.get(TRACE_ID))
                .message(ex.getMessage())
                .data(parseClientResponse(ex.getClientResponse()))
                .build());
  }

  @ExceptionHandler(ClientSideException.class)
  public ResponseEntity<?> handleClientSideException(ClientSideException ex) {
    return ResponseEntity.status(ex.getHttpStatusCode())
        .body(
            ResponseDTO.builder()
                .status(FAIL)
                .traceId(MDC.get(TRACE_ID))
                .message(ex.getMessage())
                .data(parseClientResponse(ex.getClientResponse()))
                .build());
  }

  @ExceptionHandler(BaseException.class)
  public ResponseEntity<?> handleBaseException(BaseException ex) {
    log.error("[Error] [BaseException] Request failed");
    return ResponseEntity.status(ex.getHttpStatusCode())
        .body(
            ResponseDTO.builder()
                .status(FAIL)
                .traceId(MDC.get(TRACE_ID))
                .message("Request could not be processed due to a business validation error")
                .build());
  }

  @ExceptionHandler(CustomException.class)
  public ResponseEntity<?> handleCustomException(CustomException ex) {

    log.error("[Error] [CustomException] Application error occurred");
    return ResponseEntity.status(ex.getHttpStatusCode())
        .body(
            ResponseDTO.builder()
                .status(ERROR)
                .traceId(MDC.get(TRACE_ID))
                .message("An unexpected error occurred. Please try again later.")
                .build());
  }

  @ExceptionHandler({WebExchangeBindException.class, DateTimeException.class})
  public ResponseEntity<?> handleNotValidException(WebExchangeBindException ex) {
    log.error("[Error] [WebExchangeBindException] {}", ex.getMessage());
    List<Map<String, String>> errorsList = new ArrayList<>();
    ex.getAllErrors()
        .forEach(
            err -> {
              Map<String, String> errorsMap = new HashMap<>();
              errorsMap.put("developerMessage", err.getDefaultMessage());
              errorsMap.put("defaultUserMessage", err.getDefaultMessage());
              errorsList.add(errorsMap);
            });

    Map<String, Object> result = new HashMap<>();
    result.put("errors", errorsList);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ResponseDTO.builder()
                .status(FAIL)
                .message("validations failed, please provide proper input")
                .traceId(MDC.get(TRACE_ID))
                .data(parseClientResponse(result))
                .build());
  }

  @ExceptionHandler(ServerWebInputException.class)
  public ResponseEntity<?> handleServerWebInputException(ServerWebInputException ex) {
    log.error("[ServerWebInputException] {}", ex.getMessage());
    String message;
    HttpStatus statusCode = HttpStatus.BAD_REQUEST;

    if (ex.getCause() instanceof DecodingException
        && ex.getCause().getCause() instanceof JsonMappingException) {
      return handleJsonMappingException((JsonMappingException) ex.getCause().getCause());
    } else if (ex instanceof MissingRequestValueException missingRequestValueException
        && missingRequestValueException.getLabel().equals("header")
        && missingRequestValueException.getName().equals(PRODUCT_CODE)) {
      message = "partnerId header missing, please provide proper headers";
    } else {
      message = "Invalid Request";
    }
    return ResponseEntity.status(statusCode)
        .body(
            ResponseDTO.builder()
                .status(ERROR)
                .message(message)
                .data(parseClientResponse("Bad request"))
                .traceId(MDC.get(TRACE_ID))
                .build());
  }

  @ExceptionHandler(JsonMappingException.class)
  public ResponseEntity<?> handleJsonMappingException(JsonMappingException ex) {
    log.error("[JsonMappingException] {}", ex.getMessage());
    HttpStatus statusCode = HttpStatus.BAD_REQUEST;

    // Extract field names from the exception
    List<String> fieldNames =
        ex.getPath().stream().map(JsonMappingException.Reference::getFieldName).toList();

    return ResponseEntity.status(statusCode)
        .body(
            ResponseDTO.builder()
                .status(ERROR)
                .message("Payload is incorrect, please provide correct payload")
                .data(parseClientResponse("Incorrect fields: " + String.join(", ", fieldNames)))
                .traceId(MDC.get(TRACE_ID))
                .build());
  }

  @ExceptionHandler(UnsupportedMediaTypeException.class)
  public ResponseEntity<?> handleUnsupportedMediaTypeException(UnsupportedMediaTypeException ex) {
    log.error("[UnsupportedMediaTypeException] {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(
            ResponseDTO.builder()
                .status(SERVER_ERROR)
                .message(ex.getMessage())
                .data(parseClientResponse(ex.getMessage()))
                .traceId(MDC.get(TRACE_ID))
                .build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleException(Exception ex) {

    log.error("[Exception] Unexpected server error occurred");

    HttpStatus statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
    String clientMessage = "Something went wrong. Please try again later."; // default SAFE message

    if (ex instanceof MethodNotAllowedException) {
      statusCode = HttpStatus.METHOD_NOT_ALLOWED;
      clientMessage = "HTTP method not allowed for this endpoint";

    } else if (ex instanceof NoResourceFoundException) {
      statusCode = HttpStatus.NOT_FOUND;
      clientMessage = "Requested resource was not found";

    } else if (ex instanceof ResponseStatusException) {
      statusCode = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
      clientMessage = "Unsupported media type in request";
    }

    return ResponseEntity.status(statusCode)
        .body(
            ResponseDTO.builder()
                .status(ERROR)
                .message(clientMessage)
                .data(parseClientResponse(clientMessage))
                .traceId(MDC.get(TRACE_ID))
                .build());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
    log.error("[Error] [IllegalArgumentException]", ex);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ResponseDTO.builder()
                .status(ERROR)
                .message(ex.getMessage())
                .traceId(MDC.get(TRACE_ID))
                .build());
  }

  @ExceptionHandler(MissingRequestValueException.class)
  public ResponseEntity<?> handleMissingRequestValueException(Exception ex) {
    log.error("[Error] [MissingRequestValueException]", ex);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ResponseDTO.builder()
                .status(ERROR)
                .message(ex.getMessage())
                .data(parseClientResponse(ex.getMessage()))
                .traceId(MDC.get(TRACE_ID))
                .build());
  }

  public static Object parseClientResponse(Object clientResponse) {
    if (clientResponse == null) return null;

    if (clientResponse instanceof M2pErrorResponseDTO || clientResponse instanceof Map) {
      return clientResponse;
    }

    if (!(clientResponse instanceof String responseStr)) {
      return clientResponse; // Let everything else pass through unmodified
    }

    try {
      return OBJECT_MAPPER.readValue(responseStr, M2pErrorResponseDTO.class);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      log.debug("Failed to parse as M2pErrorResponseDTO: {}", e.getMessage());
    }

    try {
      return OBJECT_MAPPER.readValue(responseStr, Map.class);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      log.debug("Failed to parse as Map: {}", e.getMessage());
    }

    M2pErrorResponseDTO fallback = new M2pErrorResponseDTO();
    fallback.setDeveloperMessage(responseStr);
    fallback.setDefaultUserMessage(responseStr);
    return fallback;
  }
}
