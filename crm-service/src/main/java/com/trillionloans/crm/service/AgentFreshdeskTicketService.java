package com.trillionloans.crm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.trillionloans.crm.model.CrmModels.FreshdeskAgentTicket;
import com.trillionloans.crm.model.CrmModels.FreshdeskBucketSummary;
import com.trillionloans.crm.model.CrmModels.FreshdeskConversationEntry;
import com.trillionloans.crm.model.CrmModels.FreshdeskTicketBucketResponse;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.model.CrmModels.TicketPriority;
import com.trillionloans.crm.model.CrmModels.TicketStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentFreshdeskTicketService {

  private static final Logger log = LoggerFactory.getLogger(AgentFreshdeskTicketService.class);

  private final FreshdeskTicketService freshdeskTicketService;
  private final CrmStore crmStore;
  private final Map<String, FreshdeskAgentTicket> ticketStore = new ConcurrentHashMap<>();
  private Instant lastSyncedAt = Instant.now();

  public AgentFreshdeskTicketService(
      FreshdeskTicketService freshdeskTicketService, CrmStore crmStore) {
    this.freshdeskTicketService = freshdeskTicketService;
    this.crmStore = crmStore;
    seedMockTickets();
  }

  public Instant getLastSyncedAt() {
    return lastSyncedAt;
  }

  public boolean isFreshdeskConfigured() {
    return freshdeskTicketService.isConfigured();
  }

  public String freshdeskBaseUrl() {
    return freshdeskTicketService.baseUrl();
  }

  public List<FreshdeskAgentTicket> listAllTickets() {
    return ticketStore.values().stream()
        .sorted(Comparator.comparing(FreshdeskAgentTicket::updatedAt).reversed())
        .collect(Collectors.toList());
  }

  public Optional<FreshdeskAgentTicket> findById(String ticketId) {
    return Optional.ofNullable(ticketStore.get(ticketId));
  }

  public FreshdeskTicketBucketResponse listBucket(
      StaffUser agent,
      String query,
      TicketPriority priority,
      String mobileNumber,
      String loanAccountNumber,
      Instant createdFrom,
      Instant createdTo,
      Instant closedFrom,
      Instant closedTo) {
    ensureSynced(agent);
    return listScopedBucket(
        BucketScope.forUser(agent),
        agent,
        null,
        query,
        priority,
        mobileNumber,
        loanAccountNumber,
        createdFrom,
        createdTo,
        closedFrom,
        closedTo);
  }

  public FreshdeskTicketBucketResponse listOrgBucket(
      String query,
      TicketPriority priority,
      String mobileNumber,
      String loanAccountNumber,
      Instant createdFrom,
      Instant createdTo,
      Instant closedFrom,
      Instant closedTo) {
    return listScopedBucket(
        BucketScope.ORG,
        null,
        null,
        query,
        priority,
        mobileNumber,
        loanAccountNumber,
        createdFrom,
        createdTo,
        closedFrom,
        closedTo);
  }

  public FreshdeskTicketBucketResponse listTeamBucket(
      String leadUserId,
      String query,
      TicketPriority priority,
      String mobileNumber,
      String loanAccountNumber,
      Instant createdFrom,
      Instant createdTo,
      Instant closedFrom,
      Instant closedTo) {
    return listScopedBucket(
        BucketScope.TEAM,
        null,
        leadUserId,
        query,
        priority,
        mobileNumber,
        loanAccountNumber,
        createdFrom,
        createdTo,
        closedFrom,
        closedTo);
  }

  private FreshdeskTicketBucketResponse listScopedBucket(
      BucketScope scope,
      StaffUser agent,
      String leadUserId,
      String query,
      TicketPriority priority,
      String mobileNumber,
      String loanAccountNumber,
      Instant createdFrom,
      Instant createdTo,
      Instant closedFrom,
      Instant closedTo) {
    Set<String> teamEmails = teamEmailsForScope(scope, agent, leadUserId);
    List<FreshdeskAgentTicket> tickets =
        ticketStore.values().stream()
            .filter(ticket -> matchesScope(ticket, scope, agent, teamEmails))
            .filter(ticket -> matchesQuery(ticket, query))
            .filter(ticket -> priority == null || ticket.priority() == priority)
            .filter(ticket -> mobileNumber == null || mobileNumber.isBlank()
                || containsIgnoreCase(ticket.mobileNumber(), mobileNumber))
            .filter(ticket -> loanAccountNumber == null || loanAccountNumber.isBlank()
                || containsIgnoreCase(ticket.loanAccountNumber(), loanAccountNumber))
            .filter(ticket -> createdFrom == null || !ticket.createdAt().isBefore(createdFrom))
            .filter(ticket -> createdTo == null || !ticket.createdAt().isAfter(createdTo))
            .filter(ticket -> closedFrom == null
                || (ticket.closedAt() != null && !ticket.closedAt().isBefore(closedFrom)))
            .filter(ticket -> closedTo == null
                || (ticket.closedAt() != null && !ticket.closedAt().isAfter(closedTo)))
            .sorted(Comparator.comparing(FreshdeskAgentTicket::updatedAt).reversed())
            .collect(Collectors.toList());

    return new FreshdeskTicketBucketResponse(
        tickets, summarize(tickets), lastSyncedAt, freshdeskTicketService.isConfigured());
  }

  public FreshdeskTicketBucketResponse syncFromFreshdesk(StaffUser agent) {
    ensureSynced(agent);
    return listBucket(agent, null, null, null, null, null, null, null, null);
  }

  private void ensureSynced(StaffUser agent) {
    if (freshdeskTicketService.isConfigured()) {
      try {
        List<FreshdeskAgentTicket> remoteTickets =
            freshdeskTicketService.fetchAgentTickets(agent.email());
        remoteTickets.forEach(ticket -> ticketStore.put(ticket.id(), ticket));
      } catch (RuntimeException exception) {
        log.warn("Freshdesk sync failed for agent {}", agent.email(), exception);
      }
    }
    lastSyncedAt = Instant.now();
  }

  public List<FreshdeskConversationEntry> getConversations(String ticketId) {
    FreshdeskAgentTicket ticket =
        ticketStore.values().stream()
            .filter(item -> item.id().equals(ticketId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Freshdesk ticket not found"));

    if (freshdeskTicketService.isConfigured()) {
      List<FreshdeskConversationEntry> remote =
          freshdeskTicketService.fetchConversations(ticket.freshdeskId());
      if (!remote.isEmpty()) {
        FreshdeskAgentTicket updated =
            copyTicket(
                ticket,
                ticket.status(),
                ticket.priority(),
                remote,
                ticket.closedAt(),
                remote.size());
        ticketStore.put(updated.id(), updated);
        return remote;
      }
    }
    return ticket.conversations();
  }

  public FreshdeskAgentTicket linkCustomer(
      StaffUser agent, String ticketId, String leadId, String mobileNumber, String loanAccountNumber) {
    FreshdeskAgentTicket ticket =
        ticketStore.values().stream()
            .filter(item -> item.id().equals(ticketId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Freshdesk ticket not found"));

    if (freshdeskTicketService.isConfigured()) {
      freshdeskTicketService.linkCustomer(
          ticket.freshdeskId(), leadId, mobileNumber, loanAccountNumber);
    }

    FreshdeskAgentTicket updated =
        new FreshdeskAgentTicket(
            ticket.id(),
            ticket.freshdeskId(),
            leadId,
            ticket.subject(),
            ticket.requesterName(),
            ticket.requesterEmail(),
            mobileNumber != null ? mobileNumber : ticket.mobileNumber(),
            loanAccountNumber != null ? loanAccountNumber : ticket.loanAccountNumber(),
            ticket.status(),
            ticket.priority(),
            ticket.category(),
            ticket.assigneeName(),
            ticket.assigneeEmail(),
            ticket.channel(),
            ticket.sourceChannel(),
            ticket.slaHint(),
            ticket.createdAt(),
            Instant.now(),
            ticket.closedAt(),
            ticket.conversationCount(),
            ticket.conversations());
    ticketStore.put(updated.id(), updated);
    return updated;
  }

  public void registerTicketFromFunnel(
      String ticketRef,
      String freshdeskId,
      String leadId,
      String subject,
      String mobileNumber,
      String loanAccountNumber,
      String channelTag) {
    Instant now = Instant.now();
    ticketStore.put(
        ticketRef,
        new FreshdeskAgentTicket(
            ticketRef,
            freshdeskId == null ? ticketRef.replace("fd-", "") : freshdeskId,
            leadId,
            subject,
            "Customer",
            "",
            mobileNumber,
            loanAccountNumber,
            TicketStatus.OPEN,
            TicketPriority.MEDIUM,
            "CRM funnel",
            "CRM",
            "",
            "Phone",
            channelTag,
            "New",
            now,
            now,
            null,
            1,
            List.of(
                new FreshdeskConversationEntry(
                    "conv-" + ticketRef,
                    "CRM",
                    subject,
                    now,
                    false))));
  }

  public void markResolved(String ticketId) {
    ticketStore.computeIfPresent(
        ticketId,
        (id, ticket) ->
            copyTicket(
                ticket,
                TicketStatus.RESOLVED,
                ticket.priority(),
                ticket.conversations(),
                Instant.now(),
                ticket.conversationCount()));
  }

  public FreshdeskAgentTicket addReply(StaffUser agent, String ticketId, String body) {
    FreshdeskAgentTicket ticket =
        ticketStore.values().stream()
            .filter(item -> item.id().equals(ticketId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Freshdesk ticket not found"));

    if (freshdeskTicketService.isConfigured()) {
      freshdeskTicketService.addReply(ticket.freshdeskId(), body);
    }

    List<FreshdeskConversationEntry> conversations = new ArrayList<>(ticket.conversations());
    conversations.add(
        new FreshdeskConversationEntry(
            "conv-" + UUID.randomUUID(),
            agent.name(),
            body,
            Instant.now(),
            true));

    FreshdeskAgentTicket updated =
        copyTicket(
            ticket,
            ticket.status(),
            ticket.priority(),
            conversations,
            ticket.closedAt(),
            conversations.size());
    ticketStore.put(updated.id(), updated);
    return updated;
  }

  public FreshdeskAgentTicket updateTicket(
      StaffUser agent, String ticketId, TicketStatus status, TicketPriority priority) {
    FreshdeskAgentTicket ticket =
        ticketStore.values().stream()
            .filter(item -> item.id().equals(ticketId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Freshdesk ticket not found"));

    TicketStatus nextStatus = status != null ? status : ticket.status();
    TicketPriority nextPriority = priority != null ? priority : ticket.priority();
    Instant closedAt =
        nextStatus == TicketStatus.CLOSED || nextStatus == TicketStatus.RESOLVED
            ? Instant.now()
            : ticket.closedAt();

    if (freshdeskTicketService.isConfigured()) {
      freshdeskTicketService.updateTicket(ticket.freshdeskId(), nextStatus, nextPriority);
    }

    FreshdeskAgentTicket updated =
        copyTicket(
            ticket,
            nextStatus,
            nextPriority,
            ticket.conversations(),
            closedAt,
            ticket.conversationCount());
    ticketStore.put(updated.id(), updated);
    return updated;
  }

  public static Instant parseInstantParam(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (DateTimeParseException ignored) {
      return LocalDate.parse(value).atStartOfDay().toInstant(ZoneOffset.UTC);
    }
  }

  public static TicketPriority parsePriority(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return TicketPriority.valueOf(value.toUpperCase());
  }

  private void seedMockTickets() {
    Instant now = Instant.now();
    putMock(
        "fd-26043",
        "26043",
        "1002001",
        "Request to avoid bounce charges and negative reporting - Account XX7457",
        "Rahul Mehta",
        "rahul.mehta@example.com",
        "9999999999",
        "LAN-1002001",
        TicketStatus.OPEN,
        TicketPriority.LOW,
        "Collections",
        "Asha Agent",
        "agent1@trillionloans.com",
        "Email",
        "greylabs_bot",
        "First response due",
        now.minusSeconds(86400 * 2),
        now.minusSeconds(3600),
        null);
    putMock(
        "fd-26044",
        "26044",
        "1002002",
        "Settlement query for closed loan",
        "Priya Sharma",
        "priya.sharma@example.com",
        "8888888888",
        "LAN-1002002-2",
        TicketStatus.PENDING,
        TicketPriority.MEDIUM,
        "Loan servicing",
        "Asha Agent",
        "agent1@trillionloans.com",
        "Web",
        "agent",
        "Waiting on customer",
        now.minusSeconds(86400 * 5),
        now.minusSeconds(7200),
        null);
    putMock(
        "fd-26045",
        "26045",
        "1002001",
        "Customer requested updated repayment schedule",
        "Rahul Mehta",
        "rahul.mehta@example.com",
        "9999999999",
        "LAN-1002001",
        TicketStatus.RESOLVED,
        TicketPriority.HIGH,
        "Repayment",
        "Asha Agent",
        "agent1@trillionloans.com",
        "Phone",
        "greylabs_bot",
        "Resolved",
        now.minusSeconds(86400 * 12),
        now.minusSeconds(86400),
        now.minusSeconds(86400));
    putMock(
        "fd-26046",
        "26046",
        "1002002",
        "EMI debit failed - need callback",
        "Priya Sharma",
        "priya.sharma@example.com",
        "8888888888",
        "LAN-1002002-2",
        TicketStatus.OPEN,
        TicketPriority.URGENT,
        "Payments",
        "Asha Agent",
        "agent1@trillionloans.com",
        "Email",
        "agent",
        "First response overdue by 5 hours",
        now.minusSeconds(86400),
        now.minusSeconds(1800),
        null);
    putMock(
        "fd-26047",
        "26047",
        "1002002",
        "NOC request for foreclosed account",
        "Priya Sharma",
        "priya.sharma@example.com",
        "8888888888",
        "LAN-1002002-2",
        TicketStatus.CLOSED,
        TicketPriority.LOW,
        "Documents",
        "Ravi Agent",
        "agent2@trillionloans.com",
        "Email",
        "agent",
        "Closed",
        now.minusSeconds(86400 * 20),
        now.minusSeconds(86400 * 3),
        now.minusSeconds(86400 * 3));
  }

  private void putMock(
      String id,
      String freshdeskId,
      String leadId,
      String subject,
      String requesterName,
      String requesterEmail,
      String mobileNumber,
      String loanAccountNumber,
      TicketStatus status,
      TicketPriority priority,
      String category,
      String assigneeName,
      String assigneeEmail,
      String channel,
      String sourceChannel,
      String slaHint,
      Instant createdAt,
      Instant updatedAt,
      Instant closedAt) {
    List<FreshdeskConversationEntry> conversations =
        List.of(
            new FreshdeskConversationEntry(
                "conv-" + freshdeskId + "-1",
                requesterName,
                "Initial request logged from " + channel.toLowerCase() + " channel.",
                createdAt,
                false));
    ticketStore.put(
        id,
        new FreshdeskAgentTicket(
            id,
            freshdeskId,
            leadId,
            subject,
            requesterName,
            requesterEmail,
            mobileNumber,
            loanAccountNumber,
            status,
            priority,
            category,
            assigneeName,
            assigneeEmail,
            channel,
            sourceChannel,
            slaHint,
            createdAt,
            updatedAt,
            closedAt,
            conversations.size(),
            conversations));
  }

  private FreshdeskAgentTicket copyTicket(
      FreshdeskAgentTicket ticket,
      TicketStatus status,
      TicketPriority priority,
      List<FreshdeskConversationEntry> conversations,
      Instant closedAt,
      int conversationCount) {
    return new FreshdeskAgentTicket(
        ticket.id(),
        ticket.freshdeskId(),
        ticket.leadId(),
        ticket.subject(),
        ticket.requesterName(),
        ticket.requesterEmail(),
        ticket.mobileNumber(),
        ticket.loanAccountNumber(),
        status,
        priority,
        ticket.category(),
        ticket.assigneeName(),
        ticket.assigneeEmail(),
        ticket.channel(),
        ticket.sourceChannel(),
        ticket.slaHint(),
        ticket.createdAt(),
        Instant.now(),
        closedAt,
        conversationCount,
        conversations);
  }

  private boolean matchesScope(
      FreshdeskAgentTicket ticket,
      BucketScope scope,
      StaffUser agent,
      Set<String> teamEmails) {
    if (scope == BucketScope.ORG) {
      return true;
    }
    if (ticket.assigneeEmail() == null) {
      return false;
    }
    String email = ticket.assigneeEmail().toLowerCase();
    if (scope == BucketScope.AGENT) {
      return agent != null && email.equals(agent.email().toLowerCase());
    }
    return teamEmails.contains(email);
  }

  private Set<String> teamEmailsForScope(BucketScope scope, StaffUser agent, String leadUserId) {
    if (scope != BucketScope.TEAM) {
      return Set.of();
    }
    String resolvedLeadId = leadUserId;
    if (resolvedLeadId == null && agent != null && agent.role() == Role.LEAD) {
      resolvedLeadId = agent.id();
    }
    if (resolvedLeadId == null) {
      return Set.of();
    }
    return crmStore.agentsForLead(resolvedLeadId).stream()
        .map(user -> user.email().toLowerCase())
        .collect(Collectors.toSet());
  }

  private enum BucketScope {
    AGENT,
    TEAM,
    ORG;

    static BucketScope forUser(StaffUser user) {
      if (user.role() == Role.ADMIN) {
        return ORG;
      }
      if (user.role() == Role.LEAD) {
        return TEAM;
      }
      return AGENT;
    }
  }

  private boolean matchesQuery(FreshdeskAgentTicket ticket, String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    String normalized = query.toLowerCase();
    return containsIgnoreCase(ticket.subject(), normalized)
        || containsIgnoreCase(ticket.freshdeskId(), normalized)
        || containsIgnoreCase(ticket.requesterName(), normalized)
        || containsIgnoreCase(ticket.requesterEmail(), normalized)
        || containsIgnoreCase(ticket.mobileNumber(), normalized)
        || containsIgnoreCase(ticket.loanAccountNumber(), normalized);
  }

  private boolean containsIgnoreCase(String source, String needle) {
    return source != null && source.toLowerCase().contains(needle.toLowerCase());
  }

  private FreshdeskBucketSummary summarize(List<FreshdeskAgentTicket> tickets) {
    return new FreshdeskBucketSummary(
        tickets.size(),
        tickets.stream().filter(ticket -> ticket.status() == TicketStatus.OPEN).count(),
        tickets.stream().filter(ticket -> ticket.status() == TicketStatus.PENDING).count(),
        tickets.stream().filter(ticket -> ticket.status() == TicketStatus.RESOLVED).count(),
        tickets.stream().filter(ticket -> ticket.status() == TicketStatus.CLOSED).count());
  }
}
