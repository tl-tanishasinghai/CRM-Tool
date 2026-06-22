package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.CrmModels.LoanRepaymentSchedule;
import com.trillionloans.crm.model.CrmModels.LoanTransactionHistory;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.WrapperModels.ApiResponse;
import com.trillionloans.crm.model.WrapperModels.LoanDetailResponse;
import com.trillionloans.crm.service.AuthService;
import com.trillionloans.crm.service.LoanAggregationService;
import com.trillionloans.crm.service.LoanDetailService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm/customers/{leadId}/loans/{loanAccountNumber}")
public class LoanController {

  private final AuthService authService;
  private final LoanDetailService loanDetailService;
  private final LoanAggregationService loanAggregationService;

  public LoanController(
      AuthService authService,
      LoanDetailService loanDetailService,
      LoanAggregationService loanAggregationService) {
    this.authService = authService;
    this.loanDetailService = loanDetailService;
    this.loanAggregationService = loanAggregationService;
  }

  /** FR2: aggregated loan detail for agent desk and bot layer. */
  @GetMapping("/details")
  public ApiResponse<LoanDetailResponse> details(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String leadId,
      @PathVariable String loanAccountNumber) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return ApiResponse.success(loanAggregationService.getLoanDetail(leadId, loanAccountNumber));
  }

  @GetMapping("/rps")
  public LoanRepaymentSchedule repaymentSchedule(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String leadId,
      @PathVariable String loanAccountNumber) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return loanDetailService.getRepaymentSchedule(leadId, loanAccountNumber);
  }

  @GetMapping("/rps/original")
  public LoanRepaymentSchedule originalSchedule(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String leadId,
      @PathVariable String loanAccountNumber) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return loanDetailService.getOriginalSchedule(leadId, loanAccountNumber);
  }

  @GetMapping("/transactions")
  public LoanTransactionHistory transactions(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String leadId,
      @PathVariable String loanAccountNumber) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return loanDetailService.getTransactions(leadId, loanAccountNumber);
  }
}
