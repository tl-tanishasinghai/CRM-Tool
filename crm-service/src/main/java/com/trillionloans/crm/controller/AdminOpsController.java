package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.CrmModels.AgentOpsRow;
import com.trillionloans.crm.model.CrmModels.IntegrationsHealthResponse;
import com.trillionloans.crm.model.CrmModels.OpsOverviewResponse;
import com.trillionloans.crm.model.CrmModels.OpsTicketsPage;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.model.CrmModels.TeamOpsRow;
import com.trillionloans.crm.service.AdminOpsService;
import com.trillionloans.crm.service.AgentFreshdeskTicketService;
import com.trillionloans.crm.service.AuthService;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm/admin/ops")
public class AdminOpsController {

  private final AuthService authService;
  private final AdminOpsService adminOpsService;

  public AdminOpsController(AuthService authService, AdminOpsService adminOpsService) {
    this.authService = authService;
    this.adminOpsService = adminOpsService;
  }

  @GetMapping("/overview")
  public OpsOverviewResponse overview(
      @RequestHeader("X-CRM-Token") String token,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    StaffUser user = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD);
    return adminOpsService.getOverview(
        user, parseInstant(from), parseInstant(to));
  }

  @GetMapping("/teams")
  public List<TeamOpsRow> teams(
      @RequestHeader("X-CRM-Token") String token,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to) {
    StaffUser user = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD);
    return adminOpsService.getTeams(user, parseInstant(from), parseInstant(to));
  }

  @GetMapping("/agents")
  public List<AgentOpsRow> agents(
      @RequestHeader("X-CRM-Token") String token,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String leadId) {
    StaffUser user = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD);
    return adminOpsService.getAgents(user, parseInstant(from), parseInstant(to), leadId);
  }

  @GetMapping("/tickets")
  public OpsTicketsPage tickets(
      @RequestHeader("X-CRM-Token") String token,
      @RequestParam(required = false) String source,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String leadId,
      @RequestParam(required = false) String agentId,
      @RequestParam(required = false) String mobileNumber,
      @RequestParam(required = false) String loanAccountNumber,
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "25") int size) {
    StaffUser user = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD);
    return adminOpsService.getTickets(
        user,
        source,
        status,
        leadId,
        agentId,
        mobileNumber,
        loanAccountNumber,
        parseInstant(from),
        parseInstant(to),
        page,
        size);
  }

  @GetMapping("/health")
  public IntegrationsHealthResponse health(@RequestHeader("X-CRM-Token") String token) {
    authService.requireAnyRole(token, Role.ADMIN);
    return adminOpsService.getIntegrationsHealth();
  }

  private Instant parseInstant(String value) {
    return AgentFreshdeskTicketService.parseInstantParam(value);
  }
}
