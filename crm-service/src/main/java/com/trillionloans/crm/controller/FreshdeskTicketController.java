package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.CrmModels.FreshdeskAgentTicket;
import com.trillionloans.crm.model.CrmModels.FreshdeskTicketBucketResponse;
import com.trillionloans.crm.model.CrmModels.FreshdeskTicketReplyRequest;
import com.trillionloans.crm.model.CrmModels.FreshdeskTicketUpdateRequest;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.model.CrmModels.TicketPriority;
import com.trillionloans.crm.service.AgentFreshdeskTicketService;
import com.trillionloans.crm.service.AuthService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm/freshdesk/tickets")
public class FreshdeskTicketController {

  private final AuthService authService;
  private final AgentFreshdeskTicketService agentFreshdeskTicketService;

  public FreshdeskTicketController(
      AuthService authService, AgentFreshdeskTicketService agentFreshdeskTicketService) {
    this.authService = authService;
    this.agentFreshdeskTicketService = agentFreshdeskTicketService;
  }

  @GetMapping("/my-bucket")
  public FreshdeskTicketBucketResponse myBucket(
      @RequestHeader("X-CRM-Token") String token,
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String priority,
      @RequestParam(required = false) String mobileNumber,
      @RequestParam(required = false) String loanAccountNumber,
      @RequestParam(required = false) String createdFrom,
      @RequestParam(required = false) String createdTo,
      @RequestParam(required = false) String closedFrom,
      @RequestParam(required = false) String closedTo) {
    StaffUser agent = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return buildBucketResponse(
        agent,
        query,
        priority,
        mobileNumber,
        loanAccountNumber,
        createdFrom,
        createdTo,
        closedFrom,
        closedTo);
  }

  @GetMapping("/org-bucket")
  public FreshdeskTicketBucketResponse orgBucket(
      @RequestHeader("X-CRM-Token") String token,
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String priority,
      @RequestParam(required = false) String mobileNumber,
      @RequestParam(required = false) String loanAccountNumber,
      @RequestParam(required = false) String createdFrom,
      @RequestParam(required = false) String createdTo,
      @RequestParam(required = false) String closedFrom,
      @RequestParam(required = false) String closedTo) {
    authService.requireAnyRole(token, Role.ADMIN);
    return agentFreshdeskTicketService.listOrgBucket(
        query,
        AgentFreshdeskTicketService.parsePriority(priority),
        mobileNumber,
        loanAccountNumber,
        AgentFreshdeskTicketService.parseInstantParam(createdFrom),
        AgentFreshdeskTicketService.parseInstantParam(createdTo),
        AgentFreshdeskTicketService.parseInstantParam(closedFrom),
        AgentFreshdeskTicketService.parseInstantParam(closedTo));
  }

  @GetMapping("/team-bucket")
  public FreshdeskTicketBucketResponse teamBucket(
      @RequestHeader("X-CRM-Token") String token,
      @RequestParam(required = false) String leadId,
      @RequestParam(required = false) String query,
      @RequestParam(required = false) String priority,
      @RequestParam(required = false) String mobileNumber,
      @RequestParam(required = false) String loanAccountNumber,
      @RequestParam(required = false) String createdFrom,
      @RequestParam(required = false) String createdTo,
      @RequestParam(required = false) String closedFrom,
      @RequestParam(required = false) String closedTo) {
    StaffUser user = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD);
    String resolvedLeadId = user.role() == Role.LEAD ? user.id() : leadId;
    return agentFreshdeskTicketService.listTeamBucket(
        resolvedLeadId,
        query,
        AgentFreshdeskTicketService.parsePriority(priority),
        mobileNumber,
        loanAccountNumber,
        AgentFreshdeskTicketService.parseInstantParam(createdFrom),
        AgentFreshdeskTicketService.parseInstantParam(createdTo),
        AgentFreshdeskTicketService.parseInstantParam(closedFrom),
        AgentFreshdeskTicketService.parseInstantParam(closedTo));
  }

  private FreshdeskTicketBucketResponse buildBucketResponse(
      StaffUser agent,
      String query,
      String priority,
      String mobileNumber,
      String loanAccountNumber,
      String createdFrom,
      String createdTo,
      String closedFrom,
      String closedTo) {
    TicketPriority parsedPriority = AgentFreshdeskTicketService.parsePriority(priority);
    Instant parsedCreatedFrom = AgentFreshdeskTicketService.parseInstantParam(createdFrom);
    Instant parsedCreatedTo = AgentFreshdeskTicketService.parseInstantParam(createdTo);
    Instant parsedClosedFrom = AgentFreshdeskTicketService.parseInstantParam(closedFrom);
    Instant parsedClosedTo = AgentFreshdeskTicketService.parseInstantParam(closedTo);
    return agentFreshdeskTicketService.listBucket(
        agent,
        query,
        parsedPriority,
        mobileNumber,
        loanAccountNumber,
        parsedCreatedFrom,
        parsedCreatedTo,
        parsedClosedFrom,
        parsedClosedTo);
  }

  @PostMapping("/sync")
  public FreshdeskTicketBucketResponse sync(@RequestHeader("X-CRM-Token") String token) {
    StaffUser agent = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return agentFreshdeskTicketService.syncFromFreshdesk(agent);
  }

  @PostMapping("/{ticketId}/reply")
  public FreshdeskAgentTicket reply(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String ticketId,
      @Valid @RequestBody FreshdeskTicketReplyRequest request) {
    StaffUser agent = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return agentFreshdeskTicketService.addReply(agent, ticketId, request.body());
  }

  @PatchMapping("/{ticketId}")
  public FreshdeskAgentTicket update(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String ticketId,
      @RequestBody FreshdeskTicketUpdateRequest request) {
    StaffUser agent = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return agentFreshdeskTicketService.updateTicket(
        agent, ticketId, request.status(), request.priority());
  }
}
