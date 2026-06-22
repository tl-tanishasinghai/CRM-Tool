package com.trillionloans.crm.repository;

import com.trillionloans.crm.model.CrmModels.AgentNote;
import com.trillionloans.crm.model.CrmModels.CallDirection;
import com.trillionloans.crm.model.CrmModels.CallDisposition;
import com.trillionloans.crm.model.CrmModels.CallEvent;
import com.trillionloans.crm.model.CrmModels.CallSyncStatus;
import com.trillionloans.crm.model.CrmModels.CrmLead;
import com.trillionloans.crm.model.CrmModels.LeadSource;
import com.trillionloans.crm.model.CrmModels.LeadStatus;
import com.trillionloans.crm.model.CrmModels.Priority;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.model.CrmModels.TicketPriority;
import com.trillionloans.crm.model.CrmModels.TicketStatus;
import com.trillionloans.crm.model.CrmModels.TicketSummary;
import com.trillionloans.crm.model.WrapperModels.PiiField;
import com.trillionloans.crm.model.WrapperModels.PiiRevealAudit;
import java.sql.Timestamp;
import java.time.Instant;
import org.springframework.jdbc.core.RowMapper;

public final class CrmRowMappers {

  private CrmRowMappers() {}

  public static final RowMapper<StaffUser> STAFF_USER =
      (rs, rowNum) ->
          new StaffUser(
              rs.getString("id"),
              rs.getString("name"),
              rs.getString("email"),
              Role.valueOf(rs.getString("role")),
              com.trillionloans.crm.model.CrmModels.UserStatus.valueOf(rs.getString("status")),
              rs.getString("lead_id"),
              toInstant(rs.getTimestamp("created_at")));

  public static final RowMapper<CrmLead> CRM_LEAD =
      (rs, rowNum) ->
          new CrmLead(
              rs.getString("id"),
              rs.getString("lead_id"),
              rs.getString("client_id"),
              rs.getString("mobile_number"),
              rs.getString("title"),
              rs.getString("loan_account_number"),
              rs.getString("loan_application_id"),
              LeadStatus.valueOf(rs.getString("status")),
              Priority.valueOf(rs.getString("priority")),
              LeadSource.valueOf(rs.getString("source")),
              rs.getString("assigned_agent_id"),
              rs.getString("assigned_lead_id"),
              toInstant(rs.getTimestamp("assigned_at")),
              toInstant(rs.getTimestamp("created_at")),
              toInstant(rs.getTimestamp("updated_at")));

  public static final RowMapper<TicketSummary> TICKET =
      (rs, rowNum) ->
          new TicketSummary(
              rs.getString("id"),
              rs.getString("lead_id"),
              rs.getString("subject"),
              TicketStatus.valueOf(rs.getString("status")),
              TicketPriority.valueOf(rs.getString("priority")),
              rs.getString("category"),
              toInstant(rs.getTimestamp("created_at")),
              toInstant(rs.getTimestamp("updated_at")));

  public static final RowMapper<CallEvent> CALL =
      (rs, rowNum) ->
          new CallEvent(
              rs.getString("id"),
              rs.getString("call_sid"),
              rs.getString("parent_call_sid"),
              rs.getString("lead_id"),
              rs.getString("agent_id"),
              enumOrNull(CallDirection.class, rs.getString("call_direction")),
              enumOrNull(CallDisposition.class, rs.getString("disposition")),
              rs.getString("call_status"),
              rs.getString("phone_number"),
              rs.getString("from_number"),
              rs.getString("to_number"),
              (Integer) rs.getObject("duration_seconds"),
              rs.getString("recording_url"),
              toInstant(rs.getTimestamp("started_at")),
              toInstant(rs.getTimestamp("ended_at")),
              enumOrNull(CallSyncStatus.class, rs.getString("sync_status")),
              toInstant(rs.getTimestamp("created_at")),
              toInstant(rs.getTimestamp("updated_at")));

  public static final RowMapper<AgentNote> NOTE =
      (rs, rowNum) ->
          new AgentNote(
              rs.getString("id"),
              rs.getString("lead_id"),
              rs.getString("agent_id"),
              rs.getString("disposition"),
              rs.getString("note"),
              toInstant(rs.getTimestamp("follow_up_at")),
              toInstant(rs.getTimestamp("created_at")));

  public static final RowMapper<PiiRevealAudit> PII_AUDIT =
      (rs, rowNum) ->
          new PiiRevealAudit(
              rs.getString("id"),
              rs.getString("lead_id"),
              rs.getString("agent_id"),
              PiiField.valueOf(rs.getString("field_name")),
              rs.getString("reason"),
              toInstant(rs.getTimestamp("created_at")));

  private static Instant toInstant(Timestamp timestamp) {
    return timestamp == null ? null : timestamp.toInstant();
  }

  private static Timestamp toTimestamp(Instant instant) {
    return instant == null ? null : Timestamp.from(instant);
  }

  public static Timestamp ts(Instant instant) {
    return toTimestamp(instant);
  }

  private static <E extends Enum<E>> E enumOrNull(Class<E> type, String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return Enum.valueOf(type, value);
  }
}
