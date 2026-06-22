package com.trillionloans.customer_portal.api.external;

import static java.util.Objects.requireNonNull;

import com.trillionloans.customer_portal.api.WebClientFactory;
import com.trillionloans.customer_portal.api.WebClientFactoryImpl;
import com.trillionloans.customer_portal.constant.StringConstants;
import com.trillionloans.customer_portal.model.dto.CategoryListResponse;
import com.trillionloans.customer_portal.model.dto.FreshdeskCategoryFieldResponse;
import com.trillionloans.customer_portal.model.dto.FreshdeskTicketResponse;
import com.trillionloans.customer_portal.model.dto.SubmitFormRequest;
import com.trillionloans.customer_portal.model.internal.WebClientParameters;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FreshdeskApi {
  private final WebClientFactory webClientFactory;
  private final Environment environment;
  private final String categoryFieldEndpoint;

  public FreshdeskApi(
      @Value("${freshdesk.api.base-url}") String baseUrl,
      @Value("${freshdesk.api.ticket-category-field-endpoint}") String categoryFieldEndpoint,
      Environment env) {
    this.webClientFactory = new WebClientFactoryImpl(baseUrl, "freshdesk", env, String.class);
    this.environment = env;
    this.categoryFieldEndpoint = categoryFieldEndpoint;
  }

  private MultipartBodyBuilder buildTicketRequestBody(SubmitFormRequest request) {
    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();

    // Required fields
    bodyBuilder.part("email", request.getEmail());
    bodyBuilder.part(
        "subject",
        request.getConcernCategory() != null
            ? request.getConcernCategory()
            : "Customer Support Request");
    bodyBuilder.part("description", request.getDescription());

    // Freshdesk mandatory fields
    bodyBuilder.part("status", StringConstants.Freshdesk.STATUS_OPEN);

    // Custom fields mapping
    if (request.getConcernCategory() != null) {
      bodyBuilder.part("custom_fields[cf_category]", request.getConcernCategory());
    }
    if (request.getPanCard() != null) {
      bodyBuilder.part("custom_fields[cf_pan_card]", request.getPanCard());
    }
    if (request.getRegisteredMobileNumber() != null) {
      bodyBuilder.part(
          "custom_fields[cf_customer_contact_number]", request.getRegisteredMobileNumber());
    }
    if (request.getLoanId() != null) {
      bodyBuilder.part("custom_fields[cf_loan_id]", request.getLoanId());
    }

    // Attachments handling
    if (request.getAttachments() != null) {
      for (SubmitFormRequest.Attachment attachment : request.getAttachments()) {
        byte[] fileBytes = Base64.getDecoder().decode(attachment.getFileContent());
        ByteArrayResource resource =
            new ByteArrayResource(fileBytes) {
              @Override
              public String getFilename() {
                return attachment.getFileName();
              }
            };
        bodyBuilder
            .part("attachments[]", resource)
            .header(
                "Content-Disposition",
                "form-data; name=\"attachments[]\"; filename=\"" + attachment.getFileName() + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM);
      }
    }

    return bodyBuilder;
  }

  public Mono<FreshdeskTicketResponse> createTicket(SubmitFormRequest request) {
    MultipartBodyBuilder bodyBuilder = buildTicketRequestBody(request);

    String endpoint =
        requireNonNull(environment.getProperty("freshdesk.api.create-ticket.endpoint"));
    WebClientParameters params =
        new WebClientParameters("CREATE_TICKET", "freshdesk", 0, false, true, false);

    return webClientFactory.postMultipart(
        endpoint, bodyBuilder, getFreshdeskHeaders(), FreshdeskTicketResponse.class, params);
  }

  public Mono<CategoryListResponse> getTopLevelCategories() {
    WebClientParameters params =
        new WebClientParameters("GET_CATEGORIES", "freshdesk", 0, false, true, false);

    return webClientFactory
        .getData(
            categoryFieldEndpoint,
            getFreshdeskHeaders(),
            FreshdeskCategoryFieldResponse.class,
            params)
        .map(
            response -> {
              List<FreshdeskCategoryFieldResponse.Choice> choices = response.getChoices();
              List<String> categories =
                  choices == null
                      ? List.of()
                      : choices.stream()
                          .map(FreshdeskCategoryFieldResponse.Choice::getLabel)
                          .toList();
              return new CategoryListResponse(categories);
            });
  }

  private HttpHeaders getFreshdeskHeaders() {
    HttpHeaders headers = new HttpHeaders();
    String apiKey = requireNonNull(environment.getProperty("freshdesk.api.key"));
    String authHeaderValue = Base64.getEncoder().encodeToString((apiKey + ":X").getBytes());
    headers.add("Authorization", "Basic " + authHeaderValue);
    return headers;
  }
}
