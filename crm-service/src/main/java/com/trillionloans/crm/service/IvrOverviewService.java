package com.trillionloans.crm.service;

import com.trillionloans.crm.model.CrmModels.CallDirection;
import com.trillionloans.crm.model.CrmModels.CallDisposition;
import com.trillionloans.crm.model.CrmModels.CallEvent;
import com.trillionloans.crm.model.CrmModels.CrmLead;
import com.trillionloans.crm.model.CrmModels.FreshdeskAgentTicket;
import com.trillionloans.crm.model.CrmModels.IvrCallRow;
import com.trillionloans.crm.model.CrmModels.IvrCallSource;
import com.trillionloans.crm.model.CrmModels.IvrOverviewResponse;
import com.trillionloans.crm.model.CrmModels.IvrOverviewSummary;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class IvrOverviewService {

  private final CrmStore store;
  private final AgentFreshdeskTicketService freshdeskTicketService;
  private final PiiMaskingService piiMaskingService;
  private final Map<String, IvrSeedMeta> seedMeta = new ConcurrentHashMap<>();

  public IvrOverviewService(
      CrmStore store,
      AgentFreshdeskTicketService freshdeskTicketService,
      PiiMaskingService piiMaskingService) {
    this.store = store;
    this.freshdeskTicketService = freshdeskTicketService;
    this.piiMaskingService = piiMaskingService;
    seedMetadata();
  }

  public IvrOverviewResponse listOverview(
      StaffUser user,
      String query,
      String leadId,
      String mobileNumber,
      String loanAccountNumber,
      String disposition,
      String callSource,
      Instant from,
      Instant to,
      int page,
      int size) {
    List<CallEvent> scopedCalls = scopedCalls(user);
    List<IvrCallRow> rows =
        scopedCalls.stream()
            .filter(call -> leadId == null || leadId.isBlank() || leadId.equals(call.leadId()))
            .filter(
                call ->
                    mobileNumber == null
                        || mobileNumber.isBlank()
                        || contains(resolveRawMobile(call), mobileNumber))
            .filter(
                call ->
                    loanAccountNumber == null
                        || loanAccountNumber.isBlank()
                        || contains(resolveLoanAccountNumber(call), loanAccountNumber))
            .filter(
                call ->
                    disposition == null
                        || disposition.isBlank()
                        || call.disposition().name().equalsIgnoreCase(disposition))
            .filter(
                call ->
                    callSource == null
                        || callSource.isBlank()
                        || seedMeta
                            .getOrDefault(call.id(), IvrSeedMeta.infer(call))
                            .source()
                            .name()
                            .equalsIgnoreCase(callSource))
            .filter(call -> from == null || !call.startedAt().isBefore(from))
            .filter(call -> to == null || !call.startedAt().isAfter(to))
            .filter(call -> matchesQuery(call, query))
            .map(this::toRow)
            .sorted(Comparator.comparing(IvrCallRow::startedAt).reversed())
            .toList();

    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    int fromIndex = Math.min(safePage * safeSize, rows.size());
    int toIndex = Math.min(fromIndex + safeSize, rows.size());

    return new IvrOverviewResponse(
        rows.subList(fromIndex, toIndex),
        rows.size(),
        safePage,
        safeSize,
        summarize(rows));
  }

  private List<CallEvent> scopedCalls(StaffUser user) {
    if (user.role() == Role.AGENT) {
      return store.callsForAgent(user.id());
    }
    if (user.role() == Role.LEAD) {
      Set<String> teamAgentIds =
          store.agentsForLead(user.id()).stream()
              .map(StaffUser::id)
              .collect(Collectors.toSet());
      return store.listCalls().stream()
          .filter(call -> call.agentId() != null && teamAgentIds.contains(call.agentId()))
          .toList();
    }
    return store.listCalls();
  }

  private IvrCallRow toRow(CallEvent call) {
    IvrSeedMeta meta = seedMeta.getOrDefault(call.id(), IvrSeedMeta.infer(call));
    StaffUser agent = safeUser(call.agentId()).orElse(null);
    CrmLead lead = call.leadId() == null ? null : store.findLeadByLeadId(call.leadId()).orElse(null);
    FreshdeskAgentTicket ticket = resolveTicket(call, lead, meta);

    String mobile =
        lead != null && lead.mobileNumber() != null
            ? lead.mobileNumber()
            : call.phoneNumber();
    String lan =
        lead != null && lead.loanAccountNumber() != null
            ? lead.loanAccountNumber()
            : meta.loanAccountNumber();
    String clientId = lead != null ? lead.clientId() : null;

    return new IvrCallRow(
        call.id(),
        call.callSid(),
        call.agentId(),
        agent != null ? agent.name() : "GreyLabs Bot",
        call.leadId(),
        clientId,
        piiMaskingService.maskMobile(mobile),
        lan,
        meta.summary(),
        ticket != null ? ticket.id() : meta.freshdeskTicketId(),
        ticket != null ? ticket.freshdeskId() : null,
        ticket != null ? ticket.status().name() : null,
        call.direction(),
        call.disposition(),
        meta.source(),
        meta.categoryL1(),
        meta.categoryL2(),
        meta.categoryL3(),
        call.durationSeconds(),
        call.recordingUrl(),
        call.startedAt(),
        call.syncStatus());
  }

  private FreshdeskAgentTicket resolveTicket(CallEvent call, CrmLead lead, IvrSeedMeta meta) {
    if (meta.freshdeskTicketId() != null) {
      return freshdeskTicketService.findById(meta.freshdeskTicketId()).orElse(null);
    }
    return freshdeskTicketService.listAllTickets().stream()
        .filter(
            ticket ->
                (call.leadId() != null && call.leadId().equals(ticket.leadId()))
                    || (lead != null
                        && lead.mobileNumber() != null
                        && lead.mobileNumber().equals(ticket.mobileNumber())))
        .findFirst()
        .orElse(null);
  }

  private IvrOverviewSummary summarize(List<IvrCallRow> rows) {
    return new IvrOverviewSummary(
        rows.size(),
        rows.stream().filter(row -> row.callSource() == IvrCallSource.GREYLABS_BOT).count(),
        rows.stream().filter(row -> row.freshdeskTicketId() != null).count(),
        rows.stream().filter(row -> row.disposition() == CallDisposition.CONNECTED).count(),
        rows.stream().filter(row -> row.disposition() == CallDisposition.CALLBACK_REQUESTED).count());
  }

  private String resolveRawMobile(CallEvent call) {
    CrmLead lead =
        call.leadId() == null ? null : store.findLeadByLeadId(call.leadId()).orElse(null);
    if (lead != null && lead.mobileNumber() != null) {
      return lead.mobileNumber();
    }
    return call.phoneNumber();
  }

  private String resolveLoanAccountNumber(CallEvent call) {
    IvrSeedMeta meta = seedMeta.getOrDefault(call.id(), IvrSeedMeta.infer(call));
    CrmLead lead =
        call.leadId() == null ? null : store.findLeadByLeadId(call.leadId()).orElse(null);
    if (lead != null && lead.loanAccountNumber() != null) {
      return lead.loanAccountNumber();
    }
    return meta.loanAccountNumber();
  }

  private boolean matchesQuery(CallEvent call, String query) {
    if (!StringUtils.hasText(query)) {
      return true;
    }
    IvrSeedMeta meta = seedMeta.getOrDefault(call.id(), IvrSeedMeta.infer(call));
    StaffUser agent = safeUser(call.agentId()).orElse(null);
    CrmLead lead = call.leadId() == null ? null : store.findLeadByLeadId(call.leadId()).orElse(null);
    FreshdeskAgentTicket ticket = resolveTicket(call, lead, meta);
    String normalized = query.toLowerCase();
    return contains(meta.summary(), normalized)
        || contains(call.leadId(), normalized)
        || contains(lead == null ? null : lead.clientId(), normalized)
        || contains(resolveRawMobile(call), normalized)
        || contains(resolveLoanAccountNumber(call), normalized)
        || contains(ticket == null ? null : ticket.freshdeskId(), normalized)
        || contains(ticket == null ? meta.freshdeskTicketId() : ticket.id(), normalized)
        || contains(call.callSid(), normalized)
        || contains(agent == null ? null : agent.name(), normalized)
        || contains(meta.categoryL1(), normalized)
        || contains(meta.categoryL2(), normalized)
        || contains(meta.categoryL3(), normalized);
  }

  private boolean contains(String value, String needle) {
    return value != null && value.toLowerCase().contains(needle.toLowerCase());
  }

  private Optional<StaffUser> safeUser(String userId) {
    if (userId == null) {
      return Optional.empty();
    }
    try {
      return Optional.of(store.getUser(userId));
    } catch (NotFoundException exception) {
      return Optional.empty();
    }
  }

  private void seedMetadata() {
    seedMeta.put(
        "call-ivr-1",
        new IvrSeedMeta(
            IvrCallSource.GREYLABS_BOT,
            "LAN-1002001",
            "fd-26043",
            "GreyLabs bot explained EMI due date. Customer asked for waiver on bounce charges — escalated to agent callback.",
            "Lending",
            "Repayment",
            "Bounce charge waiver request"));
    seedMeta.put(
        "call-ivr-2",
        new IvrSeedMeta(
            IvrCallSource.GREYLABS_BOT,
            "LAN-1002002-2",
            "fd-26044",
            "Bot shared settlement steps for closed loan. Customer requested written confirmation — ticket created for agent follow-up.",
            "Lending",
            "Settlement",
            "Written confirmation requested"));
    seedMeta.put(
        "call-ivr-3",
        new IvrSeedMeta(
            IvrCallSource.GREYLABS_BOT,
            "LAN-1002001",
            null,
            "GreyLabs bot provided updated repayment schedule verbally. Customer satisfied — no ticket raised.",
            "Lending",
            "Repayment",
            "Schedule shared"));
    seedMeta.put(
        "call-1001",
        new IvrSeedMeta(
            IvrCallSource.EXOTEL_INBOUND,
            "LAN-900001",
            null,
            "Inbound call — customer requested callback on repayment schedule.",
            "Lending",
            "Repayment",
            "Callback requested"));
  }

  private record IvrSeedMeta(
      IvrCallSource source,
      String loanAccountNumber,
      String freshdeskTicketId,
      String summary,
      String categoryL1,
      String categoryL2,
      String categoryL3) {

    static IvrSeedMeta infer(CallEvent call) {
      IvrCallSource source =
          call.syncStatus().name().equals("SYNCED") && call.callSid() != null
                  && call.callSid().contains("greylabs")
              ? IvrCallSource.GREYLABS_BOT
              : call.direction() == CallDirection.INBOUND
                  ? IvrCallSource.EXOTEL_INBOUND
                  : call.direction() == CallDirection.OUTBOUND
                      ? IvrCallSource.EXOTEL_OUTBOUND
                      : IvrCallSource.AGENT_MANUAL;
      String summary =
          switch (call.disposition()) {
            case CONNECTED -> "Call connected — customer query handled.";
            case CALLBACK_REQUESTED -> "Customer requested a callback from an agent.";
            case ESCALATED -> "Bot could not resolve — escalated to Freshdesk ticket.";
            case RESOLVED -> "Query resolved on the call.";
            case PROMISE_TO_PAY -> "Customer gave promise-to-pay on the call.";
            default -> call.disposition().name().replace('_', ' ') + " on call.";
          };
      return new IvrSeedMeta(source, null, null, summary, "General", "CRM", "Call log");
    }
  }
}
