package com.trillionloans.crm.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class CrmModels {

  private CrmModels() {}

  public enum Role {
    ADMIN,
    LEAD,
    AGENT
  }

  public enum UserStatus {
    ACTIVE,
    INACTIVE
  }

  public enum LeadStatus {
    NEW,
    ASSIGNED,
    IN_PROGRESS,
    FOLLOW_UP,
    ESCALATED,
    CLOSED
  }

  public enum LeadSource {
    INBOUND_CALL,
    OUTBOUND_CAMPAIGN,
    FRESHDESK,
    MANUAL,
    MOCK
  }

  public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
  }

  public enum CallDirection {
    INBOUND,
    OUTBOUND
  }

  public enum CallDisposition {
    CONNECTED,
    NOT_CONNECTED,
    CALLBACK_REQUESTED,
    ESCALATED,
    PROMISE_TO_PAY,
    WRONG_NUMBER,
    RESOLVED
  }

  public enum CallSyncStatus {
    SYNCED,
    MANUAL,
    FAILED
  }

  public enum IvrCallSource {
    GREYLABS_BOT,
    EXOTEL_INBOUND,
    EXOTEL_OUTBOUND,
    AGENT_MANUAL
  }

  public enum TicketStatus {
    OPEN,
    PENDING,
    RESOLVED,
    CLOSED
  }

  public enum TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT
  }

  public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

  public record LoginResponse(String token, StaffUser user) {}

  public record StaffUser(
      String id,
      String name,
      String email,
      Role role,
      UserStatus status,
      String leadId,
      Instant createdAt) {}

  public record UpsertUserRequest(
      @NotBlank String name,
      @NotBlank String email,
      @NotNull Role role,
      String leadId,
      UserStatus status) {}

  public record CustomerProfile(
      String leadId,
      String clientId,
      String name,
      String mobileNo,
      String email,
      String panLast4,
      LocalDate dateOfBirth,
      String city,
      String address,
      String ucic,
      String dataSource) {}

  public record LoanSummary(
      String loanAccountNumber,
      String loanApplicationId,
      String product,
      String status,
      String lenderName,
      String currentStage,
      String applicationStatus,
      BigDecimal principal,
      BigDecimal outstanding,
      BigDecimal emi,
      BigDecimal paidAmount,
      BigDecimal remainingAmount,
      BigDecimal excessAdjusted,
      BigDecimal excessRefunded,
      BigDecimal interestRate,
      Integer dpd,
      Integer tenureMonths,
      Integer installmentRemaining,
      LocalDate disbursementDate,
      LocalDate nextDueDate,
      String message) {}

  public record CrmLead(
      String id,
      String leadId,
      String clientId,
      String mobileNumber,
      String title,
      String loanAccountNumber,
      String loanApplicationId,
      LeadStatus status,
      Priority priority,
      LeadSource source,
      String assignedAgentId,
      String assignedLeadId,
      Instant assignedAt,
      Instant createdAt,
      Instant updatedAt) {}

  public record LeadIngestRequest(
      @NotBlank String leadId,
      String clientId,
      String mobileNumber,
      String title,
      String loanAccountNumber,
      String loanApplicationId,
      LeadSource source,
      Priority priority) {}

  public record AssignLeadRequest(@NotBlank String agentId, String reason) {}

  public record UpdateLeadStatusRequest(@NotNull LeadStatus status) {}

  public record TicketSummary(
      String id,
      String leadId,
      String subject,
      TicketStatus status,
      TicketPriority priority,
      String category,
      Instant createdAt,
      Instant updatedAt) {}

  public record FreshdeskConversationEntry(
      String id, String author, String body, Instant createdAt, boolean agentReply) {}

  public record FreshdeskAgentTicket(
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
      Instant closedAt,
      int conversationCount,
      List<FreshdeskConversationEntry> conversations) {}

  public record FreshdeskLinkCustomerRequest(
      @NotBlank String leadId, String mobileNumber, String loanAccountNumber) {}

  public record FreshdeskBucketSummary(
      long total, long openCount, long pendingCount, long resolvedCount, long closedCount) {}

  public record FreshdeskTicketBucketResponse(
      List<FreshdeskAgentTicket> tickets,
      FreshdeskBucketSummary summary,
      Instant syncedAt,
      boolean freshdeskConfigured) {}

  public record FreshdeskTicketReplyRequest(@NotBlank String body) {}

  public record FreshdeskTicketUpdateRequest(TicketStatus status, TicketPriority priority) {}

  public record CreateTicketRequest(
      @NotBlank String subject,
      @NotBlank String description,
      String category,
      TicketPriority priority,
      String loanAccountNumber) {}

  public record CallEvent(
      String id,
      String callSid,
      String parentCallSid,
      String leadId,
      String agentId,
      CallDirection direction,
      CallDisposition disposition,
      String callStatus,
      String phoneNumber,
      String fromNumber,
      String toNumber,
      Integer durationSeconds,
      String recordingUrl,
      String sourceChannel,
      String freshdeskTicketId,
      Instant startedAt,
      Instant endedAt,
      CallSyncStatus syncStatus,
      Instant createdAt,
      Instant updatedAt) {}

  public record CreateCallEventRequest(
      @NotBlank String leadId,
      String agentId,
      @NotNull CallDirection direction,
      @NotNull CallDisposition disposition,
      @NotBlank String phoneNumber,
      Integer durationSeconds,
      String recordingUrl,
      Instant startedAt) {}

  public record UpdateCallDispositionRequest(@NotNull CallDisposition disposition) {}

  public record ExotelSyncResult(
      Instant from,
      Instant to,
      int fetched,
      int inserted,
      int updated,
      int failed,
      String message) {}

  public record AgentNote(
      String id,
      String leadId,
      String agentId,
      String disposition,
      String note,
      Instant followUpAt,
      Instant createdAt) {}

  public record CreateNoteRequest(
      @NotBlank String note, String disposition, Instant followUpAt) {}

  public record ActivityItem(
      String id, String type, String title, String description, String actorId, Instant occurredAt) {}

  public record CustomerDashboard(
      CustomerProfile profile,
      List<LoanSummary> loans,
      List<TicketSummary> tickets,
      List<CallEvent> calls,
      List<AgentNote> notes,
      List<ActivityItem> activity) {}

  public record QueueSummary(long total, long newItems, long followUps, long escalated) {}

  public record CrmBucketSummary(
      long total,
      long newCount,
      long assignedCount,
      long inProgressCount,
      long followUpCount,
      long escalatedCount,
      long closedCount) {}

  public record OpsRateMetrics(
      double crmResolutionRate,
      double freshdeskResolutionRate,
      double escalationRate,
      long crmOpenBacklog,
      long freshdeskOpenBacklog,
      Double avgCrmCloseHours,
      Double avgFreshdeskCloseHours) {}

  public record OpsOverviewResponse(
      Instant from,
      Instant to,
      CrmBucketSummary crmBuckets,
      FreshdeskBucketSummary freshdeskBuckets,
      OpsRateMetrics rates,
      List<TeamOpsRow> teams,
      List<AgentOpsRow> agents) {}

  public record TeamOpsRow(
      String leadId,
      String leadName,
      long agentCount,
      long openCrm,
      long openFreshdesk,
      double crmResolutionRate,
      double freshdeskResolutionRate,
      long escalations) {}

  public record AgentOpsRow(
      String agentId,
      String agentName,
      String teamLeadId,
      String teamLeadName,
      long openCrm,
      long openFreshdesk,
      long resolvedCrm7d,
      long resolvedFreshdesk7d,
      Double avgHandleHours) {}

  public enum OpsTicketSource {
    CRM,
    FRESHDESK
  }

  public record OpsTicketRow(
      String id,
      OpsTicketSource source,
      String leadId,
      String title,
      String status,
      String priority,
      String assigneeId,
      String assigneeName,
      String teamLeadId,
      String teamLeadName,
      String mobileNumber,
      String loanAccountNumber,
      Instant createdAt,
      Instant updatedAt,
      Instant closedAt) {}

  public record OpsTicketsPage(List<OpsTicketRow> tickets, long total, int page, int size) {}

  public record IntegrationsHealthResponse(
      boolean freshdeskConfigured,
      Instant freshdeskLastSync,
      boolean exotelConfigured,
      String exotelStatus,
      boolean losLmsLiveData) {}

  public record IvrCallRow(
      String callId,
      String callSid,
      String agentId,
      String agentName,
      String assignedAgent,
      String leadId,
      String clientId,
      String mobileNumber,
      String loanAccountNumber,
      String email,
      String callSummary,
      String freshdeskTicketId,
      String freshdeskTicketRef,
      String ticketStatus,
      CallDirection direction,
      CallDisposition disposition,
      IvrCallSource callSource,
      String categoryL1,
      String categoryL2,
      String categoryL3,
      Integer durationSeconds,
      String recordingUrl,
      Instant startedAt,
      CallSyncStatus syncStatus) {}

  public record IvrOverviewResponse(
      List<IvrCallRow> calls, long total, int page, int size, IvrOverviewSummary summary) {}

  public record IvrOverviewSummary(
      long total,
      long greylabsBot,
      long escalatedToTicket,
      long connected,
      long callbackRequested) {}

  public record TeamQueueResponse(
      List<StaffUser> agents,
      List<CrmLead> leads,
      QueueSummary summary,
      CrmBucketSummary crmBuckets) {}

  public record SearchResult(
      String type,
      String leadId,
      String clientId,
      String mobileNumber,
      String loanAccountNumber,
      String loanApplicationId,
      String displayName,
      String matchedOn) {}

  public record FieldSearchResult(
      String leadId,
      String clientId,
      String matchedField,
      String matchedValue,
      String highlightLoanAccountNumber,
      boolean customerFound,
      String displayName,
      String mobileNumber) {}

  public record RepaymentScheduleRow(
      Integer period,
      Integer days,
      String date,
      String paidDate,
      String emiPaidDate,
      BigDecimal principal,
      BigDecimal principalOutstanding,
      BigDecimal interest,
      BigDecimal fees,
      BigDecimal penalties,
      BigDecimal due,
      BigDecimal paid,
      BigDecimal outstanding,
      BigDecimal inAdvance,
      BigDecimal late) {}

  public record LoanRepaymentSchedule(
      String loanAccountNumber,
      String scheduleType,
      List<RepaymentScheduleRow> periods) {}

  public record LoanTransactionRow(
      String office,
      String transactionDate,
      String transactionType,
      BigDecimal amount,
      BigDecimal principal,
      BigDecimal interest,
      BigDecimal fees,
      BigDecimal penalties,
      BigDecimal principalOutstanding,
      String transactionStatus,
      BigDecimal excessAmount) {}

  public record LoanTransactionHistory(
      String loanAccountNumber, List<LoanTransactionRow> transactions) {}
}
