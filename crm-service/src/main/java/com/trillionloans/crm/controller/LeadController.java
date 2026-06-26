package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.CrmModels.AssignLeadRequest;
import com.trillionloans.crm.model.CrmModels.CrmBucketSummary;
import com.trillionloans.crm.model.CrmModels.CrmLead;
import com.trillionloans.crm.model.CrmModels.LeadIngestRequest;
import com.trillionloans.crm.model.CrmModels.LeadStatus;
import com.trillionloans.crm.model.CrmModels.QueueSummary;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.SearchResult;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.model.CrmModels.TeamQueueResponse;
import com.trillionloans.crm.model.CrmModels.UpdateLeadStatusRequest;
import com.trillionloans.crm.service.AuthService;
import com.trillionloans.crm.service.CrmStore;
import com.trillionloans.crm.service.ExternalDataService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
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
@RequestMapping("/crm")
public class LeadController {

  private static final Pattern MOBILE_PATTERN = Pattern.compile("^\\d{10}$");

  private final AuthService authService;
  private final CrmStore store;
  private final ExternalDataService externalDataService;

  public LeadController(
      AuthService authService, CrmStore store, ExternalDataService externalDataService) {
    this.authService = authService;
    this.store = store;
    this.externalDataService = externalDataService;
  }

  @GetMapping("/leads/my-queue")
  public List<CrmLead> myQueue(@RequestHeader("X-CRM-Token") String token) {
    StaffUser user = authService.requireUser(token);
    if (user.role() == Role.AGENT) {
      return store.leadsForAgent(user.id());
    }
    if (user.role() == Role.LEAD) {
      return store.leadsForLead(user.id());
    }
    return store.listLeads();
  }

  @GetMapping("/leads/team-queue")
  public TeamQueueResponse teamQueue(@RequestHeader("X-CRM-Token") String token) {
    StaffUser user = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD);
    List<CrmLead> leads = user.role() == Role.LEAD ? store.leadsForLead(user.id()) : store.listLeads();
    long followUps = leads.stream().filter(lead -> lead.status() == LeadStatus.FOLLOW_UP).count();
    long escalated = leads.stream().filter(lead -> lead.status() == LeadStatus.ESCALATED).count();
    long newItems = leads.stream().filter(lead -> lead.status() == LeadStatus.NEW).count();
    CrmBucketSummary crmBuckets =
        new CrmBucketSummary(
            leads.size(),
            leads.stream().filter(lead -> lead.status() == LeadStatus.NEW).count(),
            leads.stream().filter(lead -> lead.status() == LeadStatus.ASSIGNED).count(),
            leads.stream().filter(lead -> lead.status() == LeadStatus.IN_PROGRESS).count(),
            followUps,
            escalated,
            leads.stream().filter(lead -> lead.status() == LeadStatus.CLOSED).count());
    return new TeamQueueResponse(
        user.role() == Role.LEAD ? store.agentsForLead(user.id()) : store.activeAgents(),
        leads,
        new QueueSummary(leads.size(), newItems, followUps, escalated),
        crmBuckets);
  }

  @PostMapping("/leads/ingest")
  public CrmLead ingest(
      @RequestHeader("X-CRM-Token") String token, @Valid @RequestBody LeadIngestRequest request) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return store.ingestLead(request);
  }

  @PostMapping("/leads/{crmLeadId}/assign")
  public CrmLead assign(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String crmLeadId,
      @Valid @RequestBody AssignLeadRequest request) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD);
    return store.assignLead(crmLeadId, request.agentId());
  }

  @PatchMapping("/leads/{crmLeadId}/status")
  public CrmLead updateStatus(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String crmLeadId,
      @Valid @RequestBody UpdateLeadStatusRequest request) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return store.updateLeadStatus(crmLeadId, request.status());
  }

  @GetMapping("/search")
  @Deprecated
  public List<SearchResult> search(
      @RequestHeader("X-CRM-Token") String token,
      @RequestParam String query,
      @RequestParam(required = false, defaultValue = "all") String type) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    String trimmedQuery = query == null ? "" : query.trim();
    if (trimmedQuery.isBlank()) {
      return List.of();
    }

    Map<String, SearchResult> results = new LinkedHashMap<>();

    store.searchLeads(trimmedQuery).stream()
        .map(
            lead ->
                new SearchResult(
                    type,
                    lead.leadId(),
                    lead.clientId(),
                    lead.mobileNumber(),
                    lead.loanAccountNumber(),
                    lead.loanApplicationId(),
                    lead.title() + " · " + lead.mobileNumber(),
                    matchedOn(lead, trimmedQuery)))
        .forEach(result -> results.put(result.leadId(), result));

    if (MOBILE_PATTERN.matcher(trimmedQuery).matches()) {
      externalDataService.searchLeadIdsByMobile(trimmedQuery).stream()
          .filter(leadId -> !results.containsKey(leadId))
          .map(
              leadId -> {
                var profile = externalDataService.getCustomerProfile(leadId);
                return new SearchResult(
                    type,
                    leadId,
                    profile.clientId(),
                    profile.mobileNo(),
                    null,
                    null,
                    profile.name() + " · " + profile.mobileNo(),
                    "mobileNumber");
              })
          .forEach(result -> results.put(result.leadId(), result));
    }

    return new ArrayList<>(results.values());
  }

  private String matchedOn(CrmLead lead, String query) {
    if (lead.leadId() != null && lead.leadId().contains(query)) {
      return "leadId";
    }
    if (lead.mobileNumber() != null && lead.mobileNumber().contains(query)) {
      return "mobileNumber";
    }
    if (lead.loanAccountNumber() != null && lead.loanAccountNumber().contains(query)) {
      return "loanAccountNumber";
    }
    if (lead.loanApplicationId() != null && lead.loanApplicationId().contains(query)) {
      return "loanApplicationId";
    }
    return "clientId";
  }
}
