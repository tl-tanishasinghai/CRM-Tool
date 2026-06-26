package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.CrmModels.CallDisposition;
import com.trillionloans.crm.model.CrmModels.CallEvent;
import com.trillionloans.crm.model.CrmModels.CallSyncStatus;
import com.trillionloans.crm.model.CrmModels.CreateCallEventRequest;
import com.trillionloans.crm.model.CrmModels.ExotelSyncResult;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.model.CrmModels.UpdateCallDispositionRequest;
import com.trillionloans.crm.service.AuthService;
import com.trillionloans.crm.service.CallFunnelService;
import com.trillionloans.crm.service.CrmStore;
import com.trillionloans.crm.service.ExotelCallSyncService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/crm")
public class CallController {

  private final AuthService authService;
  private final CrmStore store;
  private final ExotelCallSyncService exotelCallSyncService;
  private final CallFunnelService callFunnelService;

  public CallController(
      AuthService authService,
      CrmStore store,
      ExotelCallSyncService exotelCallSyncService,
      CallFunnelService callFunnelService) {
    this.authService = authService;
    this.store = store;
    this.exotelCallSyncService = exotelCallSyncService;
    this.callFunnelService = callFunnelService;
  }

  @GetMapping("/calls")
  public List<CallEvent> allCalls(@RequestHeader("X-CRM-Token") String token) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD);
    return store.listCalls();
  }

  @GetMapping("/customers/{leadId}/calls")
  public List<CallEvent> calls(
      @RequestHeader("X-CRM-Token") String token, @PathVariable String leadId) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    return store.callsForLead(leadId);
  }

  @PostMapping("/calls/events")
  public CallEvent createCallEvent(
      @RequestHeader("X-CRM-Token") String token,
      @Valid @RequestBody CreateCallEventRequest request) {
    StaffUser user = authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    CallEvent event =
        new CallEvent(
            "call-" + UUID.randomUUID(),
            "manual-" + UUID.randomUUID(),
            null,
            request.leadId(),
            request.agentId() == null ? user.id() : request.agentId(),
            request.direction(),
            request.disposition(),
            request.disposition().name().toLowerCase(),
            request.phoneNumber(),
            request.direction().name().equals("INBOUND") ? request.phoneNumber() : null,
            request.direction().name().equals("OUTBOUND") ? request.phoneNumber() : null,
            request.durationSeconds(),
            request.recordingUrl(),
            "agent",
            null,
            request.startedAt() == null ? Instant.now() : request.startedAt(),
            null,
            CallSyncStatus.MANUAL,
            Instant.now(),
            Instant.now());
    return store.addCall(event);
  }

  @PatchMapping("/calls/events/{callId}/disposition")
  public CallEvent updateDisposition(
      @RequestHeader("X-CRM-Token") String token,
      @PathVariable String callId,
      @Valid @RequestBody UpdateCallDispositionRequest request) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD, Role.AGENT);
    CallEvent existing =
        store.listCalls().stream()
            .filter(call -> call.id().equals(callId))
            .findFirst()
            .orElseThrow(() -> new com.trillionloans.crm.service.NotFoundException("Call not found"));
    CallEvent updated =
        new CallEvent(
            existing.id(),
            existing.callSid(),
            existing.parentCallSid(),
            existing.leadId(),
            existing.agentId(),
            existing.direction(),
            request.disposition(),
            request.disposition().name().toLowerCase(),
            existing.phoneNumber(),
            existing.fromNumber(),
            existing.toNumber(),
            existing.durationSeconds(),
            existing.recordingUrl(),
            existing.sourceChannel(),
            existing.freshdeskTicketId(),
            existing.startedAt(),
            existing.endedAt(),
            existing.syncStatus(),
            existing.createdAt(),
            Instant.now());
    store.addCall(updated);
    callFunnelService.handleDispositionUpdate(updated, request.disposition());
    return updated;
  }

  @PostMapping("/calls/sync/exotel")
  public ExotelSyncResult syncExotel(@RequestHeader("X-CRM-Token") String token) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD);
    return exotelCallSyncService.syncPreviousDay();
  }
}
