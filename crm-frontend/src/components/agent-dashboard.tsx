'use client'

import { FormEvent, useEffect, useMemo, useState } from 'react'
import {
  createCallEvent,
  createNote,
  createTicket,
  fetchCustomerProfile,
  fetchDashboard,
  fetchMyQueue,
  ingestLead,
  searchCustomers,
  updateLeadStatus
} from '@/lib/api'
import type { CrmLead, CustomerDashboard, CustomerProfileResponse, SearchResult } from '@/lib/types'
import {
  CustomerOverview,
  CustomerTabBar,
  LoanTabs,
  TabShell,
  TicketTabs
} from '@/components/customer-workspace'
import { LoanDetailPanel } from '@/components/loan-detail-panel'
import { AgentFreshdeskBucket } from '@/components/agent-freshdesk-bucket'
import { AgentIvrOverview } from '@/components/agent-ivr-overview'
import { WorkflowPanel } from '@/components/workflow-panel'

type WorkspaceMode = 'workspace' | 'support' | 'ivr' | 'workflow'
type CustomerTab = 'overview' | 'loans' | 'activity'

export function AgentDashboard() {
  const [mode, setMode] = useState<WorkspaceMode>('workspace')
  const [queue, setQueue] = useState<CrmLead[]>([])
  const [selectedLeadId, setSelectedLeadId] = useState('')
  const [selectedTicketId, setSelectedTicketId] = useState('')
  const [selectedLoanId, setSelectedLoanId] = useState('')
  const [dashboard, setDashboard] = useState<CustomerDashboard | null>(null)
  const [customerProfile, setCustomerProfile] = useState<CustomerProfileResponse | null>(null)
  const [query, setQuery] = useState('')
  const [inboundMobile, setInboundMobile] = useState('')
  const [results, setResults] = useState<SearchResult[]>([])
  const [note, setNote] = useState('')
  const [ticketSubject, setTicketSubject] = useState('')
  const [callDisposition, setCallDisposition] = useState('CONNECTED')
  const [activeTab, setActiveTab] = useState<CustomerTab>('overview')
  const [searching, setSearching] = useState(false)
  const [supportTicketId, setSupportTicketId] = useState('')

  const selectedTicket = useMemo(
    () =>
      queue.find((item) => item.id === selectedTicketId)
      || queue.find((item) => item.leadId === selectedLeadId),
    [queue, selectedLeadId, selectedTicketId]
  )

  const loanAccounts = customerProfile?.loanAccounts || []

  const selectedLoanIdResolved = useMemo(() => {
    if (selectedLoanId && loanAccounts.some((loan) => loan.loanAccountNumber === selectedLoanId)) {
      return selectedLoanId
    }
    return loanAccounts[0]?.loanAccountNumber || selectedLoanId
  }, [loanAccounts, selectedLoanId])

  async function refreshQueue() {
    const data = await fetchMyQueue()
    setQueue(data)
    if (!selectedLeadId && data[0]) {
      setSelectedLeadId(data[0].leadId)
      setSelectedTicketId(data[0].id)
    }
  }

  async function refreshDashboard(leadId = selectedLeadId) {
    if (!leadId) {
      setDashboard(null)
      setCustomerProfile(null)
      return
    }
    const [dashboardData, profileData] = await Promise.all([
      fetchDashboard(leadId),
      fetchCustomerProfile(leadId)
    ])
    setDashboard(dashboardData)
    setCustomerProfile(profileData)
    const accounts = profileData.loanAccounts
    if (!selectedLoanId || !accounts.some((loan) => loan.loanAccountNumber === selectedLoanId)) {
      setSelectedLoanId(accounts[0]?.loanAccountNumber || '')
    }
  }

  function openLoanFromOverview(loanAccountNumber: string) {
    setSelectedLoanId(loanAccountNumber)
    setActiveTab('loans')
  }

  useEffect(() => {
    refreshQueue().catch(() => undefined)
  }, [])

  useEffect(() => {
    refreshDashboard().catch(() => undefined)
  }, [selectedLeadId])

  function openCustomer(leadId: string, ticketId?: string) {
    setMode('workspace')
    setSelectedLeadId(leadId)
    if (ticketId) {
      setSelectedTicketId(ticketId)
    }
    setActiveTab('overview')
  }

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!query.trim()) {
      return
    }
    setSearching(true)
    try {
      const data = await searchCustomers(query.trim())
      setResults(data)
      if (data[0]) {
        openCustomer(data[0].leadId)
      }
    } finally {
      setSearching(false)
    }
  }

  async function handleMockInbound() {
    const mobile = inboundMobile.trim()
    if (!mobile) {
      return
    }
    const existing = await searchCustomers(mobile)
    if (existing[0]) {
      setQuery(mobile)
      setResults(existing)
      openCustomer(existing[0].leadId)
      return
    }

    const lead = await ingestLead({
      leadId: String(Date.now()).slice(-7),
      mobileNumber: mobile,
      title: `Inbound call · ${mobile}`,
      source: 'INBOUND_CALL',
      priority: 'MEDIUM'
    })
    await refreshQueue()
    openCustomer(lead.leadId, lead.id)
  }

  async function handleAddNote(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!note.trim() || !selectedLeadId) {
      return
    }
    await createNote(selectedLeadId, note, callDisposition)
    setNote('')
    await refreshDashboard()
  }

  async function handleCreateTicket(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!ticketSubject.trim() || !selectedLeadId) {
      return
    }
    await createTicket(selectedLeadId, ticketSubject, ticketSubject)
    setTicketSubject('')
    await refreshDashboard()
  }

  async function handleLogCall() {
    if (!selectedLeadId) {
      return
    }
    await createCallEvent(
      selectedLeadId,
      customerProfile?.identity.mobileMasked || inboundMobile || '9999999999',
      callDisposition
    )
    await refreshDashboard()
  }

  async function handleStatus(status: string) {
    const lead = queue.find((item) => item.leadId === selectedLeadId)
    if (!lead) {
      return
    }
    await updateLeadStatus(lead.id, status)
    await refreshQueue()
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <p className="eyebrow">Agent desk</p>
          <h2>Customer workspace</h2>
        </div>
        <span className="badge">{queue.length} assigned</span>
      </div>

      <div className="mode-tabs">
        <button
          className={`mode-tab ${mode === 'workspace' ? 'active' : ''}`}
          type="button"
          onClick={() => setMode('workspace')}
        >
          Workspace
        </button>
        <button
          className={`mode-tab ${mode === 'support' ? 'active' : ''}`}
          type="button"
          onClick={() => setMode('support')}
        >
          Support
        </button>
        <button
          className={`mode-tab ${mode === 'ivr' ? 'active' : ''}`}
          type="button"
          onClick={() => setMode('ivr')}
        >
          IVR
        </button>
        <button
          className={`mode-tab ${mode === 'workflow' ? 'active' : ''}`}
          type="button"
          onClick={() => setMode('workflow')}
        >
          Workflow
        </button>
      </div>

      {mode === 'support' ? (
        <AgentFreshdeskBucket
          initialTicketId={supportTicketId}
          onOpenCustomer={(leadId) => {
            openCustomer(leadId)
          }}
        />
      ) : mode === 'ivr' ? (
        <AgentIvrOverview
          onOpenCustomer={(leadId) => {
            openCustomer(leadId)
            setMode('workspace')
          }}
          onOpenTicket={(ticketId, leadId) => {
            setSupportTicketId(ticketId)
            if (leadId) {
              openCustomer(leadId)
            }
            setMode('support')
          }}
        />
      ) : mode === 'workflow' ? (
        <WorkflowPanel />
      ) : (
        <>
          <form className="command-bar" onSubmit={handleSearch}>
            <input
              placeholder="Search mobile, lead ID, loan ID…"
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
            <input
              placeholder="Inbound call mobile"
              value={inboundMobile}
              onChange={(event) => setInboundMobile(event.target.value)}
            />
            <button className="secondary" type="button" onClick={handleMockInbound}>
              Open from call
            </button>
            <button disabled={searching || !query.trim()} type="submit">
              {searching ? 'Searching…' : 'Search'}
            </button>
          </form>

          {results.length > 0 && (
            <p className="search-hint">
              {results.length} match{results.length === 1 ? '' : 'es'} found.
            </p>
          )}

          <section className="workspace-stack">
            <div className="panel workspace-toolbar">
              <p className="eyebrow">Assigned tickets</p>
              <TicketTabs
                queue={queue}
                selectedTicketId={selectedTicketId}
                onSelect={openCustomer}
              />
            </div>

            {!dashboard || !customerProfile ? (
              <div className="panel empty-state">
                Select a ticket above or search to open a customer profile.
              </div>
            ) : (
              <div className="panel workspace-body">
                <div className="customer-header">
                  <div>
                    <p className="eyebrow">Customer 360</p>
                    <h3>{customerProfile.identity.name}</h3>
                  </div>
                  <div className="header-actions">
                    {selectedTicket && (
                      <span className={`status-pill ${statusTone(selectedTicket.status)}`}>
                        {selectedTicket.status.replace('_', ' ')}
                      </span>
                    )}
                    <button className="secondary" type="button" onClick={() => handleStatus('IN_PROGRESS')}>
                      Start
                    </button>
                    <button className="secondary" type="button" onClick={() => handleStatus('FOLLOW_UP')}>
                      Follow-up
                    </button>
                    <button className="ghost" type="button" onClick={() => handleStatus('ESCALATED')}>
                      Escalate
                    </button>
                  </div>
                </div>

                <CustomerTabBar activeTab={activeTab} onChange={setActiveTab} />

                <TabShell>
                  {activeTab === 'overview' && (
                    <CustomerOverview
                      leadId={selectedLeadId}
                      profile={customerProfile}
                      ticket={selectedTicket}
                      onSelectLoan={openLoanFromOverview}
                    />
                  )}

                  {activeTab === 'loans' && (
                    <>
                      <LoanTabs
                        loans={loanAccounts}
                        selectedLoanId={selectedLoanIdResolved}
                        onSelect={setSelectedLoanId}
                      />
                      {selectedLoanIdResolved ? (
                        <LoanDetailPanel
                          leadId={selectedLeadId}
                          loanAccountNumber={selectedLoanIdResolved}
                        />
                      ) : (
                        <p className="empty-state">No loans on this customer.</p>
                      )}
                    </>
                  )}

                  {activeTab === 'activity' && (
                    <>
                      <section className="summary-section">
                        <p className="section-title">Quick actions</p>
                        <div className="form-row">
                          <select
                            value={callDisposition}
                            onChange={(event) => setCallDisposition(event.target.value)}
                          >
                            <option value="CONNECTED">Connected</option>
                            <option value="NOT_CONNECTED">Not connected</option>
                            <option value="CALLBACK_REQUESTED">Callback requested</option>
                            <option value="ESCALATED">Escalated</option>
                            <option value="PROMISE_TO_PAY">Promise to pay</option>
                          </select>
                          <button type="button" onClick={handleLogCall}>
                            Log call
                          </button>
                        </div>
                        <form className="inline-form" onSubmit={handleCreateTicket}>
                          <input
                            placeholder="New support ticket subject"
                            value={ticketSubject}
                            onChange={(event) => setTicketSubject(event.target.value)}
                          />
                          <button className="secondary" type="submit">
                            Create ticket
                          </button>
                        </form>
                        <form onSubmit={handleAddNote}>
                          <textarea
                            placeholder="Add a note or disposition"
                            value={note}
                            onChange={(event) => setNote(event.target.value)}
                          />
                          <button className="secondary" style={{ marginTop: 10 }} type="submit">
                            Save note
                          </button>
                        </form>
                      </section>

                      <div className="grid two">
                        <ActivityBlock title="Tickets" empty="No support tickets yet.">
                          {dashboard.tickets.map((ticket) => (
                            <div className="compact-item" key={ticket.id}>
                              <div>
                                <strong>{ticket.subject}</strong>
                                <p className="meta">
                                  {ticket.status} · {ticket.priority}
                                </p>
                              </div>
                            </div>
                          ))}
                        </ActivityBlock>

                        <ActivityBlock title="Calls" empty="No calls logged yet.">
                          {dashboard.calls.map((call) => (
                            <div className="compact-item" key={call.id}>
                              <div>
                                <strong>
                                  {call.direction} · {call.disposition}
                                </strong>
                                <p className="meta">{call.phoneNumber}</p>
                              </div>
                            </div>
                          ))}
                        </ActivityBlock>
                      </div>

                      <ActivityBlock title="Recent activity" empty="Notes and updates will appear here.">
                        {dashboard.activity.map((item) => (
                          <div className="compact-item" key={item.id}>
                            <div>
                              <strong>{item.title}</strong>
                              <p className="meta">
                                {item.type} · {item.description}
                              </p>
                            </div>
                          </div>
                        ))}
                      </ActivityBlock>
                    </>
                  )}
                </TabShell>
              </div>
            )}
          </section>
        </>
      )}
    </div>
  )
}

function ActivityBlock({
  children,
  empty,
  title
}: {
  children: React.ReactNode
  empty: string
  title: string
}) {
  const items = Array.isArray(children) ? children.filter(Boolean) : children ? [children] : []
  return (
    <section className="summary-section">
      <p className="section-title">{title}</p>
      <div className="compact-list">
        {items.length ? items : <p className="empty-state">{empty}</p>}
      </div>
    </section>
  )
}

function statusTone(status: string) {
  if (status === 'ESCALATED') return 'danger'
  if (status === 'FOLLOW_UP') return 'warning'
  if (status === 'IN_PROGRESS' || status === 'CLOSED') return 'success'
  return ''
}
