'use client'

import { FormEvent, useEffect, useState } from 'react'
import { fetchCustomerSummary, fetchOpsTickets } from '@/lib/api'
import type { CustomerSummaryResponse, OpsTicketRow, OpsTicketsPage } from '@/lib/types'

const CRM_STATUSES = ['NEW', 'ASSIGNED', 'IN_PROGRESS', 'FOLLOW_UP', 'ESCALATED', 'CLOSED']
const FD_STATUSES = ['OPEN', 'PENDING', 'RESOLVED', 'CLOSED']

export function AdminTicketExplorer() {
  const [page, setPage] = useState<OpsTicketsPage | null>(null)
  const [selected, setSelected] = useState<OpsTicketRow | null>(null)
  const [summary, setSummary] = useState<CustomerSummaryResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [filters, setFilters] = useState({
    source: 'BOTH',
    status: '',
    mobileNumber: '',
    loanAccountNumber: '',
    page: 0
  })

  async function load(next = filters) {
    setLoading(true)
    try {
      const data = await fetchOpsTickets({
        source: next.source || undefined,
        status: next.status || undefined,
        mobileNumber: next.mobileNumber || undefined,
        loanAccountNumber: next.loanAccountNumber || undefined,
        page: next.page,
        size: 25
      })
      setPage(data)
      if (!selected && data.tickets[0]) {
        setSelected(data.tickets[0])
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load().catch(() => undefined)
  }, [])

  useEffect(() => {
    if (!selected?.leadId) {
      setSummary(null)
      return
    }
    fetchCustomerSummary(selected.leadId)
      .then(setSummary)
      .catch(() => setSummary(null))
  }, [selected])

  async function handleFilterSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const next = { ...filters, page: 0 }
    setFilters(next)
    await load(next)
  }

  async function handlePageChange(nextPage: number) {
    const next = { ...filters, page: nextPage }
    setFilters(next)
    await load(next)
  }

  const statusOptions =
    filters.source === 'CRM'
      ? CRM_STATUSES
      : filters.source === 'FRESHDESK'
        ? FD_STATUSES
        : [...CRM_STATUSES, ...FD_STATUSES]

  return (
    <div>
      <div className="page-header">
        <div>
          <p className="eyebrow">Ticket explorer</p>
          <h2>Cross-bucket ticket search (read-only)</h2>
        </div>
      </div>

      <section className="panel">
        <form className="grid four" onSubmit={handleFilterSubmit}>
          <select
            value={filters.source}
            onChange={(event) => setFilters((current) => ({ ...current, source: event.target.value }))}
          >
            <option value="BOTH">CRM + Freshdesk</option>
            <option value="CRM">CRM only</option>
            <option value="FRESHDESK">Freshdesk only</option>
          </select>
          <select
            value={filters.status}
            onChange={(event) => setFilters((current) => ({ ...current, status: event.target.value }))}
          >
            <option value="">All statuses</option>
            {statusOptions.map((status) => (
              <option key={status} value={status}>
                {status}
              </option>
            ))}
          </select>
          <input
            placeholder="Mobile number"
            value={filters.mobileNumber}
            onChange={(event) =>
              setFilters((current) => ({ ...current, mobileNumber: event.target.value }))
            }
          />
          <input
            placeholder="Loan account number"
            value={filters.loanAccountNumber}
            onChange={(event) =>
              setFilters((current) => ({ ...current, loanAccountNumber: event.target.value }))
            }
          />
          <button type="submit">Apply filters</button>
        </form>
      </section>

      <div className="grid two" style={{ marginTop: 16 }}>
        <section className="panel">
          <h3>
            Results {page ? `(${page.total})` : ''}
            {loading ? ' — loading' : ''}
          </h3>
          <table>
            <thead>
              <tr>
                <th>Source</th>
                <th>Title</th>
                <th>Status</th>
                <th>Assignee</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {(page?.tickets || []).map((ticket) => (
                <tr
                  key={`${ticket.source}-${ticket.id}`}
                  className={selected?.id === ticket.id ? 'selected-row' : ''}
                  onClick={() => setSelected(ticket)}
                  style={{ cursor: 'pointer' }}
                >
                  <td>{ticket.source}</td>
                  <td>{ticket.title}</td>
                  <td>{ticket.status}</td>
                  <td>{ticket.assigneeName || 'Unassigned'}</td>
                  <td>{formatDate(ticket.updatedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {page && page.total > page.size && (
            <div className="inline-actions" style={{ marginTop: 12 }}>
              <button
                type="button"
                className="secondary"
                disabled={page.page <= 0}
                onClick={() => handlePageChange(page.page - 1)}
              >
                Previous
              </button>
              <span className="meta">
                Page {page.page + 1} of {Math.ceil(page.total / page.size)}
              </span>
              <button
                type="button"
                className="secondary"
                disabled={(page.page + 1) * page.size >= page.total}
                onClick={() => handlePageChange(page.page + 1)}
              >
                Next
              </button>
            </div>
          )}
        </section>

        <section className="panel">
          <h3>Read-only customer context</h3>
          {!selected && <p className="meta">Select a ticket to view masked customer summary.</p>}
          {selected && (
            <div className="grid">
              <p>
                <strong>{selected.title}</strong>
              </p>
              <p className="meta">
                {selected.source} · {selected.status} · Lead {selected.leadId}
              </p>
              <p className="meta">
                Assignee: {selected.assigneeName || 'Unassigned'} · Team lead:{' '}
                {selected.teamLeadName || '-'}
              </p>
              <p className="meta">
                Mobile: {selected.mobileNumber || '-'} · LAN: {selected.loanAccountNumber || '-'}
              </p>
              {summary ? (
                <>
                  <p>
                    <strong>{summary.identity.name}</strong>
                  </p>
                  <p className="meta">Mobile: {summary.identity.mobileMasked}</p>
                  <p className="meta">Email: {summary.identity.emailMasked}</p>
                  <p className="meta">UCIC: {summary.identity.ucic}</p>
                  <p className="meta">
                    Loans on file: {summary.loanAccounts.length} · Source: {summary.dataSource}
                  </p>
                </>
              ) : (
                <p className="meta">Customer summary unavailable for this lead.</p>
              )}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}
