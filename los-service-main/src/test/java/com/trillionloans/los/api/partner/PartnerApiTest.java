package com.trillionloans.los.api.partner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trillionloans.los.service.producers.KafkaLoggingService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.DefaultPropertiesPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ContextConfiguration(classes = {PartnerApi.class})
@ExtendWith(SpringExtension.class)
@DisabledInAotMode
class PartnerApiTest {
  @MockBean private Environment environment;

  @MockBean private PartnerApi partnerApi;
  @MockBean private KafkaLoggingService kafkaLoggingService;

  /**
   * Method under test: {@link PartnerApi#registerAadhaarXmlCallback(Object, String, String, String,
   * Integer)}
   */
  @Test
  void testRegisterAadhaarXmlCallback() throws AssertionError {
    Mono<Object> justResult = Mono.just("Data");
    when(partnerApi.registerAadhaarXmlCallback(
            Mockito.<Object>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<String>any(),
            Mockito.<Integer>any()))
        .thenReturn(justResult);

    Mono<Object> actualPublisher =
        partnerApi.registerAadhaarXmlCallback(
            "Request Body", "Uri", "Call Method", "Partner Code", 3);

    verify(partnerApi)
        .registerAadhaarXmlCallback(
            isA(Object.class), eq("Uri"), eq("Call Method"), eq("Partner Code"), eq(3));
    assertSame(justResult, actualPublisher);
    StepVerifier.FirstStep<Object> createResult = StepVerifier.create(actualPublisher);
    createResult
        .assertNext(
            o -> {
              assertEquals("Data", o);
              return;
            })
        .expectComplete()
        .verify();
  }

  /**
   * Method under test: {@link PartnerApi#registerDisbursementCallback(Object, String, String,
   * String, Integer)}
   */
  @Test
  void testRegisterDisbursementCallback() throws AssertionError {
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(new MutablePropertySources());
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerDisbursementCallback(
                    "Request Body", "Uri", "Call Method", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
  }

  /**
   * Method under test: {@link PartnerApi#registerDisbursementCallback(Object, String, String,
   * String, Integer)}
   */
  @Test
  void testRegisterDisbursementCallback2() throws AssertionError {
    MutablePropertySources mutablePropertySources = new MutablePropertySources();
    mutablePropertySources.addFirst(new DefaultPropertiesPropertySource(new HashMap<>()));
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerDisbursementCallback(
                    "Request Body", "Uri", "Call Method", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
  }

  /**
   * Method under test: {@link PartnerApi#registerDisbursementCallback(Object, String, String,
   * String, Integer)}
   */
  @Test
  void testRegisterDisbursementCallback3() throws AssertionError {
    MutablePropertySources mutablePropertySources = mock(MutablePropertySources.class);

    ArrayList<PropertySource<?>> propertySourceList = new ArrayList<>();
    Mockito.<Iterator<PropertySource<?>>>when(mutablePropertySources.iterator())
        .thenReturn(propertySourceList.iterator());
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerDisbursementCallback("Request Body", "Uri", "POST", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
    verify(mutablePropertySources).iterator();
  }

  /**
   * Method under test: {@link PartnerApi#registerDisbursementCallback(Object, String, String,
   * String, Integer)}
   */
  @Test
  void testRegisterDisbursementCallback4() throws AssertionError {
    MutablePropertySources mutablePropertySources = mock(MutablePropertySources.class);

    ArrayList<PropertySource<?>> propertySourceList = new ArrayList<>();
    Mockito.<Iterator<PropertySource<?>>>when(mutablePropertySources.iterator())
        .thenReturn(propertySourceList.iterator());
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerDisbursementCallback("Request Body", "Uri", "PUT", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
    verify(mutablePropertySources).iterator();
  }

  /**
   * Method under test: {@link PartnerApi#registerDisbursementCallback(Object, String, String,
   * String, Integer)}
   */
  @Test
  void testRegisterDisbursementCallback5() throws AssertionError {
    MutablePropertySources mutablePropertySources = mock(MutablePropertySources.class);

    ArrayList<PropertySource<?>> propertySourceList = new ArrayList<>();
    Mockito.<Iterator<PropertySource<?>>>when(mutablePropertySources.iterator())
        .thenReturn(propertySourceList.iterator());
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");
    PartnerApi partnerApi = new PartnerApi(environment, kafkaLoggingService);

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            partnerApi.registerDisbursementCallback(
                new StandardEnvironment(), "Uri", "POST", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
    verify(mutablePropertySources).iterator();
  }

  /**
   * Method under test: {@link PartnerApi#registerDisbursementCallback(Object, String, String,
   * String, Integer)}
   */
  @Test
  void testRegisterDisbursementCallback6() throws AssertionError {
    MutablePropertySources mutablePropertySources = mock(MutablePropertySources.class);

    ArrayList<PropertySource<?>> propertySourceList = new ArrayList<>();
    Mockito.<Iterator<PropertySource<?>>>when(mutablePropertySources.iterator())
        .thenReturn(propertySourceList.iterator());
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    // Act and Assert
    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerDisbursementCallback(null, "Uri", "POST", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
    verify(mutablePropertySources).iterator();
  }

  /**
   * Method under test: {@link PartnerApi#registerDisbursementCallback(Object, String, String,
   * String, Integer)}
   */
  @Test
  void testRegisterDisbursementCallback7() throws AssertionError {
    MutablePropertySources mutablePropertySources = mock(MutablePropertySources.class);

    ArrayList<PropertySource<?>> propertySourceList = new ArrayList<>();
    Mockito.<Iterator<PropertySource<?>>>when(mutablePropertySources.iterator())
        .thenReturn(propertySourceList.iterator());
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerDisbursementCallback(null, "Uri", "PUT", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
    verify(mutablePropertySources).iterator();
  }

  /**
   * Method under test: {@link PartnerApi#registerESignCallback(Object, String, String, String,
   * Integer)}
   */
  @Test
  void testRegisterESignCallback() throws AssertionError {
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(new MutablePropertySources());
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerESignCallback("Request Body", "Uri", "Call Method", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
  }

  /**
   * Method under test: {@link PartnerApi#registerESignCallback(Object, String, String, String,
   * Integer)}
   */
  @Test
  void testRegisterESignCallback2() throws AssertionError {
    MutablePropertySources mutablePropertySources = new MutablePropertySources();
    mutablePropertySources.addFirst(new DefaultPropertiesPropertySource(new HashMap<>()));
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerESignCallback("Request Body", "Uri", "Call Method", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
  }

  /**
   * Method under test: {@link PartnerApi#registerESignCallback(Object, String, String, String,
   * Integer)}
   */
  @Test
  void testRegisterESignCallback3() throws AssertionError {
    MutablePropertySources mutablePropertySources = mock(MutablePropertySources.class);

    ArrayList<PropertySource<?>> propertySourceList = new ArrayList<>();
    Mockito.<Iterator<PropertySource<?>>>when(mutablePropertySources.iterator())
        .thenReturn(propertySourceList.iterator());
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerESignCallback("Request Body", "Uri", "POST", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
    verify(mutablePropertySources).iterator();
  }

  /**
   * Method under test: {@link PartnerApi#registerESignCallback(Object, String, String, String,
   * Integer)}
   */
  @Test
  void testRegisterESignCallback4() throws AssertionError {
    MutablePropertySources mutablePropertySources = mock(MutablePropertySources.class);

    ArrayList<PropertySource<?>> propertySourceList = new ArrayList<>();
    Mockito.<Iterator<PropertySource<?>>>when(mutablePropertySources.iterator())
        .thenReturn(propertySourceList.iterator());
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerESignCallback("Request Body", "Uri", "PUT", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
    verify(mutablePropertySources).iterator();
  }

  /**
   * Method under test: {@link PartnerApi#registerESignCallback(Object, String, String, String,
   * Integer)}
   */
  @Test
  void testRegisterESignCallback5() throws AssertionError {
    MutablePropertySources mutablePropertySources = mock(MutablePropertySources.class);

    ArrayList<PropertySource<?>> propertySourceList = new ArrayList<>();
    Mockito.<Iterator<PropertySource<?>>>when(mutablePropertySources.iterator())
        .thenReturn(propertySourceList.iterator());
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");
    PartnerApi partnerApi = new PartnerApi(environment, kafkaLoggingService);

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            partnerApi.registerESignCallback(
                new StandardEnvironment(), "Uri", "POST", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
    verify(mutablePropertySources).iterator();
  }

  /**
   * Method under test: {@link PartnerApi#registerESignCallback(Object, String, String, String,
   * Integer)}
   */
  @Test
  void testRegisterESignCallback6() throws AssertionError {
    MutablePropertySources mutablePropertySources = mock(MutablePropertySources.class);

    ArrayList<PropertySource<?>> propertySourceList = new ArrayList<>();
    Mockito.<Iterator<PropertySource<?>>>when(mutablePropertySources.iterator())
        .thenReturn(propertySourceList.iterator());
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerESignCallback(null, "Uri", "POST", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
    verify(mutablePropertySources).iterator();
  }

  /**
   * Method under test: {@link PartnerApi#registerESignCallback(Object, String, String, String,
   * Integer)}
   */
  @Test
  void testRegisterESignCallback7() throws AssertionError {
    MutablePropertySources mutablePropertySources = mock(MutablePropertySources.class);

    ArrayList<PropertySource<?>> propertySourceList = new ArrayList<>();
    Mockito.<Iterator<PropertySource<?>>>when(mutablePropertySources.iterator())
        .thenReturn(propertySourceList.iterator());
    StandardEnvironment environment = mock(StandardEnvironment.class);
    when(environment.getPropertySources()).thenReturn(mutablePropertySources);
    when(environment.getProperty(Mockito.<String>any())).thenReturn("42");

    StepVerifier.FirstStep<Object> createResult =
        StepVerifier.create(
            (new PartnerApi(environment, kafkaLoggingService))
                .registerESignCallback(null, "Uri", "PUT", "Partner Code", 3));
    createResult.expectError().verify();
    verify(environment, atLeast(1)).getProperty(Mockito.<String>any());
    verify(environment).getPropertySources();
    verify(mutablePropertySources).iterator();
  }
}
