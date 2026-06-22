package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.CrmModels.AgentNote;
import com.trillionloans.crm.model.CrmModels.CreateNoteRequest;
import com.trillionloans.crm.model.CrmModels.CreateTicketRequest;
import com.trillionloans.crm.model.CrmModels.CustomerDashboard;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.model.CrmModels.TicketSummary;
import com.trillionloans.crm.model.WrapperModels.ApiResponse;
import com.trillionloans.crm.model.WrapperModels.CustomerProfileResponse;
import com.trillionloans.crm.model.WrapperModels.PiiRevealRequest;
import com.trillionloans.crm.model.WrapperModels.PiiRevealResponse;
import com.trillionloans.crm.service.AuthService;
import com.trillionloans.crm.service.CrmStore;
import com.trillionloans.crm.service.CustomerSummaryService;
import com.trillionloans.crm.service.DashboardService;
import com.trillionloans.crm.service.PiiRevealService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm/customers/{leadId}")
public class CustomerController {

  private final AuthService authService;
  private final CrmStore store;
  private final DashboardService dashboardService;
  private final CustomerSummaryService customerSummaryService;
  private final PiiRevealService piiRevealService;

  public CustomerController(
      AuthService authService,
      CrmStore store,
      DashboardService dashboardService,
      CustomerSummaryService customerSummaryService,
      PiiRevealService piiRevealService) {
    this.authService = authService;
    this.store = store;
    this.dashboardService = dashboardService;
    this.customerSummaryService = customerSummaryService;
    this.piiRevealService = piiRevealService;
  }

  @GetMapping("/dashboard")
  public CustomerDashboard dashboard(
      @RequestHeader("X-CRM-Token") String token, @PathVariable String leadId) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return dashboardService.getDashboard(leadId);
  }

  /** FR1 profile card: masked identity + loan account chips. */
  @GetMapping("/profile")
  public ApiResponse<CustomerProfileResponse> profile(
      @RequestHeader("X-CRM-Token") String token, @PathVariable String leadId) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return ApiResponse.success(customerSummaryService.getProfile(leadId));
  }

  @PostMapping("/reveal")
  public ApiResponse<PiiRevealResponse> reveal(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String leadId,
      @Valid @RequestBody PiiRevealRequest request) {
    StaffUser user = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return ApiResponse.success(
        piiRevealService.reveal(leadId, user.id(), request.field(), request.reason()));
  }

  @GetMapping("/tickets")
  public List<TicketSummary> tickets(
      @RequestHeader("X-CRM-Token") String token, @PathVariable String leadId) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return store.ticketsForLead(leadId);
  }

  @PostMapping("/tickets")
  public TicketSummary createTicket(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String leadId,
      @Valid @RequestBody CreateTicketRequest request) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return store.addTicket(leadId, request.subject(), request.category(), request.priority());
  }

  @GetMapping("/notes")
  public List<AgentNote> notes(
      @RequestHeader("X-CRM-Token") String token, @PathVariable String leadId) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return store.notesForLead(leadId);
  }

  @PostMapping("/notes")
  public AgentNote createNote(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String leadId,
      @Valid @RequestBody CreateNoteRequest request) {
    StaffUser user = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    AgentNote note =
        new AgentNote(
            "note-" + UUID.randomUUID(),
            leadId,
            user.id(),
            request.disposition(),
            request.note(),
            request.followUpAt(),
            Instant.now());
    return store.addNote(note);
  }
}
