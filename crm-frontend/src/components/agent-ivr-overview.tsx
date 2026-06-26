'use client'

import { FormEvent, useEffect, useState } from 'react'
import { fetchIvrOverview } from '@/lib/api'
import type { IvrCallRow, IvrOverviewResponse } from '@/lib/types'

type Props = {
  onOpenTicket?: (ticketId: string, leadId?: string | null) => void
  onOpenCustomer?: (leadId: string) => void
}

type AssignmentTab = 'assigned' | 'unassigned'

export function AgentIvrOverview({ onOpenTicket, onOpenCustomer }: Props) {
  const [data, setData] = useState<IvrOverviewResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [assignment, setAssignment] = useState<AssignmentTab>('assigned')
  const [filters, setFilters] = useState({
    query: '',
    leadId: '',
    mobileNumber: '',
    loanAccountNumber: '',
    disposition: '',
    page: 0
  })

  async function load(next = filters, nextAssignment: AssignmentTab = assignment) {
    setLoading(true)
    try {
      const response = await fetchIvrOverview({
        assignment: nextAssignment,
        query: next.query || undefined,
        leadId: next.leadId || undefined,
        mobileNumber: next.mobileNumber || undefined,
        loanAccountNumber: next.loanAccountNumber || undefined,
        disposition: next.disposition || undefined,
        page: next.page,
        size: 25
      })
      setData(response)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load().catch(() => undefined)
  }, [])

  async function handleAssignmentChange(next: AssignmentTab) {
    setAssignment(next)
    const nextFilters = { ...filters, page: 0 }
    setFilters(nextFilters)
    await load(nextFilters, next)
  }

  async function handleFilterSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const next = { ...filters, page: 0 }
    setFilters(next)
    await load(next)
  }

  async function handlePageChange(page: number) {
    const next = { ...filters, page }
    setFilters(next)
    await load(next)
  }

  return (
    <div className="ivr-overview">
      <div className="panel ivr-intro">
        <div>
          <p className="eyebrow">IVR overview</p>
          <h3>GreyLabs bot + Exotel call history</h3>
          <p className="meta">
            Assigned calls from the last 24 hours. GreyLabs bot and agent inbound paths both route
            through Exotel — source is shown as a read-only badge on each row.
          </p>
        </div>
        {data && (
          <div className="ivr-stat-strip">
            <IvrStat label="Total calls" value={data.summary.total} />
            <IvrStat label="GreyLabs bot" value={data.summary.greylabsBot} />
            <IvrStat label="With ticket" value={data.summary.escalatedToTicket} />
            <IvrStat label="Callbacks" value={data.summary.callbackRequested} />
          </div>
        )}
      </div>

      <div className="mode-tabs" style={{ marginBottom: 16 }}>
        <button
          className={`mode-tab ${assignment === 'assigned' ? 'active' : ''}`}
          onClick={() => handleAssignmentChange('assigned').catch(() => undefined)}
          type="button"
        >
          Assigned to me
        </button>
        <button
          className={`mode-tab ${assignment === 'unassigned' ? 'active' : ''}`}
          onClick={() => handleAssignmentChange('unassigned').catch(() => undefined)}
          type="button"
        >
          Not assigned
        </button>
      </div>

      <section className="panel">
        <form className="filter-grid" onSubmit={handleFilterSubmit}>
          <input
            placeholder="Search summary, lead, ticket, call ID…"
            value={filters.query}
            onChange={(event) => setFilters((c) => ({ ...c, query: event.target.value }))}
          />
          <input
            placeholder="Lead ID"
            value={filters.leadId}
            onChange={(event) => setFilters((c) => ({ ...c, leadId: event.target.value }))}
          />
          <input
            placeholder="Mobile number"
            value={filters.mobileNumber}
            onChange={(event) => setFilters((c) => ({ ...c, mobileNumber: event.target.value }))}
          />
          <input
            placeholder="Loan account (LAN)"
            value={filters.loanAccountNumber}
            onChange={(event) =>
              setFilters((c) => ({ ...c, loanAccountNumber: event.target.value }))
            }
          />
          <select
            value={filters.disposition}
            onChange={(event) => setFilters((c) => ({ ...c, disposition: event.target.value }))}
          >
            <option value="">All dispositions</option>
            <option value="CONNECTED">Connected</option>
            <option value="CALLBACK_REQUESTED">Callback requested</option>
            <option value="ESCALATED">Escalated</option>
            <option value="RESOLVED">Resolved</option>
            <option value="NOT_CONNECTED">Not connected</option>
          </select>
          <button type="submit">Search</button>
        </form>
      </section>

      <section className="panel" style={{ marginTop: 16 }}>
        <div className="ivr-table-head">
          <h3>
            Call log {data ? `(${data.total})` : ''} · Last 24h ·{' '}
            {assignment === 'assigned' ? 'Assigned' : 'Not assigned'}
          </h3>
          {loading && <span className="meta">Loading…</span>}
        </div>

        <div className="ivr-table-wrap">
          <table className="ivr-table">
            <thead>
              <tr>
                <th>Time</th>
                <th>Source</th>
                <th>Agent</th>
                <th>Lead / LAN</th>
                <th>Mobile</th>
                <th>Summary</th>
                <th>Ticket</th>
                <th>Disposition</th>
              </tr>
            </thead>
            <tbody>
              {(data?.calls || []).map((call) => (
                <IvrCallRowView
                  call={call}
                  expanded={expandedId === call.callId}
                  key={call.callId}
                  onOpenCustomer={onOpenCustomer}
                  onOpenTicket={onOpenTicket}
                  onToggle={() =>
                    setExpandedId((current) => (current === call.callId ? null : call.callId))
                  }
                />
              ))}
            </tbody>
          </table>
        </div>

        {data && data.total > data.size && (
          <div className="inline-actions" style={{ marginTop: 12 }}>
            <button
              className="secondary"
              disabled={data.page <= 0}
              onClick={() => handlePageChange(data.page - 1)}
              type="button"
            >
              Previous
            </button>
            <span className="meta">
              Page {data.page + 1} of {Math.ceil(data.total / data.size)}
            </span>
            <button
              className="secondary"
              disabled={(data.page + 1) * data.size >= data.total}
              onClick={() => handlePageChange(data.page + 1)}
              type="button"
            >
              Next
            </button>
          </div>
        )}
      </section>
    </div>
  )
}

function IvrCallRowView({
  call,
  expanded,
  onToggle,
  onOpenTicket,
  onOpenCustomer
}: {
  call: IvrCallRow
  expanded: boolean
  onToggle: () => void
  onOpenTicket?: (ticketId: string, leadId?: string | null) => void
  onOpenCustomer?: (leadId: string) => void
}) {
  return (
    <>
      <tr className={expanded ? 'ivr-row-expanded' : ''} onClick={onToggle} style={{ cursor: 'pointer' }}>
        <td>{formatTime(call.startedAt)}</td>
        <td>
          <SourceBadge source={call.callSource} />
        </td>
        <td>{call.agentName || '—'}</td>
        <td>
          {call.leadId ? (
            <button
              className="link-button"
              onClick={(event) => {
                event.stopPropagation()
                onOpenCustomer?.(call.leadId!)
              }}
              type="button"
            >
              {call.leadId}
            </button>
          ) : (
            '—'
          )}
          <div className="meta">{call.loanAccountNumber || '—'}</div>
        </td>
        <td>{call.mobileNumber || '—'}</td>
        <td className="ivr-summary-cell">{truncate(call.callSummary, 72)}</td>
        <td>
          {call.freshdeskTicketId ? (
            <button
              className="link-button ticket-link"
              onClick={(event) => {
                event.stopPropagation()
                onOpenTicket?.(call.freshdeskTicketId!, call.leadId)
              }}
              type="button"
            >
              {call.freshdeskTicketRef || call.freshdeskTicketId}
            </button>
          ) : (
            <span className="meta">—</span>
          )}
          {call.ticketStatus && <div className="meta">{call.ticketStatus}</div>}
        </td>
        <td>
          <span className={`status-pill compact ${dispositionTone(call.disposition)}`}>
            {call.disposition.replace(/_/g, ' ')}
          </span>
        </td>
      </tr>
      {expanded && (
        <tr className="ivr-detail-row">
          <td colSpan={8}>
            <div className="ivr-detail-panel">
              <p>
                <strong>Call summary</strong>
              </p>
              <p>{call.callSummary}</p>
              <div className="ivr-detail-grid">
                <Detail label="Agent" value={call.agentName} />
                <Detail label="Call SID" value={call.callSid} />
                <Detail label="Client ID" value={call.clientId || '—'} />
                <Detail
                  label="Duration"
                  value={call.durationSeconds ? `${call.durationSeconds}s` : '—'}
                />
                <Detail label="Direction" value={call.direction} />
                <Detail
                  label="Category"
                  value={[call.categoryL1, call.categoryL2, call.categoryL3]
                    .filter(Boolean)
                    .join(' › ')}
                />
              </div>
              {call.freshdeskTicketId && (
                <button
                  className="secondary"
                  onClick={() => onOpenTicket?.(call.freshdeskTicketId!, call.leadId)}
                  type="button"
                >
                  Open ticket in Support →
                </button>
              )}
            </div>
          </td>
        </tr>
      )}
    </>
  )
}

function SourceBadge({ source }: { source: string }) {
  const isBot = source === 'GREYLABS_BOT'
  return (
    <span className={`context-pill ${isBot ? 'warning' : ''}`}>
      {isBot ? 'GreyLabs' : 'Agent / Inbound'}
    </span>
  )
}

function IvrStat({ label, value }: { label: string; value: number }) {
  return (
    <div className="ivr-stat">
      <p className="eyebrow">{label}</p>
      <strong>{value}</strong>
    </div>
  )
}

function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="eyebrow">{label}</p>
      <p>{value}</p>
    </div>
  )
}

function formatTime(value: string) {
  return new Date(value).toLocaleString(undefined, {
    day: '2-digit',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function truncate(value: string, max: number) {
  if (value.length <= max) {
    return value
  }
  return `${value.slice(0, max - 1)}…`
}

function dispositionTone(disposition: string) {
  if (disposition === 'ESCALATED' || disposition === 'CALLBACK_REQUESTED') return 'warning'
  if (disposition === 'RESOLVED' || disposition === 'CONNECTED') return 'success'
  if (disposition === 'NOT_CONNECTED') return 'danger'
  return ''
}
