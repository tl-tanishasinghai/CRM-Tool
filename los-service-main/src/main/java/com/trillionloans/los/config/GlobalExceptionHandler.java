package com.trillionloans.los.config;

import static com.trillionloans.los.constant.ResponseStatus.*;
import static com.trillionloans.los.constant.StringConstants.PARTNER_ID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.CustomTimeoutException;
import com.trillionloans.los.exception.ForbiddenException;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.exception.PanValidationExceptions.PanBasedLoanRejectionException;
import com.trillionloans.los.exception.ServerErrorException;
import com.trillionloans.los.model.response.ResponseDTO;
import com.trillionloans.los.model.response.m2p.M2pErrorResponseDTO;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.DecodingException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import org.springframework.web.server.MethodNotAllowedException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;

/** Rest Controller Advice class for handling the exceptions at the root level */
@Configuration
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final String TRACE_ID = "traceId";
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @ExceptionHandler(NotFoundException.class)
  public Mono<ResponseEntity<?>> handleNotFoundException(NotFoundException ex) {
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                        ResponseDTO.builder()
                            .status(FAIL)
                            .message(ex.getMessage())
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  @ExceptionHandler(ServerErrorException.class)
  public Mono<ResponseEntity<?>> handleServerErrorException(ServerErrorException ex) {
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        ResponseDTO.builder()
                            .status(SERVER_ERROR)
                            .message(ex.getMessage())
                            .traceId(context.get(TRACE_ID))
                            .data(parseClientResponse(ex.getClientResponse()))
                            .build())));
  }

  @ExceptionHandler(ClientSideException.class)
  public Mono<ResponseEntity<?>> handleClientSideException(ClientSideException ex) {
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(ex.getHttpStatusCode())
                    .body(
                        ResponseDTO.builder()
                            .status(FAIL)
                            .message(ex.getMessage())
                            .traceId(context.get(TRACE_ID))
                            .data(parseClientResponse(ex.getClientResponse()))
                            .build())));
  }

  @ExceptionHandler(ForbiddenException.class)
  public Mono<ResponseEntity<?>> handleForbiddenException(ForbiddenException ex) {
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(ex.getHttpStatusCode())
                    .body(
                        ResponseDTO.builder()
                            .status(FAIL)
                            .message(ex.getMessage())
                            .traceId(context.get(TRACE_ID))
                            .data(parseClientResponse(ex.getClientResponse()))
                            .build())));
  }

  @ExceptionHandler(BaseException.class)
  public Mono<ResponseEntity<?>> handleBaseException(BaseException ex) {
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(ex.getHttpStatusCode())
                    .body(
                        ResponseDTO.builder()
                            .status(FAIL)
                            .message(ex.getMessage())
                            .traceId(context.get(TRACE_ID))
                            .data(parseClientResponse(ex.getClientResponse()))
                            .build())));
  }

  @ExceptionHandler(UnsupportedMediaTypeException.class)
  public Mono<ResponseEntity<?>> handleUnsupportedMediaTypeException(
      UnsupportedMediaTypeException ex) {
    log.error("[Error] [UnsupportedMediaTypeException] {}", ex.getMessage());
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

  @ExceptionHandler(IncorrectResultSizeDataAccessException.class)
  public Mono<ResponseEntity<?>> handleIncorrectResultSizeDataAccessException(
      IncorrectResultSizeDataAccessException ex) {
    log.error("[Error] [IncorrectResultSizeDataAccessException] {}", ex.getMessage());
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        ResponseDTO.builder()
                            .status(ERROR)
                            .message("Data integrity error: unexpected number of records found")
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<?>> handleException(Exception ex) {
    log.error("[Error] [Exception] {}", ex.getMessage());
    String message = ex.getMessage();
    HttpStatus statusCode;
    if (ex instanceof MethodNotAllowedException) {
      statusCode = HttpStatus.METHOD_NOT_ALLOWED;
    } else if (ex instanceof UnsupportedMediaTypeStatusException
        | ex instanceof ResponseStatusException) {
      statusCode = HttpStatus.BAD_REQUEST;
      message = "Payload is empty, please provide proper payload";
    } else {
      statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
      // Sanitize message to prevent leaking internal details (SQL queries, stack traces, etc.)
      message = sanitizeErrorMessage(message);
    }
    String finalMessage = message;
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(statusCode)
                    .body(
                        ResponseDTO.builder()
                            .status(ERROR)
                            .message(finalMessage)
                            .data(parseClientResponse(sanitizeErrorMessage(ex.getMessage())))
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  @ExceptionHandler({WebExchangeBindException.class, IllegalArgumentException.class})
  public Mono<ResponseEntity<?>> handleNotValidException(Exception ex) {
    log.error("[Error] [WebExchangeBindException] {}", ex.getMessage());
    List<Map<String, String>> errorsList = new ArrayList<>();

    if (ex instanceof WebExchangeBindException webExchangeBindException) {
      webExchangeBindException
          .getAllErrors()
          .forEach(
              err -> {
                Map<String, String> errorsMap = new HashMap<>();
                errorsMap.put("developerMessage", err.getDefaultMessage());
                errorsMap.put("defaultUserMessage", err.getDefaultMessage());
                errorsList.add(errorsMap);
              });
    } else if (ex instanceof IllegalArgumentException illegalArgumentException) {
      Map<String, String> errorsMap = new HashMap<>();
      errorsMap.put("developerMessage", illegalArgumentException.getMessage());
      errorsMap.put("defaultUserMessage", illegalArgumentException.getMessage());
      errorsList.add(errorsMap);
    }

    Map<String, Object> result = new HashMap<>();
    result.put("errors", errorsList);

    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(
                        ResponseDTO.builder()
                            .status(FAIL)
                            .message("validations failed, please provide proper input")
                            .traceId(context.get(TRACE_ID))
                            .data(parseClientResponse(result))
                            .build())));
  }

  @ExceptionHandler(ServerWebInputException.class)
  public Mono<ResponseEntity<?>> handleServerWebInputException(ServerWebInputException ex) {
    log.error("[Error] [ServerWebInputException] {}", ex.getMessage());
    String message = "Payload is empty, please provide proper payload";
    HttpStatus statusCode = HttpStatus.BAD_REQUEST;
    // JsonMappingException is wrapped inside
    // ServerWebInputException, and we need to handle it
    // separately for better error message
    if (ex.getCause() instanceof DecodingException
        && ex.getCause().getCause() instanceof JsonMappingException) {
      return handleJsonMappingException((JsonMappingException) ex.getCause().getCause());
    }

    // validations for partnerId header miss
    if (ex instanceof MissingRequestValueException missingRequestValueException
        && missingRequestValueException.getLabel().equals("header")
        && missingRequestValueException.getName().equals(PARTNER_ID)) {
      message = "partnerId header missing, please provide proper headers";
    }

    String finalErrorMessage = message;
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(statusCode)
                    .body(
                        ResponseDTO.builder()
                            .status(ERROR)
                            .message(finalErrorMessage)
                            .data(parseClientResponse("Bad request"))
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  @ExceptionHandler(PanBasedLoanRejectionException.class)
  public Mono<ResponseEntity<?>> handleNotFoundException(PanBasedLoanRejectionException ex) {
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(
                        ResponseDTO.builder()
                            .status(FAIL)
                            .message(ex.getMessage())
                            .data(parseClientResponse(ex.getClientResponse()))
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  @ExceptionHandler(CustomTimeoutException.class)
  public Mono<ResponseEntity<?>> handleTimeoutException(CustomTimeoutException ex) {
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(
                        ResponseDTO.builder()
                            .status(SERVER_ERROR)
                            .message(ex.getMessage())
                            .traceId(context.get(TRACE_ID))
                            .data(parseClientResponse(ex.getClientResponse()))
                            .build())));
  }

  @ExceptionHandler(JsonMappingException.class)
  public Mono<ResponseEntity<?>> handleJsonMappingException(JsonMappingException ex) {
    log.error("[Error] [JsonMappingException] {}", ex.getMessage());
    HttpStatus statusCode = HttpStatus.BAD_REQUEST;

    // Extract field names from the exception
    List<String> fieldNames =
        ex.getPath().stream().map(JsonMappingException.Reference::getFieldName).toList();

    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(statusCode)
                    .body(
                        ResponseDTO.builder()
                            .status(ERROR)
                            .message(ex.getMessage())
                            .data(
                                parseClientResponse(
                                    "Incorrect fields: " + joinJsonFieldPath(fieldNames)))
                            .traceId(context.get(TRACE_ID))
                            .build())));
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
    } catch (JsonProcessingException ignored) {
      // Fall through to next attempt
      log.debug("Failed to parse response as M2pErrorResponseDTO");
    }

    try {
      return OBJECT_MAPPER.readValue(responseStr, Map.class);
    } catch (JsonProcessingException ignored) {
      log.debug("Failed to parse response as Map");
    }

    M2pErrorResponseDTO fallback = new M2pErrorResponseDTO();
    fallback.setDeveloperMessage(responseStr);
    fallback.setDefaultUserMessage(responseStr);
    return fallback;
  }

  private String joinJsonFieldPath(List<String> fieldNames) {
    StringBuilder result = new StringBuilder();
    for (String fieldName : fieldNames) {
      result.append(Objects.requireNonNullElse(fieldName, "."));
    }
    return result.toString();
  }

  public static String extractMessage(String rawMessage) {
    if (rawMessage == null || rawMessage.isBlank()) {
      return "[ERROR] Unknown validation error";
    }
    int colonIndex = rawMessage.indexOf(":");
    if (colonIndex != -1 && colonIndex + 1 < rawMessage.length()) {
      return rawMessage.substring(colonIndex + 1).trim();
    }
    return rawMessage.trim();
  }

  /**
   * Sanitizes error messages to prevent leaking sensitive internal details such as SQL queries,
   * database schema information, or stack traces.
   *
   * @param message the original error message
   * @return a sanitized message safe to return to clients
   */
  private static String sanitizeErrorMessage(String message) {
    if (message == null || message.isBlank()) {
      return "An unexpected error occurred";
    }

    // Check for SQL-related content that should not be exposed
    String lowerMessage = message.toLowerCase();
    if (lowerMessage.contains("select ")
        || lowerMessage.contains("insert ")
        || lowerMessage.contains("update ")
        || lowerMessage.contains("delete ")
        || lowerMessage.contains("from ")
        || lowerMessage.contains(" where ")
        || lowerMessage.contains("query [")
        || lowerMessage.contains("sql")
        || lowerMessage.contains("jdbc")
        || lowerMessage.contains("r2dbc")
        || lowerMessage.contains("database")
        || lowerMessage.contains("postgres")
        || lowerMessage.contains("mysql")
        || lowerMessage.contains("non unique result")
        || lowerMessage.contains("incorrect result size")) {
      return "An unexpected error occurred while processing your request";
    }

    return message;
  }
}
