package com.trillionloans.crm.service;

import com.trillionloans.crm.model.CrmModels.CustomerProfile;
import com.trillionloans.crm.model.WrapperModels.CustomerProfileResponse;
import com.trillionloans.crm.model.WrapperModels.CustomerSummaryResponse;
import com.trillionloans.crm.model.WrapperModels.LoanMiniSummary;
import com.trillionloans.crm.model.WrapperModels.MaskedIdentity;
import com.trillionloans.crm.integration.LoanStatusMapper;
import com.trillionloans.crm.integration.ProductNameMapper;
import com.trillionloans.crm.integration.lms.LmsLoanDetailsDto;
import com.trillionloans.crm.model.CrmModels.LoanSummary;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class CustomerSummaryService {

  private final ExternalDataService externalDataService;
  private final LmsIntegrationService lmsIntegrationService;
  private final PiiMaskingService piiMaskingService;

  public CustomerSummaryService(
      ExternalDataService externalDataService,
      LmsIntegrationService lmsIntegrationService,
      PiiMaskingService piiMaskingService) {
    this.externalDataService = externalDataService;
    this.lmsIntegrationService = lmsIntegrationService;
    this.piiMaskingService = piiMaskingService;
  }

  public CustomerProfileResponse getProfile(String leadId) {
    CustomerSummaryResponse summary = getSummaryByLeadId(leadId);
    return new CustomerProfileResponse(
        summary.identity(), summary.loanAccounts(), summary.dataSource());
  }

  public CustomerSummaryResponse getSummaryByLeadId(String leadId) {
    CustomerProfile raw = externalDataService.getCustomerProfile(leadId);
    List<LoanMiniSummary> loans = buildLoanMiniSummaries(leadId);
    if (loans.isEmpty()) {
      loans = externalDataService.getLoanSummaries(leadId).stream().map(this::fromLoanSummary).toList();
    }
    return buildSummary(raw, loans);
  }

  public CustomerSummaryResponse getSummaryByMobile(String mobile) {
    List<String> leadIds = externalDataService.searchLeadIdsByMobile(mobile);
    if (leadIds.isEmpty()) {
      return notFoundSummary();
    }

    Map<String, LoanMiniSummary> mergedLoans = new LinkedHashMap<>();
    CustomerProfile primaryProfile = null;

    for (String leadId : leadIds) {
      CustomerProfile profile = externalDataService.getCustomerProfile(leadId);
      if (primaryProfile == null) {
        primaryProfile = profile;
      }
      buildLoanMiniSummaries(leadId).forEach(loan -> mergedLoans.putIfAbsent(loan.loanAccountNumber(), loan));
    }

    if (mergedLoans.isEmpty() && primaryProfile != null) {
      externalDataService.getLoanSummaries(primaryProfile.leadId()).stream()
          .map(this::fromLoanSummary)
          .forEach(loan -> mergedLoans.putIfAbsent(loan.loanAccountNumber(), loan));
    }

    if (primaryProfile == null) {
      return notFoundSummary();
    }

    return buildSummary(primaryProfile, new ArrayList<>(mergedLoans.values()));
  }

  private CustomerSummaryResponse buildSummary(CustomerProfile raw, List<LoanMiniSummary> loans) {
    MaskedIdentity identity = toMaskedIdentity(raw);
    List<LoanMiniSummary> active =
        loans.stream().filter(loan -> isActive(loan.status())).toList();
    List<LoanMiniSummary> closed =
        loans.stream().filter(loan -> !isActive(loan.status())).toList();
    return new CustomerSummaryResponse(
        true,
        raw.leadId(),
        identity,
        active,
        closed,
        loans,
        raw.dataSource());
  }

  private CustomerSummaryResponse notFoundSummary() {
    return new CustomerSummaryResponse(
        false,
        null,
        new MaskedIdentity(
            null, "—", "—", "—", "—", "—", null, null, null, null, "NONE"),
        List.of(),
        List.of(),
        List.of(),
        "NONE");
  }

  private MaskedIdentity toMaskedIdentity(CustomerProfile raw) {
    String dob = raw.dateOfBirth() != null ? raw.dateOfBirth().toString() : null;
    return new MaskedIdentity(
        raw.name(),
        piiMaskingService.maskMobile(raw.mobileNo()),
        piiMaskingService.maskEmail(raw.email()),
        piiMaskingService.maskAddress(raw.address()),
        piiMaskingService.shortenAddress(raw.address()),
        piiMaskingService.panLast4(raw.panLast4() != null && raw.panLast4().length() == 4
            ? "XXXXXXXX" + raw.panLast4()
            : raw.panLast4()),
        dob,
        raw.leadId(),
        raw.ucic(),
        raw.clientId(),
        raw.dataSource());
  }

  private List<LoanMiniSummary> buildLoanMiniSummaries(String leadId) {
    return lmsIntegrationService.fetchLoansByLeadId(leadId).stream()
        .filter(loan -> !LoanStatusMapper.isExcluded(loan.status()))
        .map(this::fromLmsLoan)
        .toList();
  }

  private LoanMiniSummary fromLmsLoan(LmsLoanDetailsDto loan) {
    BigDecimal principal = decimal(loan.loanAmount());
    BigDecimal net = decimal(loan.netDisbursementAmount());
    BigDecimal pf = principal.subtract(net).max(BigDecimal.ZERO);
    String status = LoanStatusMapper.mapStatus(loan.status());
    return new LoanMiniSummary(
        loan.loanAccountNumber(),
        loan.loanApplicationId() != null ? loan.loanApplicationId().toString() : null,
        ProductNameMapper.resolveProductName(
            loan.productCode(), loan.productId() != null ? loan.productId() : null),
        status,
        defaultText(loan.officeName(), "TRILLIONLOANS"),
        principal,
        principal,
        net,
        pf,
        loan.tenure() != null ? loan.tenure().intValue() : null,
        BigDecimal.valueOf(loan.rateOfInterest()).setScale(2, RoundingMode.HALF_UP),
        loan.disbursementDate(),
        0,
        decimal(loan.totalPrincipalOutstanding()),
        decimal(loan.emiAmount()));
  }

  private LoanMiniSummary fromLoanSummary(LoanSummary loan) {
    return new LoanMiniSummary(
        loan.loanAccountNumber(),
        loan.loanApplicationId(),
        loan.product(),
        loan.status(),
        loan.lenderName(),
        loan.principal(),
        loan.principal(),
        loan.principal().subtract(loan.excessAdjusted()).max(BigDecimal.ZERO),
        loan.excessAdjusted(),
        loan.tenureMonths(),
        loan.interestRate(),
        loan.disbursementDate() != null ? loan.disbursementDate().toString() : null,
        loan.dpd(),
        loan.outstanding(),
        loan.emi());
  }

  private boolean isActive(String status) {
    return status != null && status.equalsIgnoreCase("Active");
  }

  private BigDecimal decimal(double value) {
    return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
  }

  private String defaultText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
