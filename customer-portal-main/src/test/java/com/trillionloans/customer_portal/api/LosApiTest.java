package com.trillionloans.customer_portal.api;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.atLeast;

import com.trillionloans.customer_portal.api.internal.LosApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

@ContextConfiguration(classes = {LosApi.class, String.class})
@ExtendWith(SpringExtension.class)
@DisabledInAotMode
public class LosApiTest {

  @MockBean private Environment environment;
  @MockBean private LosApi losApi;

  @Test
  void testFetchLeadDetails() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new LosApi("https://example.org/example", env).fetchLeadDetails("33")));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }

  @Test
  void testFetchLeadDetailAgainstMobileNumber() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new LosApi("https://example.org/example", env)
                .fetchLeadDetailAgainstMobileNumber("33")));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }

  @Test
  void testFetchLeadDetailAgainstMobileNumberAndDOB() {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new LosApi("https://example.org/example", env)
                .fetchLeadDetailAgainstMobileNumberAndDOB("33", "12")));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }

  @Test
  void testFetchLeadDetailAgainstMobileNumberAndDOBAndPAN() {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new LosApi("https://example.org/example", env)
                .fetchLeadDetailAgainstMobileNumberDOBAndPAN("33", "12", "22")));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }

  @Test
  void testFetchDocumentAgainstId() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new LosApi("https://example.org/example", env).fetchDocumentAgainstId("33", "33")));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }

  @Test
  void testFetchAllDocumentDetailsAgainstLoanAppId() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new LosApi("https://example.org/example", env)
                .fetchAllDocumentDetailsAgainstLoanAppId("33")));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }

  @Test
  void testGetCpRpsLeadData() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new LosApi("https://example.org/example", env).getCpRpsLeadData("33", "33")));

    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }
}
