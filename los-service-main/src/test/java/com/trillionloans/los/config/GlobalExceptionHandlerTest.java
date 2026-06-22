package com.trillionloans.los.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.trillionloans.los.exception.BaseException;
import com.trillionloans.los.exception.ClientSideException;
import com.trillionloans.los.exception.ForbiddenException;
import com.trillionloans.los.exception.NotFoundException;
import com.trillionloans.los.exception.ServerErrorException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;
import reactor.core.publisher.Mono;

@ContextConfiguration(classes = {GlobalExceptionHandler.class})
@ExtendWith(SpringExtension.class)
class GlobalExceptionHandlerTest {
  @Autowired private GlobalExceptionHandler globalExceptionHandler;

  /**
   * Method under test: {@link GlobalExceptionHandler#handleNotFoundException(NotFoundException)}
   */
  @Test
  void testHandleNotFoundException() {
    Mono<ResponseEntity<?>> responseEntity =
        globalExceptionHandler.handleNotFoundException(new NotFoundException("An error occurred"));
    responseEntity.subscribe(
        re -> {
          assert re != null;
          assert re.getStatusCode().is4xxClientError();
          assert re.getBody() != null;
          assert re.getBody().toString().contains("An error occurred");
        });
  }

  /**
   * Method under test: {@link
   * GlobalExceptionHandler#handleServerErrorException(ServerErrorException)}
   */
  @Test
  void testHandleServerErrorException() {
    Mono<ResponseEntity<?>> responseEntity =
        globalExceptionHandler.handleServerErrorException(
            new ServerErrorException("An error occurred", "Client Response", null));
    responseEntity.subscribe(
        re -> {
          assert re != null;
          assert re.getStatusCode().is5xxServerError();
          assert re.getBody() != null;
          assert re.getBody().toString().contains("An error occurred");
          assert re.getBody().toString().contains("Client Response");
        });
  }

  /**
   * Method under test: {@link
   * GlobalExceptionHandler#handleClientSideException(ClientSideException)}
   */
  @Test
  void testHandleClientSideException() {
    Mono<ResponseEntity<?>> responseEntity =
        globalExceptionHandler.handleClientSideException(
            new ClientSideException("An error occurred", "Client Response", null));
    responseEntity.subscribe(
        re -> {
          assert re != null;
          assert re.getStatusCode().is4xxClientError();
          assert re.getBody() != null;
          assert re.getBody().toString().contains("An error occurred");
          assert re.getBody().toString().contains("Client Response");
        });
  }

  /**
   * Method under test: {@link GlobalExceptionHandler#handleForbiddenException(ForbiddenException)}
   */
  @Test
  void testHandleForbiddenException() {
    Mono<ResponseEntity<?>> responseEntity =
        globalExceptionHandler.handleForbiddenException(
            new ForbiddenException("An error occurred", "Client Response", null));
    responseEntity.subscribe(
        re -> {
          assert re != null;
          assert re.getStatusCode().is4xxClientError();
          assert re.getBody() != null;
          assert re.getBody().toString().contains("An error occurred");
        });
  }

  /** Method under test: {@link GlobalExceptionHandler#handleBaseException(BaseException)} */
  @Test
  void testHandleBaseException() {
    Mono<ResponseEntity<?>> responseEntity =
        globalExceptionHandler.handleBaseException(
            new BaseException("An error occurred", "Client Response", null));
    responseEntity.subscribe(
        re -> {
          assert re != null;
          assert re.getStatusCode().is4xxClientError();
          assert re.getBody() != null;
          assert re.getBody().toString().contains("An error occurred");
          assert re.getBody().toString().contains("Client Response");
        });
  }

  /**
   * Method under test: {@link
   * GlobalExceptionHandler#handleUnsupportedMediaTypeException(UnsupportedMediaTypeException)}
   */
  @Test
  void testHandleUnsupportedMediaTypeException() {

    Mono<ResponseEntity<?>> responseEntity =
        globalExceptionHandler.handleUnsupportedMediaTypeException(
            new UnsupportedMediaTypeException("Just cause"));
    responseEntity.subscribe(
        re -> {
          assert re != null;
          assert re.getStatusCode().is4xxClientError();
        });
  }

  /** Method under test: {@link GlobalExceptionHandler#handleException(Exception)} */
  @Test
  void testHandleException() {
    Mono<ResponseEntity<?>> responseEntity =
        globalExceptionHandler.handleException(new Exception("foo"));
    responseEntity.subscribe(
        re -> {
          assert re != null;
          assert re.getStatusCode().is5xxServerError();
          assert re.getBody() != null;
          assert re.getBody().toString().contains("foo");
        });
  }

  /**
   * Method under test: {@link
   * GlobalExceptionHandler#handleJsonMappingException(JsonMappingException)}
   */
  @Test
  void testHandleJsonMappingException() {
    JsonMappingException ex = mock(JsonMappingException.class);
    when(ex.getPath()).thenReturn(new ArrayList<>());

    globalExceptionHandler.handleJsonMappingException(ex);

    verify(ex).getPath();
  }
}
