package com.trillionloans.crm.service;

import com.trillionloans.crm.integration.LoanStatusMapper;
import com.trillionloans.crm.integration.ProductNameMapper;
import com.trillionloans.crm.integration.lms.LmsLoanDetailsDto;
import com.trillionloans.crm.integration.los.LosLeadIdDto;
import com.trillionloans.crm.integration.los.LosLeadProfileDto;
import com.trillionloans.crm.model.CrmModels.CustomerProfile;
import com.trillionloans.crm.model.CrmModels.LoanSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Service
public class ExternalDataService {

  private static final Logger log = LoggerFactory.getLogger(ExternalDataService.class);
  private static final List<DateTimeFormatter> DATE_FORMATS =
      List.of(
          DateTimeFormatter.ISO_LOCAL_DATE,
          DateTimeFormatter.ofPattern("dd-MM-yyyy"),
          DateTimeFormatter.ofPattern("dd/MM/yyyy"),
          DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));

  private final RestClient losClient;
  private final RestClient lmsClient;
  private final String productCode;
  private final boolean useLiveIntegrations;

  public ExternalDataService(
      RestClient.Builder builder,
      @Value("${crm.integrations.los-base-url:http://localhost:8090}") String losBaseUrl,
      @Value("${crm.integrations.lms-base-url:http://localhost:8091}") String lmsBaseUrl,
      @Value("${crm.integrations.product-code:bharatpe}") String productCode,
      @Value("${crm.integrations.use-live-data:true}") boolean useLiveIntegrations) {
    this.losClient = builder.baseUrl(losBaseUrl).build();
    this.lmsClient = builder.baseUrl(lmsBaseUrl).build();
    this.productCode = productCode;
    this.useLiveIntegrations = useLiveIntegrations;
  }

  public CustomerProfile getCustomerProfile(String leadId) {
    if (!useLiveIntegrations) {
      return mockProfile(leadId, "MOCK");
    }
    try {
      LosLeadProfileDto profile =
          losClient
              .get()
              .uri("/partners/api/v1/lead/cp/{leadId}", leadId)
              .headers(this::applyLosHeaders)
              .retrieve()
              .body(LosLeadProfileDto.class);
      if (profile != null) {
        return mapProfile(leadId, profile, "LOS");
      }
    } catch (HttpClientErrorException.NotFound notFound) {
      log.warn("LOS profile not found for leadId={}", leadId);
    } catch (Exception ex) {
      log.warn("LOS profile fetch failed for leadId={}: {}", leadId, ex.getMessage());
    }
    return mockProfile(leadId, "MOCK");
  }

  public List<LoanSummary> getLoanSummaries(String leadId) {
    if (!useLiveIntegrations) {
      return mockLoans(leadId);
    }
    try {
      List<LmsLoanDetailsDto> loans =
          lmsClient
              .get()
              .uri("/partners/api/v1/collection/{leadId}/loan/details", leadId)
              .headers(this::applyJsonHeaders)
              .retrieve()
              .body(new ParameterizedTypeReference<List<LmsLoanDetailsDto>>() {});
      if (loans != null && !loans.isEmpty()) {
        return loans.stream()
            .filter(loan -> !LoanStatusMapper.isExcluded(loan.status()))
            .map(this::mapLoan)
            .toList();
      }
    } catch (Exception ex) {
      log.warn("LMS loan fetch failed for leadId={}: {}", leadId, ex.getMessage());
    }
    return mockLoans(leadId);
  }

  public List<String> searchLeadIdsByMobile(String mobileNumber) {
    if (!useLiveIntegrations || mobileNumber == null || mobileNumber.isBlank()) {
      return List.of();
    }
    Set<String> leadIds = new LinkedHashSet<>();
    try {
      LosLeadIdDto[] matches =
          losClient
              .get()
              .uri("/partners/api/v1/lead/info/{mobileNumber}", mobileNumber.trim())
              .headers(this::applyLosHeaders)
              .retrieve()
              .body(LosLeadIdDto[].class);
      if (matches != null) {
        Arrays.stream(matches)
            .map(LosLeadIdDto::resolvedLeadId)
            .filter(id -> id != null && !id.isBlank())
            .forEach(leadIds::add);
      }
    } catch (Exception ex) {
      log.debug("LOS mobile lookup (info) failed for {}: {}", mobileNumber, ex.getMessage());
    }

    if (leadIds.isEmpty()) {
      try {
        Object payload =
            losClient
                .get()
                .uri("/partners/api/v1/lead/info/{mobileNumber}", mobileNumber.trim())
                .headers(this::applyLosHeaders)
                .retrieve()
                .body(Object.class);
        leadIds.addAll(extractLeadIds(payload));
      } catch (Exception ex) {
        log.debug("LOS mobile lookup fallback failed for {}: {}", mobileNumber, ex.getMessage());
      }
    }
    return new ArrayList<>(leadIds);
  }

  public RestClient losClient() {
    return losClient;
  }

  public RestClient lmsClient() {
    return lmsClient;
  }

  public String productCode() {
    return productCode;
  }

  private void applyLosHeaders(HttpHeaders headers) {
    applyJsonHeaders(headers);
    headers.set("productCode", productCode);
  }

  private void applyJsonHeaders(HttpHeaders headers) {
    headers.setContentType(MediaType.APPLICATION_JSON);
  }

  private CustomerProfile mapProfile(String requestedLeadId, LosLeadProfileDto dto, String source) {
    String leadId = dto.leadId() != null ? dto.leadId().toString() : requestedLeadId;
    String clientId = dto.ucic() != null && !dto.ucic().isBlank() ? dto.ucic() : "C" + leadId;
    return new CustomerProfile(
        leadId,
        clientId,
        defaultText(dto.name(), "Customer " + leadId),
        defaultText(dto.mobileNo(), "—"),
        defaultText(dto.email(), "—"),
        panLast4(dto.panNumber()),
        parseDate(dto.dateOfBirth()),
        extractCity(dto.address()),
        defaultText(dto.address(), "—"),
        defaultText(dto.ucic(), clientId),
        source);
  }

  private LoanSummary mapLoan(LmsLoanDetailsDto loan) {
    BigDecimal principal = decimal(loan.loanAmount());
    BigDecimal outstanding = decimal(loan.totalPrincipalOutstanding());
    BigDecimal paid = principal.subtract(outstanding).max(BigDecimal.ZERO);
    String status = LoanStatusMapper.mapStatus(loan.status());
    int tenureMonths = loan.tenure() != null ? loan.tenure().intValue() : 0;
    String product =
        ProductNameMapper.resolveProductName(
            loan.productCode(), loan.productId() != null ? loan.productId() : null);

    return new LoanSummary(
        defaultText(loan.loanAccountNumber(), "—"),
        loan.loanApplicationId() != null ? loan.loanApplicationId().toString() : "—",
        product,
        status,
        defaultText(loan.officeName(), "TRILLIONLOANS"),
        status.equals("Active") ? "Repayment in progress" : status,
        status,
        principal,
        outstanding,
        decimal(loan.emiAmount()),
        paid,
        outstanding,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.valueOf(loan.rateOfInterest()).setScale(2, RoundingMode.HALF_UP),
        0,
        tenureMonths,
        tenureMonths,
        parseDate(loan.disbursementDate()),
        null,
        status.equals("Active")
            ? "Open the Loans tab for repayment schedule and transactions."
            : status + " loan on file.");
  }

  private List<String> extractLeadIds(Object payload) {
    Set<String> leadIds = new LinkedHashSet<>();
    if (payload instanceof List<?> list) {
      for (Object item : list) {
        if (item instanceof java.util.Map<?, ?> map) {
          Object id = map.get("id");
          if (id == null) {
            id = map.get("entityId");
          }
          if (id != null) {
            leadIds.add(String.valueOf(id));
          }
        }
      }
    }
    return new ArrayList<>(leadIds);
  }

  private String panLast4(String panNumber) {
    if (panNumber == null || panNumber.length() < 4) {
      return "—";
    }
    return panNumber.substring(panNumber.length() - 4);
  }

  private LocalDate parseDate(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    for (DateTimeFormatter formatter : DATE_FORMATS) {
      try {
        return LocalDate.parse(raw.trim(), formatter);
      } catch (DateTimeParseException ignored) {
        // try next format
      }
    }
    return null;
  }

  private String extractCity(String address) {
    if (address == null || address.isBlank()) {
      return "—";
    }
    String[] parts = address.split(",");
    String city = parts[parts.length - 1].trim();
    return city.isBlank() ? address.trim() : city;
  }

  private BigDecimal decimal(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }

  private String defaultText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }

  private CustomerProfile mockProfile(String leadId, String source) {
    if ("1002002".equals(leadId)) {
      return new CustomerProfile(
          leadId,
          "C" + leadId,
          "Priya Sharma",
          "8888888888",
          "priya.sharma@example.com",
          "9988",
          LocalDate.of(1989, 8, 21),
          "Mumbai",
          "Andheri East, Mumbai",
          "UCIC-" + leadId,
          source);
    }
    return new CustomerProfile(
        leadId,
        "C" + leadId,
        "Rahul Mehta",
        "9999999999",
        "rahul.mehta@example.com",
        "1234",
        LocalDate.of(1992, 1, 15),
        "Bengaluru",
        "Indiranagar, Bengaluru",
        "UCIC-" + leadId,
        source);
  }

  private List<LoanSummary> mockLoans(String leadId) {
    return List.of(
        new LoanSummary(
            "LAN-" + leadId,
            "LA-" + leadId,
            "BharatPe ML",
            "Active",
            "TRILLIONLOANS",
            "Verification Completed",
            "Approved",
            BigDecimal.valueOf(250000),
            BigDecimal.valueOf(182450),
            BigDecimal.valueOf(8750),
            BigDecimal.valueOf(67550),
            BigDecimal.valueOf(182450),
            BigDecimal.valueOf(3117),
            BigDecimal.valueOf(698),
            BigDecimal.valueOf(1.68),
            0,
            10,
            3,
            LocalDate.now().minusMonths(7),
            LocalDate.now().plusDays(12),
            "Application approved. Money will be transferred in the next few days."),
        new LoanSummary(
            "LAN-" + leadId + "-2",
            "LA-" + leadId + "-2",
            "Mobikwik",
            "Closed",
            "LDC",
            "Repayment Complete",
            "Closed",
            BigDecimal.valueOf(75000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(4200),
            BigDecimal.valueOf(75000),
            BigDecimal.ZERO,
            BigDecimal.valueOf(849),
            BigDecimal.ZERO,
            BigDecimal.valueOf(1.80),
            0,
            12,
            0,
            LocalDate.now().minusYears(1),
            null,
            "Loan closed successfully."),
        new LoanSummary(
            "LAN-" + leadId + "-3",
            "LA-" + leadId + "-3",
            "MoneyView",
            "Rejected",
            "LDC",
            "Eligibility Failed",
            "Rejected",
            BigDecimal.valueOf(50000),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.valueOf(1.80),
            0,
            0,
            0,
            LocalDate.now().minusYears(2),
            null,
            "Your bank is facing technical issue with your bank account. Reapply after eligibility check."));
  }
}
