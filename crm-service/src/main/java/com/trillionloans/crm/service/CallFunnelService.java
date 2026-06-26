package com.trillionloans.crm.service;

import com.trillionloans.crm.model.CrmModels.CallDisposition;
import com.trillionloans.crm.model.CrmModels.CallEvent;
import com.trillionloans.crm.model.CrmModels.IvrCallSource;
import com.trillionloans.crm.model.CrmModels.TicketPriority;
import com.trillionloans.crm.model.CrmModels.TicketStatus;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CallFunnelService {

  private static final Logger log = LoggerFactory.getLogger(CallFunnelService.class);

  private final CrmStore store;
  private final FreshdeskTicketService freshdeskTicketService;
  private final AgentFreshdeskTicketService agentFreshdeskTicketService;

  public CallFunnelService(
      CrmStore store,
      FreshdeskTicketService freshdeskTicketService,
      AgentFreshdeskTicketService agentFreshdeskTicketService) {
    this.store = store;
    this.freshdeskTicketService = freshdeskTicketService;
    this.agentFreshdeskTicketService = agentFreshdeskTicketService;
  }

  public String createTicketForCall(
      CallEvent call,
      String subject,
      String description,
      String leadId,
      String mobileNumber,
      String loanAccountNumber,
      IvrCallSource source) {
    String channelTag = source == IvrCallSource.GREYLABS_BOT ? "greylabs_bot" : "agent";
    String freshdeskId =
        freshdeskTicketService.createTicket(
            subject,
            description,
            leadId,
            mobileNumber,
            loanAccountNumber,
            channelTag);

    String ticketRef = freshdeskId == null ? "fd-local-" + UUID.randomUUID() : "fd-" + freshdeskId;
    agentFreshdeskTicketService.registerTicketFromFunnel(
        ticketRef,
        freshdeskId,
        leadId,
        subject,
        mobileNumber,
        loanAccountNumber,
        channelTag);

    CallEvent linked =
        new CallEvent(
            call.id(),
            call.callSid(),
            call.parentCallSid(),
            leadId != null ? leadId : call.leadId(),
            call.agentId(),
            call.direction(),
            call.disposition(),
            call.callStatus(),
            call.phoneNumber(),
            call.fromNumber(),
            call.toNumber(),
            call.durationSeconds(),
            call.recordingUrl(),
            channelTag,
            ticketRef,
            call.startedAt(),
            call.endedAt(),
            call.syncStatus(),
            call.createdAt(),
            Instant.now());
    store.addCall(linked);
    return ticketRef;
  }

  public void handleDispositionUpdate(CallEvent call, CallDisposition disposition) {
    if (disposition != CallDisposition.RESOLVED) {
      return;
    }
    boolean greylabs =
        "greylabs_bot".equalsIgnoreCase(call.sourceChannel())
            || (call.callSid() != null && call.callSid().toLowerCase().contains("greylabs"));
    if (!greylabs || call.freshdeskTicketId() == null) {
      return;
    }

    agentFreshdeskTicketService
        .findById(call.freshdeskTicketId())
        .ifPresent(
            ticket -> {
              if (freshdeskTicketService.isConfigured()) {
                freshdeskTicketService.updateTicket(
                    ticket.freshdeskId(), TicketStatus.RESOLVED, null);
                freshdeskTicketService.updateCustomFields(
                    ticket.freshdeskId(),
                    Map.of("cf_source_channel", "greylabs_bot"));
              }
              agentFreshdeskTicketService.markResolved(ticket.id());
              log.info(
                  "Auto-resolved Freshdesk ticket {} for GreyLabs call {}",
                  ticket.freshdeskId(),
                  call.id());
            });
  }
}
