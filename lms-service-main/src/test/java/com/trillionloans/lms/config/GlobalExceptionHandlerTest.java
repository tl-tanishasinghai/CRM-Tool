package com.trillionloans.lms.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.trillionloans.lms.exception.ClientSideException;
import com.trillionloans.lms.exception.GlobalExceptionHandler;
import com.trillionloans.lms.exception.NotFoundException;
import com.trillionloans.lms.exception.ServerErrorException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.UnsupportedMediaTypeException;

@ContextConfiguration(classes = {GlobalExceptionHandler.class})
@ExtendWith(SpringExtension.class)
class GlobalExceptionHandlerTest {
  @Autowired private GlobalExceptionHandler globalExceptionHandler;

  /**
   * Method under test: {@link GlobalExceptionHandler#handleNotFoundException(NotFoundException)}
   */
  @Test
  void testHandleNotFoundException() {
    ResponseEntity<?> responseEntity =
        globalExceptionHandler.handleNotFoundException(
            new NotFoundException("An error occurred", "sample"));
    assert responseEntity != null;
    assert responseEntity.getStatusCode().is4xxClientError();
    assert responseEntity.getBody() != null;
    assert responseEntity.getBody().toString().contains("An error occurred");
  }

  /**
   * Method under test: {@link
   * GlobalExceptionHandler#handleServerErrorException(ServerErrorException)}
   */
  @Test
  void testHandleServerErrorException() {
    ResponseEntity<?> responseEntity =
        globalExceptionHandler.handleServerErrorException(
            new ServerErrorException(
                "An error occurred",
                "Client Response",
                HttpStatusCode.valueOf(500),
                "some message"));
    responseEntity.getStatusCode().is5xxServerError();
    assert responseEntity.getBody() != null;
    assert responseEntity.getBody().toString().contains("An error occurred");
    assert responseEntity.getBody().toString().contains("Client Response");
  }

  /**
   * Method under test: {@link
   * GlobalExceptionHandler#handleClientSideException(ClientSideException)}
   */
  @Test
  void testHandleClientSideException() {
    ResponseEntity<?> responseEntity =
        globalExceptionHandler.handleClientSideException(
            new ClientSideException(
                "An error occurred",
                "Client Response",
                HttpStatusCode.valueOf(400),
                "some message"));
    assert responseEntity != null;
    assert responseEntity.getStatusCode().is4xxClientError();
    assert responseEntity.getBody() != null;
    assert responseEntity.getBody().toString().contains("An error occurred");
    assert responseEntity.getBody().toString().contains("Client Response");
  }

  /**
   * Method under test: {@link
   * GlobalExceptionHandler#handleUnsupportedMediaTypeException(UnsupportedMediaTypeException)}
   */
  @Test
  void testHandleUnsupportedMediaTypeException() {

    ResponseEntity<?> responseEntity =
        globalExceptionHandler.handleUnsupportedMediaTypeException(
            new UnsupportedMediaTypeException("Just cause"));
    assert responseEntity != null;
    assert responseEntity.getStatusCode().is4xxClientError();
  }

  /** Method under test: {@link GlobalExceptionHandler#handleException(Exception)} */
  @Test
  void testHandleException() {
    ResponseEntity<?> responseEntity = globalExceptionHandler.handleException(new Exception("foo"));

    assert responseEntity != null;
    assert responseEntity.getStatusCode().is5xxServerError();
    assert responseEntity.getBody() != null;
    assert responseEntity.getBody().toString().contains("foo");
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
