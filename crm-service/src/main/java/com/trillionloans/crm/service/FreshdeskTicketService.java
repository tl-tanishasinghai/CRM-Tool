package com.trillionloans.crm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.trillionloans.crm.model.CrmModels.CustomerProfile;
import com.trillionloans.crm.model.CrmModels.FreshdeskAgentTicket;
import com.trillionloans.crm.model.CrmModels.FreshdeskConversationEntry;
import com.trillionloans.crm.model.CrmModels.TicketPriority;
import com.trillionloans.crm.model.CrmModels.TicketStatus;
import com.trillionloans.crm.model.CrmModels.TicketSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class FreshdeskTicketService {

  private static final Logger log = LoggerFactory.getLogger(FreshdeskTicketService.class);

  private final RestClient webClient;
  private final String apiKey;
  private final String baseUrl;

  public FreshdeskTicketService(
      RestClient.Builder builder,
      @Value("${crm.integrations.freshdesk.base-url:}") String baseUrl,
      @Value("${crm.integrations.freshdesk.api-key:}") String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.webClient = baseUrl.isBlank() ? null : builder.baseUrl(baseUrl).build();
  }

  public boolean isConfigured() {
    return webClient != null && !baseUrl.isBlank() && !apiKey.isBlank();
  }

  public List<TicketSummary> searchTicketsByEmail(String email) {
    if (!isConfigured() || email == null || email.isBlank()) {
      return List.of();
    }
    try {
      JsonNode response =
          webClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/api/v2/search/tickets")
                          .queryParam("query", "\"email:'" + email + "'\"")
                          .build())
              .headers(headers -> headers.setBasicAuth(apiKey, "X"))
              .retrieve()
              .body(JsonNode.class);

      List<TicketSummary> tickets = new ArrayList<>();
      JsonNode results = response == null ? null : response.path("results");
      if (results != null && results.isArray()) {
        results.forEach(
            node -> {
              String leadId = node.path("custom_fields").path("cf_lead_id").asText("");
              tickets.add(mapTicket(leadId.isBlank() ? null : leadId, node));
            });
      }
      return tickets;
    } catch (RuntimeException exception) {
      log.warn("Freshdesk email ticket search failed for {}", email, exception);
      return List.of();
    }
  }

  public List<FreshdeskConversationEntry> fetchConversations(String freshdeskId) {
    if (!isConfigured() || freshdeskId == null || freshdeskId.isBlank()) {
      return List.of();
    }
    try {
      JsonNode response =
          webClient
              .get()
              .uri("/api/v2/tickets/{id}/conversations", freshdeskId)
              .headers(headers -> headers.setBasicAuth(apiKey, "X"))
              .retrieve()
              .body(JsonNode.class);

      List<FreshdeskConversationEntry> entries = new ArrayList<>();
      if (response != null && response.isArray()) {
        response.forEach(node -> entries.add(mapConversation(node)));
      }
      return entries;
    } catch (RuntimeException exception) {
      log.warn("Freshdesk conversation fetch failed for ticket {}", freshdeskId, exception);
      return List.of();
    }
  }

  public String createTicket(
      String subject,
      String description,
      String leadId,
      String mobileNumber,
      String loanAccountNumber,
      String channelTag) {
    if (!isConfigured()) {
      return null;
    }

    Map<String, Object> customFields = new HashMap<>();
    if (leadId != null && !leadId.isBlank()) {
      customFields.put("cf_lead_id", leadId);
    }
    if (mobileNumber != null && !mobileNumber.isBlank()) {
      customFields.put("cf_mobile", mobileNumber);
    }
    if (loanAccountNumber != null && !loanAccountNumber.isBlank()) {
      customFields.put("cf_loan_account_number", loanAccountNumber);
    }
    customFields.put("cf_source_channel", channelTag);

    Map<String, Object> payload = new HashMap<>();
    payload.put("subject", subject);
    payload.put("description", description);
    payload.put("priority", 2);
    payload.put("status", 2);
    payload.put("tags", List.of(channelTag));
    payload.put("custom_fields", customFields);

    JsonNode response =
        webClient
            .post()
            .uri("/api/v2/tickets")
            .headers(headers -> headers.setBasicAuth(apiKey, "X"))
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(JsonNode.class);
    return response == null ? null : response.path("id").asText(null);
  }

  public void updateCustomFields(String freshdeskId, Map<String, Object> fields) {
    if (!isConfigured() || fields == null || fields.isEmpty()) {
      return;
    }
    Map<String, Object> payload = new HashMap<>();
    payload.put("custom_fields", fields);
    webClient
        .put()
        .uri("/api/v2/tickets/{id}", freshdeskId)
        .headers(headers -> headers.setBasicAuth(apiKey, "X"))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .toBodilessEntity();
  }

  public void linkCustomer(
      String freshdeskId, String leadId, String mobileNumber, String loanAccountNumber) {
    if (!isConfigured()) {
      return;
    }
    Map<String, Object> fields = new HashMap<>();
    fields.put("cf_lead_id", leadId);
    if (mobileNumber != null && !mobileNumber.isBlank()) {
      fields.put("cf_mobile", mobileNumber);
    }
    if (loanAccountNumber != null && !loanAccountNumber.isBlank()) {
      fields.put("cf_loan_account_number", loanAccountNumber);
    }
    updateCustomFields(freshdeskId, fields);
  }

  public String baseUrl() {
    return baseUrl;
  }

  public List<TicketSummary> searchTickets(CustomerProfile profile) {
    if (!isConfigured() || profile == null || profile.email() == null || profile.email().isBlank()) {
      return List.of();
    }

    try {
      JsonNode response =
          webClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/api/v2/search/tickets")
                          .queryParam("query", "\"email:'" + profile.email() + "'\"")
                          .build())
              .headers(headers -> headers.setBasicAuth(apiKey, "X"))
              .retrieve()
              .body(JsonNode.class);

      List<TicketSummary> tickets = new ArrayList<>();
      JsonNode results = response == null ? null : response.path("results");
      if (results != null && results.isArray()) {
        results.forEach(node -> tickets.add(mapTicket(profile.leadId(), node)));
      }
      return tickets;
    } catch (RuntimeException exception) {
      log.warn("Freshdesk ticket search failed for lead {}", profile.leadId(), exception);
      return List.of();
    }
  }

  public List<FreshdeskAgentTicket> fetchAgentTickets(String agentEmail) {
    if (!isConfigured()) {
      return List.of();
    }

    JsonNode response =
        webClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/api/v2/search/tickets")
                        .queryParam("query", "\"agent_email:'" + agentEmail + "'\"")
                        .build())
            .headers(headers -> headers.setBasicAuth(apiKey, "X"))
            .retrieve()
            .body(JsonNode.class);

    List<FreshdeskAgentTicket> tickets = new ArrayList<>();
    JsonNode results = response == null ? null : response.path("results");
    if (results != null && results.isArray()) {
      results.forEach(node -> tickets.add(mapAgentTicket(node, agentEmail)));
    }
    return tickets;
  }

  public void addReply(String freshdeskId, String body) {
    if (!isConfigured()) {
      return;
    }

    Map<String, Object> payload = new HashMap<>();
    payload.put("body", body);

    webClient
        .post()
        .uri("/api/v2/tickets/{id}/reply", freshdeskId)
        .headers(headers -> headers.setBasicAuth(apiKey, "X"))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .toBodilessEntity();
  }

  public void updateTicket(String freshdeskId, TicketStatus status, TicketPriority priority) {
    if (!isConfigured()) {
      return;
    }

    Map<String, Object> payload = new HashMap<>();
    if (status != null) {
      payload.put("status", mapStatusToFreshdesk(status));
    }
    if (priority != null) {
      payload.put("priority", mapPriorityToFreshdesk(priority));
    }

    webClient
        .put()
        .uri("/api/v2/tickets/{id}", freshdeskId)
        .headers(headers -> headers.setBasicAuth(apiKey, "X"))
        .contentType(MediaType.APPLICATION_JSON)
        .body(payload)
        .retrieve()
        .toBodilessEntity();
  }

  private FreshdeskAgentTicket mapAgentTicket(JsonNode node, String agentEmail) {
    Instant createdAt = parseInstant(node.path("created_at").asText(null));
    Instant updatedAt = parseInstant(node.path("updated_at").asText(null));
    TicketStatus status = mapStatus(node.path("status").asInt(2));
    Instant closedAt =
        status == TicketStatus.CLOSED || status == TicketStatus.RESOLVED ? updatedAt : null;

    return new FreshdeskAgentTicket(
        "fd-" + node.path("id").asText(),
        node.path("id").asText(),
        node.path("custom_fields").path("cf_lead_id").asText(""),
        node.path("subject").asText("Freshdesk ticket"),
        node.path("requester").path("name").asText("Customer"),
        node.path("requester").path("email").asText(""),
        node.path("custom_fields").path("cf_mobile").asText(""),
        node.path("custom_fields").path("cf_loan_account_number").asText(""),
        status,
        mapPriority(node.path("priority").asInt(1)),
        node.path("type").asText("Support"),
        node.path("responder").path("name").asText("Agent"),
        agentEmail,
        node.path("source").asText("Email"),
        resolveSourceChannel(node),
        node.path("status").asText(),
        createdAt,
        updatedAt,
        closedAt,
        1,
        List.of(
            new FreshdeskConversationEntry(
                "conv-" + node.path("id").asText(),
                node.path("requester").path("name").asText("Customer"),
                node.path("description_text").asText(""),
                createdAt,
                false)));
  }

  private String resolveSourceChannel(JsonNode node) {
    String custom = node.path("custom_fields").path("cf_source_channel").asText("");
    if (!custom.isBlank()) {
      return custom;
    }
    JsonNode tags = node.path("tags");
    if (tags.isArray()) {
      for (JsonNode tag : tags) {
        String value = tag.asText("");
        if ("greylabs_bot".equalsIgnoreCase(value) || "agent".equalsIgnoreCase(value)) {
          return value;
        }
      }
    }
    return "agent";
  }

  private FreshdeskConversationEntry mapConversation(JsonNode node) {
    boolean incoming = node.path("incoming").asBoolean(true);
    return new FreshdeskConversationEntry(
        node.path("id").asText(),
        incoming ? "Customer" : "Agent",
        node.path("body_text").asText(node.path("body").asText("")),
        parseInstant(node.path("created_at").asText(null)),
        !incoming);
  }

  private TicketSummary mapTicket(String leadId, JsonNode node) {
    Instant createdAt = parseInstant(node.path("created_at").asText(null));
    Instant updatedAt = parseInstant(node.path("updated_at").asText(null));
    return new TicketSummary(
        "fd-" + node.path("id").asText(),
        leadId,
        node.path("subject").asText("Freshdesk ticket"),
        mapStatus(node.path("status").asInt(2)),
        mapPriority(node.path("priority").asInt(1)),
        node.path("type").asText("Freshdesk"),
        createdAt,
        updatedAt);
  }

  private TicketStatus mapStatus(int status) {
    return switch (status) {
      case 3 -> TicketStatus.PENDING;
      case 4 -> TicketStatus.RESOLVED;
      case 5 -> TicketStatus.CLOSED;
      default -> TicketStatus.OPEN;
    };
  }

  private int mapStatusToFreshdesk(TicketStatus status) {
    return switch (status) {
      case PENDING -> 3;
      case RESOLVED -> 4;
      case CLOSED -> 5;
      default -> 2;
    };
  }

  private TicketPriority mapPriority(int priority) {
    return switch (priority) {
      case 3 -> TicketPriority.HIGH;
      case 4 -> TicketPriority.URGENT;
      case 2 -> TicketPriority.MEDIUM;
      default -> TicketPriority.LOW;
    };
  }

  private int mapPriorityToFreshdesk(TicketPriority priority) {
    return switch (priority) {
      case HIGH -> 3;
      case URGENT -> 4;
      case MEDIUM -> 2;
      default -> 1;
    };
  }

  private Instant parseInstant(String value) {
    if (value == null || value.isBlank()) {
      return Instant.now();
    }
    return Instant.parse(value);
  }
}
