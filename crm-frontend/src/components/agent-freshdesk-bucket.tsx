'use client'

import { FormEvent, useEffect, useMemo, useState } from 'react'
import {
  fetchFreshdeskBucket,
  replyFreshdeskTicket,
  syncFreshdeskTickets,
  updateFreshdeskTicket
} from '@/lib/api'
import type { FreshdeskAgentTicket, FreshdeskTicketBucketResponse } from '@/lib/types'

type Props = {
  onOpenCustomer?: (leadId: string) => void
  initialTicketId?: string
}

export function AgentFreshdeskBucket({ onOpenCustomer, initialTicketId }: Props) {
  const [bucket, setBucket] = useState<FreshdeskTicketBucketResponse | null>(null)
  const [selectedTicketId, setSelectedTicketId] = useState('')
  const [loading, setLoading] = useState(true)
  const [syncing, setSyncing] = useState(false)
  const [reply, setReply] = useState('')
  const [filters, setFilters] = useState({
    query: '',
    priority: '',
    mobileNumber: '',
    loanAccountNumber: '',
    createdFrom: '',
    createdTo: '',
    closedFrom: '',
    closedTo: ''
  })

  const selectedTicket = useMemo(
    () => bucket?.tickets.find((ticket) => ticket.id === selectedTicketId),
    [bucket, selectedTicketId]
  )

  async function loadBucket(nextFilters = filters) {
    setLoading(true)
    try {
      const data = await fetchFreshdeskBucket({
        query: nextFilters.query || undefined,
        priority: nextFilters.priority || undefined,
        mobileNumber: nextFilters.mobileNumber || undefined,
        loanAccountNumber: nextFilters.loanAccountNumber || undefined,
        createdFrom: nextFilters.createdFrom || undefined,
        createdTo: nextFilters.createdTo || undefined,
        closedFrom: nextFilters.closedFrom || undefined,
        closedTo: nextFilters.closedTo || undefined
      })
      setBucket(data)
      if (initialTicketId && data.tickets.some((ticket) => ticket.id === initialTicketId)) {
        setSelectedTicketId(initialTicketId)
      } else if (!selectedTicketId && data.tickets[0]) {
        setSelectedTicketId(data.tickets[0].id)
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadBucket().catch(() => undefined)
  }, [])

  useEffect(() => {
    if (initialTicketId && bucket?.tickets.some((ticket) => ticket.id === initialTicketId)) {
      setSelectedTicketId(initialTicketId)
    }
  }, [initialTicketId, bucket])

  async function handleSync() {
    setSyncing(true)
    try {
      const data = await syncFreshdeskTickets()
      setBucket(data)
    } finally {
      setSyncing(false)
    }
  }

  async function handleFilterSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await loadBucket(filters)
  }

  async function handleReply(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedTicket || !reply.trim()) {
      return
    }
    const updated = await replyFreshdeskTicket(selectedTicket.id, reply.trim())
    setReply('')
    setBucket((current) =>
      current
        ? {
            ...current,
            tickets: current.tickets.map((ticket) =>
              ticket.id === updated.id ? updated : ticket
            )
          }
        : current
    )
  }

  async function handleUpdate(status?: string, priority?: string) {
    if (!selectedTicket) {
      return
    }
    const updated = await updateFreshdeskTicket(selectedTicket.id, { status, priority })
    setBucket((current) =>
      current
        ? {
            ...current,
            tickets: current.tickets.map((ticket) => (ticket.id === updated.id ? updated : ticket))
          }
        : current
    )
  }

  return (
    <div className="freshdesk-bucket">
      <div className="panel freshdesk-summary">
        <div>
          <p className="eyebrow">Freshdesk bucket</p>
          <h3>Tickets assigned to you</h3>
          <p className="meta">
            {bucket?.freshdeskConfigured
              ? 'Live Freshdesk sync is enabled.'
              : 'Showing seeded tickets until Freshdesk credentials are configured.'}
          </p>
        </div>
        <div className="header-actions">
          <button className="secondary" disabled={syncing} type="button" onClick={handleSync}>
            {syncing ? 'Syncing…' : 'Sync from Freshdesk'}
          </button>
        </div>
      </div>

      {bucket && (
        <div className="freshdesk-stats">
          <Stat label="Total" value={bucket.summary.total} />
          <Stat label="Open" value={bucket.summary.openCount} />
          <Stat label="Pending" value={bucket.summary.pendingCount} />
          <Stat label="Resolved" value={bucket.summary.resolvedCount} />
          <Stat label="Closed" value={bucket.summary.closedCount} />
        </div>
      )}

      <form className="panel freshdesk-filters" onSubmit={handleFilterSubmit}>
        <p className="section-title">Filters</p>
        <div className="filter-grid">
          <label>
            Search
            <input
              placeholder="Subject, ticket ID, requester…"
              value={filters.query}
              onChange={(event) => setFilters({ ...filters, query: event.target.value })}
            />
          </label>
          <label>
            Priority
            <select
              value={filters.priority}
              onChange={(event) => setFilters({ ...filters, priority: event.target.value })}
            >
              <option value="">All priorities</option>
              <option value="LOW">Low</option>
              <option value="MEDIUM">Medium</option>
              <option value="HIGH">High</option>
              <option value="URGENT">Urgent</option>
            </select>
          </label>
          <label>
            Mobile number
            <input
              placeholder="9999999999"
              value={filters.mobileNumber}
              onChange={(event) => setFilters({ ...filters, mobileNumber: event.target.value })}
            />
          </label>
          <label>
            Loan ID
            <input
              placeholder="LAN-1002001"
              value={filters.loanAccountNumber}
              onChange={(event) =>
                setFilters({ ...filters, loanAccountNumber: event.target.value })
              }
            />
          </label>
          <label>
            Created from
            <input
              type="date"
              value={filters.createdFrom}
              onChange={(event) => setFilters({ ...filters, createdFrom: event.target.value })}
            />
          </label>
          <label>
            Created to
            <input
              type="date"
              value={filters.createdTo}
              onChange={(event) => setFilters({ ...filters, createdTo: event.target.value })}
            />
          </label>
          <label>
            Closed from
            <input
              type="date"
              value={filters.closedFrom}
              onChange={(event) => setFilters({ ...filters, closedFrom: event.target.value })}
            />
          </label>
          <label>
            Closed to
            <input
              type="date"
              value={filters.closedTo}
              onChange={(event) => setFilters({ ...filters, closedTo: event.target.value })}
            />
          </label>
        </div>
        <div className="filter-actions">
          <button type="submit">Apply filters</button>
          <button
            className="secondary"
            type="button"
            onClick={() => {
              const cleared = {
                query: '',
                priority: '',
                mobileNumber: '',
                loanAccountNumber: '',
                createdFrom: '',
                createdTo: '',
                closedFrom: '',
                closedTo: ''
              }
              setFilters(cleared)
              loadBucket(cleared).catch(() => undefined)
            }}
          >
            Reset
          </button>
        </div>
      </form>

      <div className="freshdesk-layout">
        <div className="panel freshdesk-list">
          {loading && <p className="meta">Loading tickets…</p>}
          {!loading && !bucket?.tickets.length && (
            <p className="empty-state">No Freshdesk tickets match these filters.</p>
          )}
          {!loading &&
            bucket?.tickets.map((ticket) => (
              <button
                className={`freshdesk-row ${selectedTicketId === ticket.id ? 'active' : ''}`}
                key={ticket.id}
                type="button"
                onClick={() => setSelectedTicketId(ticket.id)}
              >
                <div className="freshdesk-row-top">
                  <strong>{ticket.subject}</strong>
                  <span className="meta">#{ticket.freshdeskId}</span>
                </div>
                <div className="freshdesk-row-meta">
                  <span className={`status-pill ${ticketTone(ticket.status)}`}>
                    {ticket.status}
                  </span>
                  <span className={`status-pill ${priorityTone(ticket.priority)}`}>
                    {ticket.priority}
                  </span>
                  <span className="meta">{ticket.requesterName}</span>
                </div>
                <p className="meta">
                  {ticket.channel} · {ticket.slaHint} · {formatDate(ticket.updatedAt)}
                </p>
              </button>
            ))}
        </div>

        {selectedTicket && (
          <div className="panel freshdesk-detail">
            <TicketDetail
              onOpenCustomer={onOpenCustomer}
              onUpdate={handleUpdate}
              ticket={selectedTicket}
            />
            <form onSubmit={handleReply}>
              <label>
                Add reply / comment
                <textarea
                  placeholder="Write a response to the customer…"
                  value={reply}
                  onChange={(event) => setReply(event.target.value)}
                />
              </label>
              <button className="secondary" disabled={!reply.trim()} type="submit">
                Post comment
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  )
}

function TicketDetail({
  onOpenCustomer,
  onUpdate,
  ticket
}: {
  onOpenCustomer?: (leadId: string) => void
  onUpdate: (status?: string, priority?: string) => Promise<void>
  ticket: FreshdeskAgentTicket
}) {
  return (
    <>
      <div className="customer-header">
        <div>
          <p className="eyebrow">Ticket #{ticket.freshdeskId}</p>
          <h3>{ticket.subject}</h3>
          <p className="meta">
            {ticket.requesterName} · {ticket.requesterEmail} · {ticket.mobileNumber}
          </p>
        </div>
        {ticket.leadId && onOpenCustomer && (
          <button className="secondary" type="button" onClick={() => onOpenCustomer(ticket.leadId)}>
            Open customer 360
          </button>
        )}
      </div>

      <div className="summary-grid">
        <Stat label="Status" value={ticket.status} />
        <Stat label="Priority" value={ticket.priority} />
        <Stat label="Loan account" value={ticket.loanAccountNumber || 'N/A'} />
        <Stat label="Category" value={ticket.category} />
        <Stat label="Created" value={formatDate(ticket.createdAt)} />
        <Stat label="Closed" value={ticket.closedAt ? formatDate(ticket.closedAt) : 'Open'} />
        <Stat label="Assignee" value={ticket.assigneeName} />
        <Stat label="SLA" value={ticket.slaHint} />
      </div>

      <div className="header-actions">
        <button className="secondary" type="button" onClick={() => onUpdate('PENDING')}>
          Mark pending
        </button>
        <button className="secondary" type="button" onClick={() => onUpdate('RESOLVED')}>
          Resolve
        </button>
        <button className="ghost" type="button" onClick={() => onUpdate('CLOSED')}>
          Close
        </button>
      </div>

      <section className="summary-section">
        <p className="section-title">Conversation</p>
        <div className="conversation-list">
          {ticket.conversations.map((entry) => (
            <div className={`conversation-item ${entry.agentReply ? 'agent' : ''}`} key={entry.id}>
              <strong>{entry.author}</strong>
              <p>{entry.body}</p>
              <span className="meta">{formatDate(entry.createdAt)}</span>
            </div>
          ))}
        </div>
      </section>
    </>
  )
}

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="summary-metric">
      <span className="summary-label">{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function ticketTone(status: string) {
  if (status === 'RESOLVED' || status === 'CLOSED') return 'success'
  if (status === 'PENDING') return 'warning'
  return ''
}

function priorityTone(priority: string) {
  if (priority === 'URGENT' || priority === 'HIGH') return 'danger'
  if (priority === 'MEDIUM') return 'warning'
  return ''
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}
