package com.trillionloans.crm.service;

import com.trillionloans.crm.model.CrmModels.AgentNote;
import com.trillionloans.crm.model.CrmModels.CallEvent;
import com.trillionloans.crm.model.CrmModels.CrmLead;
import com.trillionloans.crm.model.CrmModels.LeadIngestRequest;
import com.trillionloans.crm.model.CrmModels.LeadSource;
import com.trillionloans.crm.model.CrmModels.LeadStatus;
import com.trillionloans.crm.model.CrmModels.Priority;
import com.trillionloans.crm.model.CrmModels.Role;
import com.trillionloans.crm.model.CrmModels.StaffUser;
import com.trillionloans.crm.model.CrmModels.TicketPriority;
import com.trillionloans.crm.model.CrmModels.TicketStatus;
import com.trillionloans.crm.model.CrmModels.TicketSummary;
import com.trillionloans.crm.model.CrmModels.UpsertUserRequest;
import com.trillionloans.crm.model.CrmModels.UserStatus;
import com.trillionloans.crm.model.WrapperModels.PiiRevealAudit;
import com.trillionloans.crm.repository.CrmRowMappers;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CrmStore {

  private static final String STAFF_COLUMNS =
      "id, name, email, role, status, lead_id, created_at";

  private final JdbcTemplate jdbc;

  public CrmStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public List<StaffUser> listUsers() {
    return jdbc.query(
        "SELECT " + STAFF_COLUMNS + " FROM staff_users ORDER BY created_at",
        CrmRowMappers.STAFF_USER);
  }

  public List<StaffUser> activeAgents() {
    return jdbc.query(
        """
        SELECT %s FROM staff_users
        WHERE role = 'AGENT' AND status = 'ACTIVE'
        ORDER BY id
        """
            .formatted(STAFF_COLUMNS),
        CrmRowMappers.STAFF_USER);
  }

  public List<StaffUser> activeLeads() {
    return jdbc.query(
        """
        SELECT %s FROM staff_users
        WHERE role = 'LEAD' AND status = 'ACTIVE'
        ORDER BY id
        """
            .formatted(STAFF_COLUMNS),
        CrmRowMappers.STAFF_USER);
  }

  public List<StaffUser> agentsForLead(String leadUserId) {
    return jdbc.query(
        """
        SELECT %s FROM staff_users
        WHERE role = 'AGENT' AND status = 'ACTIVE' AND lead_id = ?
        ORDER BY id
        """
            .formatted(STAFF_COLUMNS),
        CrmRowMappers.STAFF_USER,
        leadUserId);
  }

  public Optional<StaffUser> findUserByEmail(String email) {
    List<StaffUser> users =
        jdbc.query(
            """
            SELECT %s FROM staff_users WHERE LOWER(email) = LOWER(?)
            """
                .formatted(STAFF_COLUMNS),
            CrmRowMappers.STAFF_USER,
            email);
    return users.stream().findFirst();
  }

  public StaffUser getUser(String id) {
    try {
      return jdbc.queryForObject(
          "SELECT " + STAFF_COLUMNS + " FROM staff_users WHERE id = ?",
          CrmRowMappers.STAFF_USER,
          id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("CRM user not found");
    }
  }

  @Transactional
  public StaffUser upsertUser(String id, UpsertUserRequest request) {
    String userId = id == null ? "user-" + UUID.randomUUID() : id;
    Instant createdAt =
        jdbc.query(
                """
                SELECT created_at FROM staff_users WHERE id = ?
                """,
                (rs, rowNum) -> rs.getTimestamp("created_at").toInstant(),
                userId)
            .stream()
            .findFirst()
            .orElse(Instant.now());
    UserStatus status = Optional.ofNullable(request.status()).orElse(UserStatus.ACTIVE);

    jdbc.update(
        """
        INSERT INTO staff_users (id, name, email, role, status, lead_id, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
          name = EXCLUDED.name,
          email = EXCLUDED.email,
          role = EXCLUDED.role,
          status = EXCLUDED.status,
          lead_id = EXCLUDED.lead_id
        """,
        userId,
        request.name(),
        request.email(),
        request.role().name(),
        status.name(),
        request.leadId(),
        CrmRowMappers.ts(createdAt));
    return getUser(userId);
  }

  @Transactional
  public CrmLead ingestLead(LeadIngestRequest request) {
    Optional<StaffUser> owner = selectNextAgent();
    String ownerId = owner.map(StaffUser::id).orElse(null);
    String leadOwnerId = ownerId == null ? null : getUser(ownerId).leadId();
    Instant now = Instant.now();
    String crmLeadId = "crm-lead-" + UUID.randomUUID();

    CrmLead lead =
        new CrmLead(
            crmLeadId,
            request.leadId(),
            request.clientId(),
            Optional.ofNullable(request.mobileNumber()).orElse("9999999999"),
            Optional.ofNullable(request.title()).orElse("Customer follow-up"),
            request.loanAccountNumber(),
            request.loanApplicationId(),
            ownerId == null ? LeadStatus.NEW : LeadStatus.ASSIGNED,
            Optional.ofNullable(request.priority()).orElse(Priority.MEDIUM),
            Optional.ofNullable(request.source()).orElse(LeadSource.MANUAL),
            ownerId,
            leadOwnerId,
            ownerId == null ? null : now,
            now,
            now);
    insertLead(lead);
    return lead;
  }

  @Transactional
  public CrmLead assignLead(String crmLeadId, String agentId) {
    CrmLead lead = getCrmLead(crmLeadId);
    StaffUser agent = getUser(agentId);
    if (agent.role() != Role.AGENT || agent.status() != UserStatus.ACTIVE) {
      throw new AccessDeniedException("Lead can only be assigned to an active agent");
    }
    Instant now = Instant.now();
    jdbc.update(
        """
        UPDATE crm_leads SET
          status = ?,
          assigned_agent_id = ?,
          assigned_lead_id = ?,
          assigned_at = ?,
          updated_at = ?
        WHERE id = ?
        """,
        LeadStatus.ASSIGNED.name(),
        agent.id(),
        agent.leadId(),
        CrmRowMappers.ts(now),
        CrmRowMappers.ts(now),
        crmLeadId);
    return getCrmLead(crmLeadId);
  }

  @Transactional
  public CrmLead updateLeadStatus(String crmLeadId, LeadStatus status) {
    Instant now = Instant.now();
    int updated =
        jdbc.update(
            "UPDATE crm_leads SET status = ?, updated_at = ? WHERE id = ?",
            status.name(),
            CrmRowMappers.ts(now),
            crmLeadId);
    if (updated == 0) {
      throw new NotFoundException("CRM lead not found");
    }
    return getCrmLead(crmLeadId);
  }

  public CrmLead getCrmLead(String crmLeadId) {
    try {
      return jdbc.queryForObject(
          "SELECT * FROM crm_leads WHERE id = ?", CrmRowMappers.CRM_LEAD, crmLeadId);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("CRM lead not found");
    }
  }

  public List<CrmLead> listLeads() {
    return jdbc.query(
        "SELECT * FROM crm_leads ORDER BY created_at DESC", CrmRowMappers.CRM_LEAD);
  }

  public List<CrmLead> leadsForAgent(String agentId) {
    return jdbc.query(
        """
        SELECT * FROM crm_leads
        WHERE assigned_agent_id = ?
        ORDER BY updated_at DESC
        """,
        CrmRowMappers.CRM_LEAD,
        agentId);
  }

  public List<CrmLead> leadsForLead(String leadUserId) {
    return jdbc.query(
        """
        SELECT * FROM crm_leads
        WHERE assigned_lead_id = ?
        ORDER BY updated_at DESC
        """,
        CrmRowMappers.CRM_LEAD,
        leadUserId);
  }

  public List<CrmLead> searchLeads(String query) {
    String pattern = "%" + (query == null ? "" : query.trim().toLowerCase()) + "%";
    return jdbc.query(
        """
        SELECT * FROM crm_leads
        WHERE LOWER(lead_id) LIKE ?
           OR LOWER(COALESCE(client_id, '')) LIKE ?
           OR LOWER(COALESCE(mobile_number, '')) LIKE ?
           OR LOWER(title) LIKE ?
           OR LOWER(COALESCE(loan_account_number, '')) LIKE ?
           OR LOWER(COALESCE(loan_application_id, '')) LIKE ?
        ORDER BY updated_at DESC
        """,
        CrmRowMappers.CRM_LEAD,
        pattern,
        pattern,
        pattern,
        pattern,
        pattern,
        pattern);
  }

  public TicketSummary addTicket(
      String leadId, String subject, String category, TicketPriority priority) {
    Instant now = Instant.now();
    TicketSummary ticket =
        new TicketSummary(
            "fd-local-" + UUID.randomUUID(),
            leadId,
            subject,
            TicketStatus.OPEN,
            Optional.ofNullable(priority).orElse(TicketPriority.MEDIUM),
            category == null ? "General" : category,
            now,
            now);
    jdbc.update(
        """
        INSERT INTO crm_tickets (id, lead_id, subject, status, priority, category, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """,
        ticket.id(),
        ticket.leadId(),
        ticket.subject(),
        ticket.status().name(),
        ticket.priority().name(),
        ticket.category(),
        CrmRowMappers.ts(ticket.createdAt()),
        CrmRowMappers.ts(ticket.updatedAt()));
    return ticket;
  }

  public List<TicketSummary> ticketsForLead(String leadId) {
    return jdbc.query(
        """
        SELECT * FROM crm_tickets
        WHERE lead_id = ?
        ORDER BY updated_at DESC
        """,
        CrmRowMappers.TICKET,
        leadId);
  }

  public CallEvent addCall(CallEvent call) {
    upsertCallRecord(call);
    return call;
  }

  @Transactional
  public boolean upsertCallBySid(CallEvent call) {
    String sid = call.callSid() == null ? call.id() : call.callSid();
    List<String> existing =
        jdbc.query(
            "SELECT id FROM call_logs WHERE call_sid = ?",
            (rs, rowNum) -> rs.getString("id"),
            sid);
    String id = existing.isEmpty() ? call.id() : existing.get(0);
    CallEvent toSave =
        new CallEvent(
            id,
            call.callSid(),
            call.parentCallSid(),
            call.leadId(),
            call.agentId(),
            call.direction(),
            call.disposition(),
            call.callStatus(),
            call.phoneNumber(),
            call.fromNumber(),
            call.toNumber(),
            call.durationSeconds(),
            call.recordingUrl(),
            call.sourceChannel(),
            call.freshdeskTicketId(),
            call.startedAt(),
            call.endedAt(),
            call.syncStatus(),
            call.createdAt() == null ? Instant.now() : call.createdAt(),
            Instant.now());
    upsertCallRecord(toSave);
    return existing.isEmpty();
  }

  public List<CallEvent> listCalls() {
    return jdbc.query(
        "SELECT * FROM call_logs ORDER BY started_at DESC NULLS LAST",
        CrmRowMappers.CALL);
  }

  public List<CallEvent> callsForAgent(String agentId) {
    return jdbc.query(
        """
        SELECT * FROM call_logs
        WHERE agent_id = ?
        ORDER BY started_at DESC NULLS LAST
        """,
        CrmRowMappers.CALL,
        agentId);
  }

  public List<CallEvent> callsForLead(String leadId) {
    return jdbc.query(
        """
        SELECT * FROM call_logs
        WHERE lead_id = ?
        ORDER BY started_at DESC NULLS LAST
        """,
        CrmRowMappers.CALL,
        leadId);
  }

  public Optional<CrmLead> findLeadByLeadId(String leadId) {
    List<CrmLead> leads =
        jdbc.query(
            "SELECT * FROM crm_leads WHERE lead_id = ? LIMIT 1",
            CrmRowMappers.CRM_LEAD,
            leadId);
    return leads.stream().findFirst();
  }

  public Optional<CrmLead> findLeadByLan(String lan) {
    List<CrmLead> leads =
        jdbc.query(
            """
            SELECT * FROM crm_leads
            WHERE LOWER(loan_account_number) = LOWER(?)
            LIMIT 1
            """,
            CrmRowMappers.CRM_LEAD,
            lan);
    return leads.stream().findFirst();
  }

  public Optional<CrmLead> findLeadByLoanApplicationId(String loanApplicationId) {
    List<CrmLead> leads =
        jdbc.query(
            """
            SELECT * FROM crm_leads
            WHERE LOWER(loan_application_id) = LOWER(?)
            LIMIT 1
            """,
            CrmRowMappers.CRM_LEAD,
            loanApplicationId);
    return leads.stream().findFirst();
  }

  public void updateCallTicketLink(String callId, String freshdeskTicketId, String sourceChannel) {
    jdbc.update(
        """
        UPDATE call_logs
        SET freshdesk_ticket_id = ?, source_channel = ?, updated_at = ?
        WHERE id = ?
        """,
        freshdeskTicketId,
        sourceChannel,
        CrmRowMappers.ts(Instant.now()),
        callId);
  }

  public AgentNote addNote(AgentNote note) {
    jdbc.update(
        """
        INSERT INTO agent_notes (id, lead_id, agent_id, disposition, note, follow_up_at, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """,
        note.id(),
        note.leadId(),
        note.agentId(),
        note.disposition(),
        note.note(),
        CrmRowMappers.ts(note.followUpAt()),
        CrmRowMappers.ts(note.createdAt()));
    return note;
  }

  public List<AgentNote> notesForLead(String leadId) {
    return jdbc.query(
        """
        SELECT * FROM agent_notes
        WHERE lead_id = ?
        ORDER BY created_at DESC
        """,
        CrmRowMappers.NOTE,
        leadId);
  }

  public PiiRevealAudit addPiiRevealAudit(PiiRevealAudit audit) {
    jdbc.update(
        """
        INSERT INTO pii_reveal_audits (id, lead_id, agent_id, field_name, reason, created_at)
        VALUES (?, ?, ?, ?, ?, ?)
        """,
        audit.id(),
        audit.leadId(),
        audit.agentId(),
        audit.field().name(),
        audit.reason(),
        CrmRowMappers.ts(audit.createdAt()));
    return audit;
  }

  public List<PiiRevealAudit> piiRevealAuditsForLead(String leadId) {
    return jdbc.query(
        """
        SELECT * FROM pii_reveal_audits
        WHERE lead_id = ?
        ORDER BY created_at DESC
        """,
        CrmRowMappers.PII_AUDIT,
        leadId);
  }

  public void saveSession(String token, String userId) {
    jdbc.update(
        """
        INSERT INTO auth_sessions (token, user_id, created_at)
        VALUES (?, ?, ?)
        ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id, created_at = EXCLUDED.created_at
        """,
        token,
        userId,
        CrmRowMappers.ts(Instant.now()));
  }

  public void deleteSession(String token) {
    if (token != null) {
      jdbc.update("DELETE FROM auth_sessions WHERE token = ?", token);
    }
  }

  public Optional<String> findUserIdByToken(String token) {
    List<String> userIds =
        jdbc.query(
            "SELECT user_id FROM auth_sessions WHERE token = ?",
            (rs, rowNum) -> rs.getString("user_id"),
            token);
    return userIds.stream().findFirst();
  }

  @Transactional
  protected Optional<StaffUser> selectNextAgent() {
    List<StaffUser> agents = activeAgents();
    if (agents.isEmpty()) {
      return Optional.empty();
    }
    Integer cursor =
        jdbc.queryForObject(
            """
            UPDATE crm_assignment_state
            SET cursor_value = cursor_value + 1
            WHERE state_key = 'round_robin'
            RETURNING cursor_value
            """,
            Integer.class);
    int index = Math.floorMod(cursor == null ? 0 : cursor - 1, agents.size());
    return Optional.of(agents.get(index));
  }

  private void insertLead(CrmLead lead) {
    jdbc.update(
        """
        INSERT INTO crm_leads (
          id, lead_id, client_id, mobile_number, title, loan_account_number, loan_application_id,
          status, priority, source, assigned_agent_id, assigned_lead_id, assigned_at, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        lead.id(),
        lead.leadId(),
        lead.clientId(),
        lead.mobileNumber(),
        lead.title(),
        lead.loanAccountNumber(),
        lead.loanApplicationId(),
        lead.status().name(),
        lead.priority().name(),
        lead.source().name(),
        lead.assignedAgentId(),
        lead.assignedLeadId(),
        CrmRowMappers.ts(lead.assignedAt()),
        CrmRowMappers.ts(lead.createdAt()),
        CrmRowMappers.ts(lead.updatedAt()));
  }

  private void upsertCallRecord(CallEvent call) {
    jdbc.update(
        """
        INSERT INTO call_logs (
          id, call_sid, parent_call_sid, lead_id, agent_id, call_direction, disposition, call_status,
          phone_number, from_number, to_number, duration_seconds, recording_url, source_channel,
          freshdesk_ticket_id, sync_status, started_at, ended_at, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
          call_sid = EXCLUDED.call_sid,
          parent_call_sid = EXCLUDED.parent_call_sid,
          lead_id = EXCLUDED.lead_id,
          agent_id = EXCLUDED.agent_id,
          call_direction = EXCLUDED.call_direction,
          disposition = EXCLUDED.disposition,
          call_status = EXCLUDED.call_status,
          phone_number = EXCLUDED.phone_number,
          from_number = EXCLUDED.from_number,
          to_number = EXCLUDED.to_number,
          duration_seconds = EXCLUDED.duration_seconds,
          recording_url = EXCLUDED.recording_url,
          source_channel = EXCLUDED.source_channel,
          freshdesk_ticket_id = EXCLUDED.freshdesk_ticket_id,
          sync_status = EXCLUDED.sync_status,
          started_at = EXCLUDED.started_at,
          ended_at = EXCLUDED.ended_at,
          updated_at = EXCLUDED.updated_at
        """,
        call.id(),
        call.callSid(),
        call.parentCallSid(),
        call.leadId(),
        call.agentId(),
        call.direction() == null ? null : call.direction().name(),
        call.disposition() == null ? null : call.disposition().name(),
        call.callStatus(),
        call.phoneNumber(),
        call.fromNumber(),
        call.toNumber(),
        call.durationSeconds(),
        call.recordingUrl(),
        call.sourceChannel(),
        call.freshdeskTicketId(),
        call.syncStatus() == null ? null : call.syncStatus().name(),
        CrmRowMappers.ts(call.startedAt()),
        CrmRowMappers.ts(call.endedAt()),
        CrmRowMappers.ts(call.createdAt()),
        CrmRowMappers.ts(call.updatedAt()));
  }
}
