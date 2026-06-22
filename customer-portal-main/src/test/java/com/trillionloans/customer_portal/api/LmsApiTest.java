package com.trillionloans.customer_portal.api;

import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.customer_portal.api.internal.LmsApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.test.StepVerifier;

@ContextConfiguration(classes = {LmsApi.class, String.class})
@ExtendWith(SpringExtension.class)
@DisabledInAotMode
public class LmsApiTest {

  @MockBean private LmsApi lmsApi;

  @Test
  void testFetchSOA() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create((new LmsApi("https://example.org/example", env).fetchSOA("33", "33")));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }

  @Test
  void testFetchNOC() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create((new LmsApi("https://example.org/example", env).fetchNOC("33")));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }

  @Test
  void testFetchTransactionDetails() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new LmsApi("https://example.org/example", env).fetchTransactionDetails("33")));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }

  @Test
  void testFetchAllLoanDetails() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");

    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(
            (new LmsApi("https://example.org/example", env).fetchAllLoansDetails("33")));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }

  @Test
  void testFetchRPS() throws AssertionError {
    StandardEnvironment env = mock(StandardEnvironment.class);
    when(env.getProperty(Mockito.any())).thenReturn("33");
    StepVerifier.FirstStep<?> createResult =
        StepVerifier.create(new LmsApi("https://example.org/example", env).fetchRPS("33"));
    createResult.expectError().verify();
    verify(env, atLeast(1)).getProperty(Mockito.any());
  }
}
