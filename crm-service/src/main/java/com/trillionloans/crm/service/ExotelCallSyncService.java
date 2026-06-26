package com.trillionloans.crm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.trillionloans.crm.model.CrmModels.CallDirection;
import com.trillionloans.crm.model.CrmModels.CallDisposition;
import com.trillionloans.crm.model.CrmModels.CallEvent;
import com.trillionloans.crm.model.CrmModels.CallSyncStatus;
import com.trillionloans.crm.model.CrmModels.ExotelSyncResult;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ExotelCallSyncService {

  private static final Logger log = LoggerFactory.getLogger(ExotelCallSyncService.class);
  private static final ZoneId IST = ZoneId.of("Asia/Kolkata");
  private static final DateTimeFormatter EXOTEL_QUERY_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter EXOTEL_RESPONSE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);

  private final CrmStore store;
  private final RestClient webClient;
  private final String accountSid;
  private final String apiKey;
  private final String apiToken;
  private final int pageSize;

  public ExotelCallSyncService(
      CrmStore store,
      RestClient.Builder builder,
      @Value("${crm.integrations.exotel.base-url:https://api.exotel.com}") String baseUrl,
      @Value("${crm.integrations.exotel.account-sid:}") String accountSid,
      @Value("${crm.integrations.exotel.api-key:}") String apiKey,
      @Value("${crm.integrations.exotel.api-token:}") String apiToken,
      @Value("${crm.integrations.exotel.page-size:100}") int pageSize) {
    this.store = store;
    this.webClient = builder.baseUrl(baseUrl).build();
    this.accountSid = accountSid;
    this.apiKey = apiKey;
    this.apiToken = apiToken;
    this.pageSize = pageSize;
  }

  @Scheduled(cron = "${crm.integrations.exotel.daily-cron:0 0 1 * * *}", zone = "Asia/Kolkata")
  public void syncPreviousDayScheduled() {
    ExotelSyncResult result = syncPreviousDay();
    log.info("Exotel daily call sync completed: {}", result);
  }

  public ExotelSyncResult syncPreviousDay() {
    LocalDate previousDay = LocalDate.now(IST).minusDays(1);
    Instant from = previousDay.atStartOfDay(IST).toInstant();
    Instant to = previousDay.plusDays(1).atStartOfDay(IST).minusSeconds(1).toInstant();
    return syncRange(from, to);
  }

  public ExotelSyncResult syncRange(Instant from, Instant to) {
    if (!isConfigured()) {
      return new ExotelSyncResult(from, to, 0, 0, 0, 1, "Exotel credentials are not configured");
    }

    int page = 1;
    int fetched = 0;
    int inserted = 0;
    int updated = 0;
    int failed = 0;

    while (true) {
      try {
        JsonNode response = fetchPage(from, to, page);
        List<JsonNode> calls = extractCalls(response);
        if (calls.isEmpty()) {
          break;
        }

        for (JsonNode callNode : calls) {
          CallEvent call = mapCall(callNode);
          boolean wasInserted = store.upsertCallBySid(call);
          if (wasInserted) {
            inserted++;
          } else {
            updated++;
          }
        }
        fetched += calls.size();

        if (calls.size() < pageSize || !hasNextPage(response, page)) {
          break;
        }
        page++;
      } catch (RuntimeException exception) {
        failed++;
        log.error("Exotel call sync failed for page {}", page, exception);
        break;
      }
    }

    return new ExotelSyncResult(from, to, fetched, inserted, updated, failed, "Completed");
  }

  private JsonNode fetchPage(Instant from, Instant to, int page) {
    String dateFilter =
        "gte:"
            + LocalDateTime.ofInstant(from, IST).format(EXOTEL_QUERY_FORMAT)
            + ";lte:"
            + LocalDateTime.ofInstant(to, IST).format(EXOTEL_QUERY_FORMAT);

    RuntimeException lastFailure = null;
    for (int attempt = 1; attempt <= 3; attempt++) {
      try {
        return webClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path("/v1/Accounts/{accountSid}/Calls.json")
                        .queryParam("DateCreated", dateFilter)
                        .queryParam("Page", page)
                        .queryParam("PageSize", pageSize)
                        .build(accountSid))
            .headers(headers -> headers.setBasicAuth(apiKey, apiToken))
            .retrieve()
            .body(JsonNode.class);
      } catch (RuntimeException ex) {
        lastFailure = ex;
        if (attempt < 3) {
          try {
            Thread.sleep(5000L);
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw ex;
          }
        }
      }
    }
    throw lastFailure == null ? new IllegalStateException("Exotel fetch failed") : lastFailure;
  }

  private List<JsonNode> extractCalls(JsonNode response) {
    List<JsonNode> calls = new ArrayList<>();
    if (response == null) {
      return calls;
    }
    JsonNode callsNode = response.path("Calls");
    if (!callsNode.isArray()) {
      return calls;
    }
    callsNode.forEach(node -> calls.add(node.has("Call") ? node.path("Call") : node));
    return calls;
  }

  private boolean hasNextPage(JsonNode response, int currentPage) {
    JsonNode nextPageUri = response.path("NextPageUri");
    if (!nextPageUri.isMissingNode() && !nextPageUri.asText("").isBlank()) {
      return true;
    }
    int numPages = response.path("NumPages").asInt(currentPage);
    return currentPage < numPages;
  }

  private CallEvent mapCall(JsonNode node) {
    String sid = text(node, "Sid");
    String from = text(node, "From");
    String to = text(node, "To");
    Instant startedAt = parseExotelTime(text(node, "StartTime"));
    Instant endedAt = parseExotelTime(text(node, "EndTime"));
    String status = text(node, "Status");
    CallDirection direction = parseDirection(text(node, "Direction"));

    return new CallEvent(
        "call-" + UUID.randomUUID(),
        sid,
        text(node, "ParentCallSid"),
        null,
        text(node, "AgentId"),
        direction,
        dispositionFromStatus(status),
        status,
        direction == CallDirection.INBOUND ? from : to,
        from,
        to,
        intValue(node, "Duration"),
        text(node, "RecordingUrl"),
        inferSourceChannel(sid),
        null,
        startedAt,
        endedAt,
        CallSyncStatus.SYNCED,
        Instant.now(),
        Instant.now());
  }

  private CallDirection parseDirection(String direction) {
    if (direction != null && direction.toLowerCase(Locale.ENGLISH).contains("out")) {
      return CallDirection.OUTBOUND;
    }
    return CallDirection.INBOUND;
  }

  private String inferSourceChannel(String callSid) {
    if (callSid != null && callSid.toLowerCase(Locale.ENGLISH).contains("greylabs")) {
      return "greylabs_bot";
    }
    return "agent";
  }

  private CallDisposition dispositionFromStatus(String status) {
    if (status == null) {
      return CallDisposition.NOT_CONNECTED;
    }
    return switch (status.toLowerCase(Locale.ENGLISH)) {
      case "completed" -> CallDisposition.CONNECTED;
      case "busy", "failed", "no-answer", "canceled" -> CallDisposition.NOT_CONNECTED;
      default -> CallDisposition.NOT_CONNECTED;
    };
  }

  private Instant parseExotelTime(String value) {
    if (value == null || value.isBlank()) {
      return Instant.now();
    }
    try {
      return LocalDateTime.parse(value, EXOTEL_RESPONSE_FORMAT).atZone(IST).toInstant();
    } catch (DateTimeParseException ignored) {
      try {
        return Instant.parse(value);
      } catch (DateTimeParseException ignoredAgain) {
        return Instant.now();
      }
    }
  }

  private String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    String text = value.asText();
    return text.isBlank() ? null : text;
  }

  private Integer intValue(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull() || value.asText("").isBlank()) {
      return null;
    }
    return value.asInt();
  }

  public boolean isConfigured() {
    return !accountSid.isBlank() && !apiKey.isBlank() && !apiToken.isBlank();
  }
}
