package com.trillionloans.crm.service;

import com.trillionloans.crm.model.CrmModels.AgentOpsRow;
import com.trillionloans.crm.model.CrmModels.CrmBucketSummary;
import com.trillionloans.crm.model.CrmModels.CrmLead;
import com.trillionloans.crm.model.CrmModels.FreshdeskAgentTicket;
import com.trillionloans.crm.model.CrmModels.FreshdeskBucketSummary;
import com.trillionloans.crm.model.CrmModels.IntegrationsHealthResponse;
import com.trillionloans.crm.model.CrmModels.LeadStatus;
import com.trillionloans.crm.model.CrmModels.OpsOverviewResponse;
import com.trillionloans.crm.model.CrmModels.OpsRateMetrics;
import com.trillionloans.crm.model.CrmModels.OpsTicketRow;
import com.trillionloans.crm.model.CrmModels.OpsTicketSource;
import com.trillionloans.crm.model.CrmModels.OpsTicketsPage;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.model.CrmModels.TeamOpsRow;
import com.trillionloans.crm.model.CrmModels.TicketStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminOpsService {

  private final CrmStore store;
  private final AgentFreshdeskTicketService freshdeskTicketService;
  private final ExotelCallSyncService exotelCallSyncService;
  private final boolean useLiveIntegrations;

  public AdminOpsService(
      CrmStore store,
      AgentFreshdeskTicketService freshdeskTicketService,
      ExotelCallSyncService exotelCallSyncService,
      @Value("${crm.integrations.use-live-data:true}") boolean useLiveIntegrations) {
    this.store = store;
    this.freshdeskTicketService = freshdeskTicketService;
    this.exotelCallSyncService = exotelCallSyncService;
    this.useLiveIntegrations = useLiveIntegrations;
  }

  public OpsOverviewResponse getOverview(StaffUser user, Instant from, Instant to) {
    Instant rangeFrom = from != null ? from : Instant.now().minus(Duration.ofDays(7));
    Instant rangeTo = to != null ? to : Instant.now();
    Scope scope = resolveScope(user, null);

    List<CrmLead> crmLeads = scopedCrmLeads(scope);
    List<FreshdeskAgentTicket> fdTickets = scopedFreshdeskTickets(scope);

    CrmBucketSummary crmBuckets = summarizeCrm(crmLeads);
    FreshdeskBucketSummary fdBuckets = summarizeFreshdesk(fdTickets);
    OpsRateMetrics rates = computeRates(crmLeads, fdTickets, rangeFrom, rangeTo);

    List<TeamOpsRow> teams = buildTeamRows(scope, rangeFrom, rangeTo);
    List<AgentOpsRow> agents = buildAgentRows(scope, rangeFrom, rangeTo);

    return new OpsOverviewResponse(
        rangeFrom, rangeTo, crmBuckets, fdBuckets, rates, teams, agents);
  }

  public List<TeamOpsRow> getTeams(StaffUser user, Instant from, Instant to) {
    Instant rangeFrom = from != null ? from : Instant.now().minus(Duration.ofDays(7));
    Instant rangeTo = to != null ? to : Instant.now();
    return buildTeamRows(resolveScope(user, null), rangeFrom, rangeTo);
  }

  public List<AgentOpsRow> getAgents(StaffUser user, Instant from, Instant to, String leadId) {
    Instant rangeFrom = from != null ? from : Instant.now().minus(Duration.ofDays(7));
    Instant rangeTo = to != null ? to : Instant.now();
    return buildAgentRows(resolveScope(user, leadId), rangeFrom, rangeTo);
  }

  public OpsTicketsPage getTickets(
      StaffUser user,
      String source,
      String status,
      String leadId,
      String agentId,
      String mobileNumber,
      String loanAccountNumber,
      Instant from,
      Instant to,
      int page,
      int size) {
    Scope scope = resolveScope(user, leadId);
    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);

    List<OpsTicketRow> rows = new ArrayList<>();
    String normalizedSource = source == null ? "BOTH" : source.toUpperCase(Locale.ROOT);

    if ("BOTH".equals(normalizedSource) || "CRM".equals(normalizedSource)) {
      scopedCrmLeads(scope).stream()
          .filter(lead -> matchesAgentFilter(lead, agentId, scope))
          .filter(lead -> status == null || status.isBlank() || lead.status().name().equalsIgnoreCase(status))
          .filter(lead -> mobileNumber == null || mobileNumber.isBlank()
              || contains(lead.mobileNumber(), mobileNumber))
          .filter(lead -> loanAccountNumber == null || loanAccountNumber.isBlank()
              || contains(lead.loanAccountNumber(), loanAccountNumber))
          .filter(lead -> from == null || !lead.createdAt().isBefore(from))
          .filter(lead -> to == null || !lead.createdAt().isAfter(to))
          .map(this::toCrmTicketRow)
          .forEach(rows::add);
    }

    if ("BOTH".equals(normalizedSource) || "FRESHDESK".equals(normalizedSource)) {
      scopedFreshdeskTickets(scope).stream()
          .filter(ticket -> matchesFdAgentFilter(ticket, agentId, scope))
          .filter(ticket -> status == null || status.isBlank() || ticket.status().name().equalsIgnoreCase(status))
          .filter(ticket -> mobileNumber == null || mobileNumber.isBlank()
              || contains(ticket.mobileNumber(), mobileNumber))
          .filter(ticket -> loanAccountNumber == null || loanAccountNumber.isBlank()
              || contains(ticket.loanAccountNumber(), loanAccountNumber))
          .filter(ticket -> from == null || !ticket.createdAt().isBefore(from))
          .filter(ticket -> to == null || !ticket.createdAt().isAfter(to))
          .map(this::toFreshdeskTicketRow)
          .forEach(rows::add);
    }

    rows.sort(Comparator.comparing(OpsTicketRow::updatedAt).reversed());
    long total = rows.size();
    int fromIndex = Math.min(safePage * safeSize, rows.size());
    int toIndex = Math.min(fromIndex + safeSize, rows.size());
    return new OpsTicketsPage(rows.subList(fromIndex, toIndex), total, safePage, safeSize);
  }

  public IntegrationsHealthResponse getIntegrationsHealth() {
    boolean exotelConfigured = exotelCallSyncService.isConfigured();
    String exotelStatus =
        exotelConfigured
            ? "Configured — daily T-1 sync enabled"
            : "Not configured — set EXOTEL credentials";
    return new IntegrationsHealthResponse(
        freshdeskTicketService.isFreshdeskConfigured(),
        freshdeskTicketService.getLastSyncedAt(),
        exotelConfigured,
        exotelStatus,
        useLiveIntegrations);
  }

  private List<TeamOpsRow> buildTeamRows(Scope scope, Instant from, Instant to) {
    List<StaffUser> leads =
        scope.leadUserId() == null
            ? store.activeLeads()
            : store.activeLeads().stream()
                .filter(lead -> lead.id().equals(scope.leadUserId()))
                .toList();

    return leads.stream()
        .map(
            lead -> {
              List<StaffUser> agents = store.agentsForLead(lead.id());
              List<CrmLead> teamCrm =
                  store.leadsForLead(lead.id()).stream().filter(leadItem -> inScope(scope, leadItem)).toList();
              Set<String> teamEmails =
                  agents.stream().map(agent -> agent.email().toLowerCase()).collect(Collectors.toSet());
              List<FreshdeskAgentTicket> teamFd =
                  freshdeskTicketService.listAllTickets().stream()
                      .filter(
                          ticket ->
                              ticket.assigneeEmail() != null
                                  && teamEmails.contains(ticket.assigneeEmail().toLowerCase()))
                      .toList();

              long openCrm = teamCrm.stream().filter(leadItem -> leadItem.status() != LeadStatus.CLOSED).count();
              long openFd =
                  teamFd.stream()
                      .filter(
                          ticket ->
                              ticket.status() != TicketStatus.CLOSED
                                  && ticket.status() != TicketStatus.RESOLVED)
                      .count();
              long escalations =
                  teamCrm.stream().filter(leadItem -> leadItem.status() == LeadStatus.ESCALATED).count();

              OpsRateMetrics teamRates = computeRates(teamCrm, teamFd, from, to);
              return new TeamOpsRow(
                  lead.id(),
                  lead.name(),
                  agents.size(),
                  openCrm,
                  openFd,
                  teamRates.crmResolutionRate(),
                  teamRates.freshdeskResolutionRate(),
                  escalations);
            })
        .toList();
  }

  private List<AgentOpsRow> buildAgentRows(Scope scope, Instant from, Instant to) {
    List<StaffUser> agents =
        scope.leadUserId() == null
            ? store.activeAgents()
            : store.agentsForLead(scope.leadUserId());

    if (scope.agentIds() != null && !scope.agentIds().isEmpty()) {
      agents =
          agents.stream().filter(agent -> scope.agentIds().contains(agent.id())).toList();
    }

    Instant sevenDaysAgo = Instant.now().minus(Duration.ofDays(7));

    return agents.stream()
        .map(
            agent -> {
              StaffUser teamLead =
                  agent.leadId() == null ? null : store.getUser(agent.leadId());
              List<CrmLead> agentCrm =
                  store.leadsForAgent(agent.id()).stream().filter(lead -> inScope(scope, lead)).toList();
              List<FreshdeskAgentTicket> agentFd =
                  freshdeskTicketService.listAllTickets().stream()
                      .filter(
                          ticket ->
                              ticket.assigneeEmail() != null
                                  && ticket.assigneeEmail().equalsIgnoreCase(agent.email()))
                      .toList();

              long openCrm = agentCrm.stream().filter(lead -> lead.status() != LeadStatus.CLOSED).count();
              long openFd =
                  agentFd.stream()
                      .filter(
                          ticket ->
                              ticket.status() != TicketStatus.CLOSED
                                  && ticket.status() != TicketStatus.RESOLVED)
                      .count();
              long resolvedCrm7d =
                  agentCrm.stream()
                      .filter(lead -> lead.status() == LeadStatus.CLOSED)
                      .filter(lead -> lead.updatedAt().isAfter(sevenDaysAgo))
                      .count();
              long resolvedFd7d =
                  agentFd.stream()
                      .filter(
                          ticket ->
                              ticket.status() == TicketStatus.RESOLVED
                                  || ticket.status() == TicketStatus.CLOSED)
                      .filter(
                          ticket ->
                              ticket.closedAt() != null && ticket.closedAt().isAfter(sevenDaysAgo))
                      .count();

              Double avgHandleHours = averageCloseHours(agentCrm, agentFd);
              return new AgentOpsRow(
                  agent.id(),
                  agent.name(),
                  teamLead == null ? null : teamLead.id(),
                  teamLead == null ? null : teamLead.name(),
                  openCrm,
                  openFd,
                  resolvedCrm7d,
                  resolvedFd7d,
                  avgHandleHours);
            })
        .toList();
  }

  private OpsRateMetrics computeRates(
      List<CrmLead> crmLeads, List<FreshdeskAgentTicket> fdTickets, Instant from, Instant to) {
    long crmCreatedInPeriod =
        crmLeads.stream()
            .filter(lead -> !lead.createdAt().isBefore(from) && !lead.createdAt().isAfter(to))
            .count();
    long crmResolvedInPeriod =
        crmLeads.stream()
            .filter(lead -> lead.status() == LeadStatus.CLOSED)
            .filter(lead -> !lead.updatedAt().isBefore(from) && !lead.updatedAt().isAfter(to))
            .count();

    long fdCreatedInPeriod =
        fdTickets.stream()
            .filter(ticket -> !ticket.createdAt().isBefore(from) && !ticket.createdAt().isAfter(to))
            .count();
    long fdResolvedInPeriod =
        fdTickets.stream()
            .filter(
                ticket ->
                    ticket.status() == TicketStatus.RESOLVED
                        || ticket.status() == TicketStatus.CLOSED)
            .filter(
                ticket ->
                    ticket.closedAt() != null
                        && !ticket.closedAt().isBefore(from)
                        && !ticket.closedAt().isAfter(to))
            .count();

    long crmOpenBacklog =
        crmLeads.stream().filter(lead -> lead.status() != LeadStatus.CLOSED).count();
    long fdOpenBacklog =
        fdTickets.stream()
            .filter(
                ticket ->
                    ticket.status() != TicketStatus.CLOSED
                        && ticket.status() != TicketStatus.RESOLVED)
            .count();
    long escalations =
        crmLeads.stream().filter(lead -> lead.status() == LeadStatus.ESCALATED).count();

    double crmResolutionRate = percentage(crmResolvedInPeriod, crmCreatedInPeriod);
    double fdResolutionRate = percentage(fdResolvedInPeriod, fdCreatedInPeriod);
    double escalationRate = percentage(escalations, Math.max(crmLeads.size(), 1));

    return new OpsRateMetrics(
        crmResolutionRate,
        fdResolutionRate,
        escalationRate,
        crmOpenBacklog,
        fdOpenBacklog,
        averageCrmCloseHours(crmLeads),
        averageFdCloseHours(fdTickets));
  }

  private CrmBucketSummary summarizeCrm(List<CrmLead> leads) {
    return new CrmBucketSummary(
        leads.size(),
        countCrmStatus(leads, LeadStatus.NEW),
        countCrmStatus(leads, LeadStatus.ASSIGNED),
        countCrmStatus(leads, LeadStatus.IN_PROGRESS),
        countCrmStatus(leads, LeadStatus.FOLLOW_UP),
        countCrmStatus(leads, LeadStatus.ESCALATED),
        countCrmStatus(leads, LeadStatus.CLOSED));
  }

  private FreshdeskBucketSummary summarizeFreshdesk(List<FreshdeskAgentTicket> tickets) {
    return new FreshdeskBucketSummary(
        tickets.size(),
        tickets.stream().filter(ticket -> ticket.status() == TicketStatus.OPEN).count(),
        tickets.stream().filter(ticket -> ticket.status() == TicketStatus.PENDING).count(),
        tickets.stream().filter(ticket -> ticket.status() == TicketStatus.RESOLVED).count(),
        tickets.stream().filter(ticket -> ticket.status() == TicketStatus.CLOSED).count());
  }

  private OpsTicketRow toCrmTicketRow(CrmLead lead) {
    StaffUser agent =
        lead.assignedAgentId() == null ? null : safeUser(lead.assignedAgentId()).orElse(null);
    StaffUser teamLead =
        lead.assignedLeadId() == null ? null : safeUser(lead.assignedLeadId()).orElse(null);
    return new OpsTicketRow(
        lead.id(),
        OpsTicketSource.CRM,
        lead.leadId(),
        lead.title(),
        lead.status().name(),
        lead.priority().name(),
        agent == null ? null : agent.id(),
        agent == null ? null : agent.name(),
        teamLead == null ? null : teamLead.id(),
        teamLead == null ? null : teamLead.name(),
        lead.mobileNumber(),
        lead.loanAccountNumber(),
        lead.createdAt(),
        lead.updatedAt(),
        lead.status() == LeadStatus.CLOSED ? lead.updatedAt() : null);
  }

  private OpsTicketRow toFreshdeskTicketRow(FreshdeskAgentTicket ticket) {
    StaffUser agent =
        ticket.assigneeEmail() == null
            ? null
            : store.listUsers().stream()
                .filter(user -> user.email().equalsIgnoreCase(ticket.assigneeEmail()))
                .findFirst()
                .orElse(null);
    StaffUser teamLead =
        agent == null || agent.leadId() == null ? null : safeUser(agent.leadId()).orElse(null);
    return new OpsTicketRow(
        ticket.id(),
        OpsTicketSource.FRESHDESK,
        ticket.leadId(),
        ticket.subject(),
        ticket.status().name(),
        ticket.priority().name(),
        agent == null ? null : agent.id(),
        ticket.assigneeName(),
        teamLead == null ? null : teamLead.id(),
        teamLead == null ? null : teamLead.name(),
        ticket.mobileNumber(),
        ticket.loanAccountNumber(),
        ticket.createdAt(),
        ticket.updatedAt(),
        ticket.closedAt());
  }

  private Scope resolveScope(StaffUser user, String requestedLeadId) {
    if (user.role() == Role.LEAD) {
      return Scope.team(user.id());
    }
    if (user.role() == Role.ADMIN && StringUtils.hasText(requestedLeadId)) {
      return Scope.team(requestedLeadId);
    }
    return Scope.org();
  }

  private List<CrmLead> scopedCrmLeads(Scope scope) {
    List<CrmLead> leads = scope.leadUserId() == null ? store.listLeads() : store.leadsForLead(scope.leadUserId());
    return leads.stream().filter(lead -> inScope(scope, lead)).toList();
  }

  private List<FreshdeskAgentTicket> scopedFreshdeskTickets(Scope scope) {
    if (scope.leadUserId() == null) {
      return freshdeskTicketService.listAllTickets();
    }
    Set<String> teamEmails =
        store.agentsForLead(scope.leadUserId()).stream()
            .map(agent -> agent.email().toLowerCase())
            .collect(Collectors.toSet());
    return freshdeskTicketService.listAllTickets().stream()
        .filter(
            ticket ->
                ticket.assigneeEmail() != null
                    && teamEmails.contains(ticket.assigneeEmail().toLowerCase()))
        .toList();
  }

  private boolean inScope(Scope scope, CrmLead lead) {
    if (scope.leadUserId() == null) {
      return true;
    }
    return scope.leadUserId().equals(lead.assignedLeadId());
  }

  private boolean matchesAgentFilter(CrmLead lead, String agentId, Scope scope) {
    if (!StringUtils.hasText(agentId)) {
      return true;
    }
    return agentId.equals(lead.assignedAgentId());
  }

  private boolean matchesFdAgentFilter(FreshdeskAgentTicket ticket, String agentId, Scope scope) {
    if (!StringUtils.hasText(agentId)) {
      return true;
    }
    StaffUser agent = safeUser(agentId).orElse(null);
    return agent != null
        && ticket.assigneeEmail() != null
        && ticket.assigneeEmail().equalsIgnoreCase(agent.email());
  }

  private long countCrmStatus(List<CrmLead> leads, LeadStatus status) {
    return leads.stream().filter(lead -> lead.status() == status).count();
  }

  private double percentage(long numerator, long denominator) {
    if (denominator <= 0) {
      return 0.0;
    }
    return Math.round((numerator * 10000.0) / denominator) / 100.0;
  }

  private Double averageCrmCloseHours(List<CrmLead> leads) {
    List<CrmLead> closed =
        leads.stream().filter(lead -> lead.status() == LeadStatus.CLOSED).toList();
    if (closed.isEmpty()) {
      return null;
    }
    double totalHours =
        closed.stream()
            .mapToDouble(
                lead -> Duration.between(lead.createdAt(), lead.updatedAt()).toMinutes() / 60.0)
            .sum();
    return Math.round((totalHours / closed.size()) * 10.0) / 10.0;
  }

  private Double averageFdCloseHours(List<FreshdeskAgentTicket> tickets) {
    List<FreshdeskAgentTicket> closed =
        tickets.stream()
            .filter(
                ticket ->
                    ticket.closedAt() != null
                        && (ticket.status() == TicketStatus.RESOLVED
                            || ticket.status() == TicketStatus.CLOSED))
            .toList();
    if (closed.isEmpty()) {
      return null;
    }
    double totalHours =
        closed.stream()
            .mapToDouble(
                ticket ->
                    Duration.between(ticket.createdAt(), ticket.closedAt()).toMinutes() / 60.0)
            .sum();
    return Math.round((totalHours / closed.size()) * 10.0) / 10.0;
  }

  private Double averageCloseHours(List<CrmLead> crmLeads, List<FreshdeskAgentTicket> fdTickets) {
    Double crmAvg = averageCrmCloseHours(crmLeads);
    Double fdAvg = averageFdCloseHours(fdTickets);
    if (crmAvg == null && fdAvg == null) {
      return null;
    }
    if (crmAvg == null) {
      return fdAvg;
    }
    if (fdAvg == null) {
      return crmAvg;
    }
    return Math.round(((crmAvg + fdAvg) / 2.0) * 10.0) / 10.0;
  }

  private Optional<StaffUser> safeUser(String userId) {
    try {
      return Optional.of(store.getUser(userId));
    } catch (NotFoundException exception) {
      return Optional.empty();
    }
  }

  private boolean contains(String value, String query) {
    return value != null && value.toLowerCase().contains(query.toLowerCase());
  }

  private record Scope(String leadUserId, Set<String> agentIds) {
    static Scope org() {
      return new Scope(null, null);
    }

    static Scope team(String leadUserId) {
      return new Scope(leadUserId, null);
    }
  }
}
