package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.CrmModels.IvrOverviewResponse;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.service.AgentFreshdeskTicketService;
import com.trillionloans.crm.service.AuthService;
import com.trillionloans.crm.service.IvrOverviewService;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm/ivr")
public class IvrController {

  private final AuthService authService;
  private final IvrOverviewService ivrOverviewService;

  public IvrController(AuthService authService, IvrOverviewService ivrOverviewService) {
    this.authService = authService;
    this.ivrOverviewService = ivrOverviewService;
  }

  @GetMapping("/overview")
  public IvrOverviewResponse overview(
      @RequestHeader("X-CRM-Token") String token,
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String leadId,
      @RequestParam(required = false) String mobileNumber,
      @RequestParam(required = false) String loanAccountNumber,
      @RequestParam(required = false) String disposition,
      @RequestParam(required = false) String callSource,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    StaffUser user = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return ivrOverviewService.listOverview(
        user,
        query,
        leadId,
        mobileNumber,
        loanAccountNumber,
        disposition,
        callSource,
        parseInstant(from),
        parseInstant(to),
        page,
        size);
  }

  private Instant parseInstant(String value) {
    return AgentFreshdeskTicketService.parseInstantParam(value);
  }
}
