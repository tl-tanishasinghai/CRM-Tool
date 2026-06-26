'use client'

import { FormEvent, useEffect, useMemo, useState } from 'react'
import {
  createCallEvent,
  createTicket,
  fetchCustomerProfile,
  fetchDashboard,
  searchCustomersByField
} from '@/lib/api'
import type { CustomerDashboard, CustomerProfileResponse } from '@/lib/types'
import {
  CustomerOverview,
  CustomerTabBar,
  LoanTabs,
  TabShell
} from '@/components/customer-workspace'
import { LoanDetailPanel } from '@/components/loan-detail-panel'
import { AgentFreshdeskBucket } from '@/components/agent-freshdesk-bucket'
import { AgentIvrOverview } from '@/components/agent-ivr-overview'
import { WorkflowPanel } from '@/components/workflow-panel'

type WorkspaceMode = 'workspace' | 'support' | 'ivr' | 'workflow'
type CustomerTab = 'overview' | 'loans' | 'activity'
type SearchField = 'mobile' | 'leadId' | 'lan' | 'la' | 'email'

const SEARCH_FIELDS: { value: SearchField; label: string; placeholder: string }[] = [
  { value: 'mobile', label: 'Mobile', placeholder: '10-digit mobile number' },
  { value: 'leadId', label: 'Lead ID', placeholder: 'LOS lead ID' },
  { value: 'lan', label: 'LAN', placeholder: 'Loan account number' },
  { value: 'la', label: 'LA', placeholder: 'Loan application ID' },
  { value: 'email', label: 'Email', placeholder: 'Customer email' }
]

export function AgentDashboard() {
  const [mode, setMode] = useState<WorkspaceMode>('workspace')
  const [selectedLeadId, setSelectedLeadId] = useState('')
  const [highlightLan, setHighlightLan] = useState('')
  const [selectedLoanId, setSelectedLoanId] = useState('')
  const [dashboard, setDashboard] = useState<CustomerDashboard | null>(null)
  const [customerProfile, setCustomerProfile] = useState<CustomerProfileResponse | null>(null)
  const [searchField, setSearchField] = useState<SearchField>('mobile')
  const [searchValue, setSearchValue] = useState('')
  const [inboundMobile, setInboundMobile] = useState('')
  const [customerFound, setCustomerFound] = useState<boolean | null>(null)
  const [lastMatchLabel, setLastMatchLabel] = useState('')
  const [ticketSubject, setTicketSubject] = useState('')
  const [callDisposition, setCallDisposition] = useState('CONNECTED')
  const [activeTab, setActiveTab] = useState<CustomerTab>('overview')
  const [searching, setSearching] = useState(false)
  const [supportTicketId, setSupportTicketId] = useState('')

  const loanAccounts = customerProfile?.loanAccounts || []

  const selectedLoanIdResolved = useMemo(() => {
    if (selectedLoanId && loanAccounts.some((loan) => loan.loanAccountNumber === selectedLoanId)) {
      return selectedLoanId
    }
    if (highlightLan && loanAccounts.some((loan) => loan.loanAccountNumber === highlightLan)) {
      return highlightLan
    }
    return loanAccounts[0]?.loanAccountNumber || selectedLoanId
  }, [loanAccounts, highlightLan, selectedLoanId])

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
    if (
      !selectedLoanId
      || (!accounts.some((loan) => loan.loanAccountNumber === selectedLoanId)
        && highlightLan
        && accounts.some((loan) => loan.loanAccountNumber === highlightLan))
    ) {
      setSelectedLoanId(highlightLan || accounts[0]?.loanAccountNumber || '')
    }
  }

  function openLoanFromOverview(loanAccountNumber: string) {
    setSelectedLoanId(loanAccountNumber)
    setActiveTab('loans')
  }

  useEffect(() => {
    if (selectedLeadId && customerFound) {
      refreshDashboard().catch(() => undefined)
    }
  }, [selectedLeadId, customerFound])

  function openCustomer(leadId: string, lan?: string) {
    setMode('workspace')
    setSelectedLeadId(leadId)
    setHighlightLan(lan || '')
    setCustomerFound(true)
    setActiveTab('overview')
  }

  async function runFieldSearch(field: SearchField, value: string) {
    const trimmed = value.trim()
    if (!trimmed) {
      return
    }
    setSearching(true)
    try {
      const result = await searchCustomersByField(field, trimmed)
      setCustomerFound(result.customerFound)
      setLastMatchLabel(
        result.customerFound
          ? result.displayName || result.leadId || trimmed
          : `No customer for ${field}: ${trimmed}`
      )
      if (result.customerFound && result.leadId) {
        setSelectedLeadId(result.leadId)
        setHighlightLan(result.highlightLoanAccountNumber || '')
        setActiveTab('overview')
      } else {
        setSelectedLeadId('')
        setHighlightLan('')
        setDashboard(null)
        setCustomerProfile(null)
      }
    } finally {
      setSearching(false)
    }
  }

  async function handleSearch(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    await runFieldSearch(searchField, searchValue)
  }

  async function handleMockInbound() {
    const mobile = inboundMobile.trim()
    if (!mobile) {
      return
    }
    setSearchField('mobile')
    setSearchValue(mobile)
    await runFieldSearch('mobile', mobile)
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

  const searchPlaceholder =
    SEARCH_FIELDS.find((field) => field.value === searchField)?.placeholder || 'Search value'

  return (
    <div>
      <div className="page-header">
        <div>
          <p className="eyebrow">Agent desk</p>
          <h2>Customer workspace</h2>
        </div>
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
            <select
              aria-label="Search field"
              value={searchField}
              onChange={(event) => setSearchField(event.target.value as SearchField)}
            >
              {SEARCH_FIELDS.map((field) => (
                <option key={field.value} value={field.value}>
                  {field.label}
                </option>
              ))}
            </select>
            <input
              placeholder={searchPlaceholder}
              value={searchValue}
              onChange={(event) => setSearchValue(event.target.value)}
            />
            <input
              placeholder="Inbound call mobile"
              value={inboundMobile}
              onChange={(event) => setInboundMobile(event.target.value)}
            />
            <button className="secondary" type="button" onClick={handleMockInbound}>
              Open from call
            </button>
            <button disabled={searching || !searchValue.trim()} type="submit">
              {searching ? 'Searching…' : 'Search'}
            </button>
          </form>

          {lastMatchLabel && (
            <p className="search-hint">{lastMatchLabel}</p>
          )}

          <section className="workspace-stack">
            {customerFound === false ? (
              <div className="panel empty-state">
                <p>Customer not found for this {searchField}.</p>
                <p className="meta">
                  Create a ticket in the Support tab with the mobile number in Freshdesk, then link
                  the customer when identified.
                </p>
              </div>
            ) : !dashboard || !customerProfile ? (
              <div className="panel empty-state">
                Search by field or open an inbound call to load a customer profile.
              </div>
            ) : (
              <div className="panel workspace-body">
                <div className="customer-header">
                  <div>
                    <p className="eyebrow">Customer 360</p>
                    <h3>{customerProfile.identity.name}</h3>
                  </div>
                </div>

                <CustomerTabBar activeTab={activeTab} onChange={setActiveTab} />

                <TabShell>
                  {activeTab === 'overview' && (
                    <CustomerOverview
                      leadId={selectedLeadId}
                      profile={customerProfile}
                      highlightLoanAccountNumber={highlightLan}
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

                      <ActivityBlock title="Timeline" empty="Calls and tickets will appear here.">
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
