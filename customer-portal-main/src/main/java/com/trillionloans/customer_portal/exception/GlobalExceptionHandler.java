package com.trillionloans.customer_portal.exception;

import static com.trillionloans.customer_portal.constant.ResponseStatus.ERROR;
import static com.trillionloans.customer_portal.constant.ResponseStatus.FAIL;
import static com.trillionloans.customer_portal.constant.ResponseStatus.SERVER_ERROR;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.trillionloans.customer_portal.model.response.ResponseDTO;
import com.trillionloans.customer_portal.util.CommonUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.codec.DecodingException;
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

  @ExceptionHandler(NotFoundException.class)
  public Mono<ResponseEntity<?>> handleNotFoundException(NotFoundException ex) {
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(
                        ResponseDTO.builder()
                            .status(FAIL)
                            .message(ex.getReason())
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
                            .build())));
  }

  @ExceptionHandler(UnsupportedMediaTypeException.class)
  public Mono<ResponseEntity<?>> handleUnsupportedMediaTypeException(
      UnsupportedMediaTypeException ex) {
    log.error("[Error] [UnsupportedMediaTypeException] {}", ex.getMessage());
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        ResponseDTO.builder()
                            .status(SERVER_ERROR)
                            .message(ex.getMessage())
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<?>> handleException(Exception ex) {
    log.error("[Error] [Exception] {}", ex.getMessage());
    HttpStatus statusCode;
    if (ex instanceof MethodNotAllowedException) {
      statusCode = HttpStatus.METHOD_NOT_ALLOWED;
    } else if (ex instanceof UnsupportedMediaTypeStatusException) {
      statusCode = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
    } else if (ex instanceof ResponseStatusException) {
      statusCode = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
    } else {
      statusCode = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(statusCode)
                    .body(
                        ResponseDTO.builder()
                            .status(ERROR)
                            .message(ex.getMessage())
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  @ExceptionHandler(CustomisedException.class)
  public Mono<ResponseEntity<?>> handleCustomisedException(CustomisedException ex) {
    log.error("[CustomisedException] {} | LoggerHeader: {}", ex.getMessage(), ex.getLoggerHeader());

    HttpStatus status =
        CommonUtil.nullOrEmpty(ex.getHttpStatus()) ? HttpStatus.BAD_REQUEST : ex.getHttpStatus();

    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(status)
                    .body(
                        ResponseDTO.builder()
                            .status(FAIL)
                            .message(ex.getMessage())
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  @ExceptionHandler(DownstreamServiceException.class)
  public Mono<ResponseEntity<?>> handleDownstreamServiceException(DownstreamServiceException ex) {
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        ResponseDTO.builder()
                            .status(FAIL)
                            .message(ex.getMessage())
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  @ExceptionHandler(UnexpectedServiceException.class)
  public Mono<ResponseEntity<?>> handleUnexpectedServiceException(UnexpectedServiceException ex) {
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        ResponseDTO.builder()
                            .status(ERROR)
                            .message(ex.getMessage())
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
                errorsMap.put("developerMessage", err.getObjectName());
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
                            .data(errorsList)
                            .traceId(context.get(TRACE_ID))
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
        && missingRequestValueException.getLabel().equals("header")) {
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
                            .traceId(context.get(TRACE_ID))
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
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  @ExceptionHandler(JsonProcessingFailureException.class)
  public Mono<ResponseEntity<?>> handleUnhandledServerException(JsonProcessingFailureException ex) {
    return Mono.deferContextual(
        context ->
            Mono.just(
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                        ResponseDTO.builder()
                            .status(FAIL)
                            .message(ex.getMessage())
                            .traceId(context.get(TRACE_ID))
                            .build())));
  }

  private String joinJsonFieldPath(List<String> fieldNames) {
    StringBuilder result = new StringBuilder();
    for (String fieldName : fieldNames) {
      result.append(Objects.requireNonNullElse(fieldName, "."));
    }
    return result.toString();
  }
}
