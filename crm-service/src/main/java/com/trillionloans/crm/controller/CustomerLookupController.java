package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.WrapperModels.ApiResponse;
import com.trillionloans.crm.model.WrapperModels.CustomerSummaryResponse;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.service.AuthService;
import com.trillionloans.crm.service.CustomerSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm/customers")
public class CustomerLookupController {

  private final AuthService authService;
  private final CustomerSummaryService customerSummaryService;

  public CustomerLookupController(
      AuthService authService, CustomerSummaryService customerSummaryService) {
    this.authService = authService;
    this.customerSummaryService = customerSummaryService;
  }

  /** FR1: customer summary by mobile (GreyLabs + inbound call path). */
  @GetMapping("/by-mobile/{mobile}/summary")
  public ApiResponse<CustomerSummaryResponse> summaryByMobile(
      @RequestHeader("X-CRM-Token") String token, @PathVariable String mobile) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    CustomerSummaryResponse summary = customerSummaryService.getSummaryByMobile(mobile);
    if (!summary.customerFound()) {
      return ApiResponse.failure("CUSTOMER_NOT_FOUND", "No customer found for mobile number");
    }
    return ApiResponse.success(summary);
  }

  /** FR1: customer summary by leadId. */
  @GetMapping("/{leadId}/summary")
  public ApiResponse<CustomerSummaryResponse> summaryByLeadId(
      @RequestHeader("X-CRM-Token") String token, @PathVariable String leadId) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return ApiResponse.success(customerSummaryService.getSummaryByLeadId(leadId));
  }
}
