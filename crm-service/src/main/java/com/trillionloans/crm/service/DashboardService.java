package com.trillionloans.crm.service;

import com.trillionloans.crm.model.CrmModels.ActivityItem;
import com.trillionloans.crm.model.CrmModels.AgentNote;
import com.trillionloans.crm.model.CrmModels.CallEvent;
import com.trillionloans.crm.model.CrmModels.CustomerProfile;
import com.trillionloans.crm.model.CrmModels.CustomerDashboard;
import com.trillionloans.crm.model.CrmModels.TicketSummary;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

  private final CrmStore store;
  private final ExternalDataService externalDataService;
  private final FreshdeskTicketService freshdeskTicketService;

  public DashboardService(
      CrmStore store,
      ExternalDataService externalDataService,
      FreshdeskTicketService freshdeskTicketService) {
    this.store = store;
    this.externalDataService = externalDataService;
    this.freshdeskTicketService = freshdeskTicketService;
  }

  public CustomerDashboard getDashboard(String leadId) {
    CustomerProfile profile = externalDataService.getCustomerProfile(leadId);
    List<TicketSummary> tickets =
        Stream.concat(store.ticketsForLead(leadId).stream(), freshdeskTicketService.searchTickets(profile).stream())
            .sorted(Comparator.comparing(TicketSummary::updatedAt).reversed())
            .toList();
    List<CallEvent> calls = store.callsForLead(leadId);
    List<AgentNote> notes = store.notesForLead(leadId);

    List<ActivityItem> activity =
        Stream.concat(
                tickets.stream()
                    .map(
                        ticket ->
                            new ActivityItem(
                                ticket.id(),
                                "TICKET",
                                ticket.subject(),
                                ticket.status().name(),
                                null,
                                ticket.updatedAt())),
                calls.stream()
                    .map(
                        call ->
                            new ActivityItem(
                                call.id(),
                                "CALL",
                                call.direction().name() + " call",
                                call.disposition().name(),
                                call.agentId(),
                                call.startedAt())))
            .sorted(Comparator.comparing(ActivityItem::occurredAt).reversed())
            .toList();

    return new CustomerDashboard(
        profile,
        externalDataService.getLoanSummaries(leadId),
        tickets,
        calls,
        notes,
        activity);
  }
}
