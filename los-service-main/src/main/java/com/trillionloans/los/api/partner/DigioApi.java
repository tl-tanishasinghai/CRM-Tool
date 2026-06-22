package com.trillionloans.los.api.partner;

import static com.trillionloans.los.constant.StringConstants.DIGIO_AML_PEP_CHECK;
import static com.trillionloans.los.constant.StringConstants.MANDATE_REGISTRATION_DETAILS_LOG_HEADER;
import static com.trillionloans.los.constant.StringConstants.MANDATE_REGISTRATION_LOG_HEADER;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.trillionloans.los.api.WebClientFactory;
import com.trillionloans.los.api.WebClientFactoryImpl;
import com.trillionloans.los.constant.Event;
import com.trillionloans.los.model.dto.internal.EventContext;
import com.trillionloans.los.model.dto.internal.WebClientParameters;
import com.trillionloans.los.model.request.digio.AmlPepCheckRequest;
import com.trillionloans.los.model.request.digio.DigioSignDocumentRequest;
import com.trillionloans.los.model.request.digio.DigioUploadPdfRequest;
import com.trillionloans.los.model.request.digio.MandateRegistrationDigioRequest;
import com.trillionloans.los.model.response.digio.AmlPepCheckResponse;
import com.trillionloans.los.model.response.digio.DigioSignDocumentResponse;
import com.trillionloans.los.model.response.digio.DigioUploadPdfResponse;
import com.trillionloans.los.model.response.digio.MandateDetailsDigioResponse;
import com.trillionloans.los.model.response.digio.MandateLiveBanksDigioResponse;
import com.trillionloans.los.model.response.digio.MandateRegistrationDigioResponse;
import com.trillionloans.los.service.producers.KafkaEventProducerService;
import com.trillionloans.los.service.producers.KafkaLoggingService;
import com.trillionloans.los.util.WebClientUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class DigioApi {
  private static final String ESIGNING_LOG_HEADER = "[ESIGNING]";

  private final WebClientFactory webClientFactory;
  private final WebClientFactory externalSignerWebClientFactory;
  private final Environment environment;
  private final WebClientUtil webClientUtil;
  private final String authToken;
  private final String amlPepAuthToken;

  private static final String CONTENT_TYPE = "Content-Type";

  public DigioApi(
      @Value("${digio.api.base-url}") String baseUrl,
      @Value("${digio.external-signer.base-url}") String externalSignerBaseUrl,
      @Value("${digio.auth.token}") String authToken,
      @Value("${digio.auth.aml-pep-token}") String amlPepAuthToken,
      @Value("${digio.ssl.verify-enabled:false}") boolean sslVerifyEnabled,
      KafkaLoggingService kafkaLoggingService,
      KafkaEventProducerService kafkaEventProducerService,
      Environment env) {
    boolean disableSslVerification = !sslVerifyEnabled;
    this.webClientFactory =
        new WebClientFactoryImpl(
            baseUrl,
            "Digio",
            env,
            kafkaLoggingService,
            kafkaEventProducerService,
            disableSslVerification);
    this.externalSignerWebClientFactory =
        new WebClientFactoryImpl(
            externalSignerBaseUrl,
            "DigioExternalSigner",
            env,
            kafkaLoggingService,
            kafkaEventProducerService,
            disableSslVerification);
    this.authToken = authToken;
    this.amlPepAuthToken = amlPepAuthToken;
    this.environment = env;
    this.webClientUtil = new WebClientUtil();
  }

  public Mono<MandateRegistrationDigioResponse> createMandateRegistration(
      MandateRegistrationDigioRequest mandateRegistrationDigioRequest,
      String loanApplicationId,
      String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("digio.api.mandate-registration")))
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.MANDATE_REGISTRATION, loanApplicationId, clientId);
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(
            null, MANDATE_REGISTRATION_LOG_HEADER, 0, true, true, eventContext);
    return webClientFactory.postDataWithoutStringSerialization(
        uri,
        mandateRegistrationDigioRequest,
        getHeaders(),
        MandateRegistrationDigioResponse.class,
        webClientParameters);
  }

  public Mono<MandateDetailsDigioResponse> fetchMandateRegistrationStatus(
      String mandateId, String loanApplicationId, String clientId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("digio.api.fetch-mandate")))
            .buildAndExpand(mandateId)
            .toUriString();
    EventContext eventContext =
        new EventContext(Event.MANDATE_REGISTRATION_DETAILS, loanApplicationId, clientId);
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(
            null, MANDATE_REGISTRATION_DETAILS_LOG_HEADER, 3, true, true, eventContext);
    return webClientFactory.getData(
        uri, getHeaders(), MandateDetailsDigioResponse.class, webClientParameters);
  }

  public Flux<MandateLiveBanksDigioResponse> fetchDigioMandateLiveBanks(String mode) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("digio.api.live-banks")))
            .queryParam("mode", mode)
            .toUriString();
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(
            null, MANDATE_REGISTRATION_DETAILS_LOG_HEADER, 3, true, true, null);
    return webClientFactory.getFluxDataWithTypeRef(
        uri,
        getHeaders(),
        new ParameterizedTypeReference<MandateLiveBanksDigioResponse>() {},
        webClientParameters);
  }

  private static final Gson GSON =
      new GsonBuilder().disableHtmlEscaping().serializeNulls().create();

  public Mono<DigioUploadPdfResponse> uploadPdfDocument(DigioUploadPdfRequest request) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("digio.api.upload-pdf")))
            .toUriString();
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(
            null, ESIGNING_LOG_HEADER + "[UPLOAD_PDF]", 0, true, true, null);
    Map<String, Object> requestBody = toUploadPdfRequestBody(request);
    return webClientFactory
        .postDataWithoutStringSerialization(
            uri, requestBody, getHeaders(), String.class, webClientParameters)
        .map(json -> GSON.fromJson(json, DigioUploadPdfResponse.class));
  }

  public Mono<DigioSignDocumentResponse> signDocument(DigioSignDocumentRequest request) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(
                    environment.getProperty("digio.external-signer.sign-document")))
            .toUriString();
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(
            null, ESIGNING_LOG_HEADER + "[SIGN_DOCUMENT]", 0, true, true, null);
    Map<String, Object> requestBody = toSignDocumentRequestBody(request);
    return externalSignerWebClientFactory
        .postDataWithoutStringSerialization(
            uri, requestBody, getHeaders(), String.class, webClientParameters)
        .map(json -> GSON.fromJson(json, DigioSignDocumentResponse.class));
  }

  private Map<String, Object> toUploadPdfRequestBody(DigioUploadPdfRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("signers", toSignersList(request.getSigners()));
    body.put("expire_in_days", request.getExpireInDays());
    body.put("display_on_page", request.getDisplayOnPage());
    body.put("send_sign_link", request.getSendSignLink());
    body.put("notify_signers", request.getNotifySigners());
    body.put("file_name", request.getFileName());
    body.put("file_data", request.getFileData());
    return body;
  }

  private List<Map<String, Object>> toSignersList(List<DigioUploadPdfRequest.Signer> signers) {
    if (signers == null) return List.of();
    return signers.stream()
        .map(
            s -> {
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("identifier", s.getIdentifier());
              m.put("name", s.getName());
              m.put("reason", s.getReason());
              m.put("sign_type", s.getSignType());
              return m;
            })
        .collect(Collectors.toList());
  }

  private Map<String, Object> toSignDocumentRequestBody(DigioSignDocumentRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("identifier", request.getIdentifier());
    body.put("document_id", request.getDocumentId());
    body.put("reason", request.getReason());
    body.put("key_store_name", request.getKeyStoreName());
    return body;
  }

  public Mono<AmlPepCheckResponse> performAmlPepScreening(AmlPepCheckRequest request) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("digio.api.aml-pep-screening")))
            .toUriString();

    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(null, DIGIO_AML_PEP_CHECK, 3, true, true, null);
    HttpHeaders headers = getHeaders(amlPepAuthToken);
    headers.setContentType(MediaType.APPLICATION_JSON);

    return webClientFactory.postDataWithoutStringSerialization(
        uri, request, headers, AmlPepCheckResponse.class, webClientParameters);
  }

  public Mono<byte[]> downloadDocument(String documentId) {
    String uri =
        UriComponentsBuilder.fromUriString(
                Objects.requireNonNull(environment.getProperty("digio.api.download-document")))
            .queryParam("document_id", documentId)
            .toUriString();
    WebClientParameters webClientParameters =
        webClientUtil.getWebClientParameters(
            null, ESIGNING_LOG_HEADER + "[DOWNLOAD_DOCUMENT]", 0, true, false, null);
    return webClientFactory.getData(uri, getHeaders(), byte[].class, webClientParameters);
  }

  private HttpHeaders getHeaders(String token) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add("Authorization", "Basic " + token);
    return headers;
  }

  private HttpHeaders getHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.add(CONTENT_TYPE, "application/json");
    headers.add("Authorization", "Basic " + authToken);
    return headers;
  }
}
