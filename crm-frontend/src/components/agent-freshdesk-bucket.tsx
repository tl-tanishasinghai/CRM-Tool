'use client'

import { FormEvent, useEffect, useMemo, useState } from 'react'
import {
  fetchFreshdeskConfig,
  fetchFreshdeskConversations,
  linkFreshdeskCustomer,
  replyFreshdeskTicket,
  searchCustomersByField,
  syncFreshdeskTickets
} from '@/lib/api'
import type {
  FreshdeskAgentTicket,
  FreshdeskConversationEntry,
  FreshdeskTicketBucketResponse
} from '@/lib/types'

type Props = {
  onOpenCustomer?: (leadId: string) => void
  initialTicketId?: string
}

export function AgentFreshdeskBucket({ onOpenCustomer, initialTicketId }: Props) {
  const [bucket, setBucket] = useState<FreshdeskTicketBucketResponse | null>(null)
  const [selectedTicketId, setSelectedTicketId] = useState('')
  const [expandedTicketId, setExpandedTicketId] = useState<string | null>(null)
  const [expandedConversations, setExpandedConversations] = useState<FreshdeskConversationEntry[]>([])
  const [loadingConversations, setLoadingConversations] = useState(false)
  const [loading, setLoading] = useState(true)
  const [syncing, setSyncing] = useState(false)
  const [freshdeskBaseUrl, setFreshdeskBaseUrl] = useState('')
  const [reply, setReply] = useState('')
  const [linkLeadId, setLinkLeadId] = useState('')
  const [linking, setLinking] = useState(false)
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

  async function autoSync() {
    setSyncing(true)
    try {
      const data = await syncFreshdeskTickets()
      setBucket(data)
      if (initialTicketId && data.tickets.some((ticket) => ticket.id === initialTicketId)) {
        setSelectedTicketId(initialTicketId)
        setExpandedTicketId(initialTicketId)
      } else if (!selectedTicketId && data.tickets[0]) {
        setSelectedTicketId(data.tickets[0].id)
      }
    } finally {
      setSyncing(false)
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchFreshdeskConfig()
      .then((config) => setFreshdeskBaseUrl(config.freshdeskBaseUrl || ''))
      .catch(() => undefined)
    autoSync().catch(() => setLoading(false))
  }, [])

  useEffect(() => {
    if (initialTicketId && bucket?.tickets.some((ticket) => ticket.id === initialTicketId)) {
      setSelectedTicketId(initialTicketId)
      setExpandedTicketId(initialTicketId)
    }
  }, [initialTicketId, bucket])

  useEffect(() => {
    if (!expandedTicketId) {
      setExpandedConversations([])
      return
    }
    setLoadingConversations(true)
    fetchFreshdeskConversations(expandedTicketId)
      .then(setExpandedConversations)
      .catch(() => setExpandedConversations([]))
      .finally(() => setLoadingConversations(false))
  }, [expandedTicketId])

  async function handleFilterSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    try {
      await autoSync()
    } finally {
      setLoading(false)
    }
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
    if (expandedTicketId === updated.id) {
      const conversations = await fetchFreshdeskConversations(updated.id)
      setExpandedConversations(conversations)
    }
  }

  async function handleLinkCustomer(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedTicket || !linkLeadId.trim()) {
      return
    }
    setLinking(true)
    try {
      const updated = await linkFreshdeskCustomer(selectedTicket.id, {
        leadId: linkLeadId.trim(),
        mobileNumber: selectedTicket.mobileNumber || undefined,
        loanAccountNumber: selectedTicket.loanAccountNumber || undefined
      })
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
      setLinkLeadId('')
    } finally {
      setLinking(false)
    }
  }

  async function handleLookupLeadForLink() {
    if (!selectedTicket?.requesterEmail) {
      return
    }
    const result = await searchCustomersByField('email', selectedTicket.requesterEmail)
    if (result.customerFound && result.leadId) {
      setLinkLeadId(result.leadId)
    }
  }

  function freshdeskTicketUrl(ticket: FreshdeskAgentTicket) {
    if (!freshdeskBaseUrl || !ticket.freshdeskId) {
      return null
    }
    return `${freshdeskBaseUrl.replace(/\/$/, '')}/a/tickets/${ticket.freshdeskId}`
  }

  return (
    <div className="freshdesk-bucket">
      <div className="panel freshdesk-summary">
        <div>
          <p className="eyebrow">Freshdesk bucket</p>
          <h3>Tickets assigned to you</h3>
          <p className="meta">
            {syncing
              ? 'Syncing from Freshdesk…'
              : bucket?.freshdeskConfigured
                ? 'Auto-synced from Freshdesk on tab open.'
                : 'Showing seeded tickets until Freshdesk credentials are configured.'}
          </p>
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
          <button type="submit">Refresh</button>
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
                onClick={() => {
                  setSelectedTicketId(ticket.id)
                  setExpandedTicketId((current) =>
                    current === ticket.id ? null : ticket.id
                  )
                }}
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
                  <ChannelBadge channel={ticket.sourceChannel} />
                  <span className="meta">{ticket.requesterName}</span>
                </div>
                <p className="meta">
                  {ticket.channel} · {ticket.conversationCount ?? ticket.conversations.length}{' '}
                  comment{(ticket.conversationCount ?? ticket.conversations.length) === 1 ? '' : 's'}{' '}
                  · {formatDate(ticket.updatedAt)}
                </p>
                {expandedTicketId === ticket.id && (
                  <div className="freshdesk-inline-thread" onClick={(event) => event.stopPropagation()}>
                    {loadingConversations ? (
                      <p className="meta">Loading conversation…</p>
                    ) : (
                      expandedConversations.map((entry) => (
                        <div
                          className={`conversation-item ${entry.agentReply ? 'agent' : ''}`}
                          key={entry.id}
                        >
                          <strong>{entry.author}</strong>
                          <p>{entry.body}</p>
                          <span className="meta">{formatDate(entry.createdAt)}</span>
                        </div>
                      ))
                    )}
                  </div>
                )}
              </button>
            ))}
        </div>

        {selectedTicket && (
          <div className="panel freshdesk-detail">
            <TicketDetail
              freshdeskUrl={freshdeskTicketUrl(selectedTicket)}
              onOpenCustomer={onOpenCustomer}
              ticket={selectedTicket}
            />

            {!selectedTicket.leadId && (
              <form className="inline-form" onSubmit={handleLinkCustomer}>
                <p className="section-title">Link customer</p>
                <div className="form-row">
                  <input
                    placeholder="Lead ID from search"
                    value={linkLeadId}
                    onChange={(event) => setLinkLeadId(event.target.value)}
                  />
                  <button
                    className="secondary"
                    disabled={!selectedTicket.requesterEmail}
                    onClick={() => handleLookupLeadForLink().catch(() => undefined)}
                    type="button"
                  >
                    Lookup by email
                  </button>
                  <button disabled={linking || !linkLeadId.trim()} type="submit">
                    {linking ? 'Linking…' : 'Link customer'}
                  </button>
                </div>
              </form>
            )}

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
  freshdeskUrl,
  onOpenCustomer,
  ticket
}: {
  freshdeskUrl: string | null
  onOpenCustomer?: (leadId: string) => void
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
        <div className="header-actions">
          <ChannelBadge channel={ticket.sourceChannel} />
          {freshdeskUrl && (
            <a className="secondary button-link" href={freshdeskUrl} rel="noreferrer" target="_blank">
              Open in Freshdesk
            </a>
          )}
          {ticket.leadId && onOpenCustomer && (
            <button className="secondary" type="button" onClick={() => onOpenCustomer(ticket.leadId)}>
              Open customer 360
            </button>
          )}
        </div>
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
    </>
  )
}

function ChannelBadge({ channel }: { channel?: string | null }) {
  if (!channel) {
    return null
  }
  const normalized = channel.toLowerCase()
  const label = normalized === 'greylabs_bot' ? 'GreyLabs bot' : 'Agent'
  const tone = normalized === 'greylabs_bot' ? 'warning' : ''
  return <span className={`context-pill ${tone}`}>{label}</span>
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
