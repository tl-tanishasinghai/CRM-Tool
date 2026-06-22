package com.trillionloans.crm.controller;

import com.trillionloans.crm.model.CrmModels.CallEvent;
import com.trillionloans.crm.model.CrmModels.CallSyncStatus;
import com.trillionloans.crm.model.CrmModels.CreateCallEventRequest;
import com.trillionloans.crm.model.CrmModels.ExotelSyncResult;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.service.AuthService;
import com.trillionloans.crm.service.CrmStore;
import com.trillionloans.crm.service.ExotelCallSyncService;
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
@RequestMapping("/crm")
public class CallController {

  private final AuthService authService;
  private final CrmStore store;
  private final ExotelCallSyncService exotelCallSyncService;

  public CallController(
      AuthService authService, CrmStore store, ExotelCallSyncService exotelCallSyncService) {
    this.authService = authService;
    this.store = store;
    this.exotelCallSyncService = exotelCallSyncService;
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
            request.startedAt() == null ? Instant.now() : request.startedAt(),
            null,
            CallSyncStatus.MANUAL,
            Instant.now(),
            Instant.now());
    return store.addCall(event);
  }

  @PostMapping("/calls/sync/exotel")
  public ExotelSyncResult syncExotel(@RequestHeader("X-CRM-Token") String token) {
    authService.requireAnyRole(token, Role.ADMIN, Role.LEAD);
    return exotelCallSyncService.syncPreviousDay();
  }
}
