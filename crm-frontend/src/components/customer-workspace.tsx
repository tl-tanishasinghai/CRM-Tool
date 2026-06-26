'use client'

import { useState } from 'react'
import { revealCustomerField } from '@/lib/api'
import type { CrmLead, CustomerProfileResponse, LoanMiniSummary, LoanSummary, PiiField } from '@/lib/types'

type CustomerTab = 'overview' | 'loans' | 'activity'

export function TicketTabs({
  queue,
  selectedTicketId,
  onSelect
}: {
  queue: CrmLead[]
  selectedTicketId: string
  onSelect: (leadId: string, ticketId: string) => void
}) {
  if (!queue.length) {
    return <p className="empty-inline">No assigned tickets.</p>
  }

  return (
    <div className="ticket-tabs" role="tablist">
      {queue.map((ticket) => {
        const label = shortTicketLabel(ticket)
        return (
          <button
            aria-selected={selectedTicketId === ticket.id}
            className={`ticket-tab ${selectedTicketId === ticket.id ? 'active' : ''}`}
            key={ticket.id}
            role="tab"
            title={ticket.title || label}
            type="button"
            onClick={() => onSelect(ticket.leadId, ticket.id)}
          >
            {label}
          </button>
        )
      })}
    </div>
  )
}

export function CustomerTabBar({
  activeTab,
  onChange
}: {
  activeTab: CustomerTab
  onChange: (tab: CustomerTab) => void
}) {
  const tabs: { id: CustomerTab; label: string }[] = [
    { id: 'overview', label: 'Overview' },
    { id: 'loans', label: 'Loans' },
    { id: 'activity', label: 'Activity' }
  ]

  return (
    <div className="customer-tabs" role="tablist">
      {tabs.map((tab) => (
        <button
          aria-selected={activeTab === tab.id}
          className={`customer-tab ${activeTab === tab.id ? 'active' : ''}`}
          key={tab.id}
          role="tab"
          type="button"
          onClick={() => onChange(tab.id)}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}

export function TabShell({ children }: { children: React.ReactNode }) {
  return <div className="tab-shell">{children}</div>
}

export function LoanTabs({
  loans,
  selectedLoanId,
  onSelect
}: {
  loans: Array<LoanSummary | LoanMiniSummary>
  selectedLoanId: string
  onSelect: (loanAccountNumber: string) => void
}) {
  return (
    <div className="loan-tabs" role="tablist">
      {loans.map((loan) => (
        <button
          aria-selected={selectedLoanId === loan.loanAccountNumber}
          className={`loan-tab ${selectedLoanId === loan.loanAccountNumber ? 'active' : ''}`}
          key={loan.loanAccountNumber}
          role="tab"
          type="button"
          onClick={() => onSelect(loan.loanAccountNumber)}
        >
          {loan.product}
        </button>
      ))}
    </div>
  )
}

export function LoanAccountPanel({ loan }: { loan: LoanSummary }) {
  const paidPercent =
    loan.principal > 0 ? Math.min(100, Math.round((loan.paidAmount / loan.principal) * 100)) : 0
  const excessPaid = loan.excessAdjusted + loan.excessRefunded

  return (
    <div className="loan-account">
      <div className="loan-account-head">
        <div>
          <p className="eyebrow">Loan account</p>
          <h3>{loan.product}</h3>
          <div className="loan-account-meta">
            <MetaField label="Loan account no" value={loan.loanAccountNumber} />
            <MetaField label="Disbursement date" value={formatDate(loan.disbursementDate)} />
          </div>
        </div>
        <span className={`status-pill ${loanTone(loan.status)}`}>{loan.status}</span>
      </div>

      <div className="loan-summary-band">
        <div className="loan-progress">
          <div
            className="loan-progress-ring"
            style={{ background: `conic-gradient(var(--brand) ${paidPercent * 3.6}deg, #e2e8f0 0deg)` }}
          >
            <span>{paidPercent}%</span>
          </div>
          <small>Repaid</small>
        </div>

        <SummaryMetric label="Amount disbursed" value={money(loan.principal)} />
        <SummaryMetric label="Amount paid" value={money(loan.paidAmount)} />
        <SummaryMetric label="ROI" value={`${loan.interestRate}%`} />
        <SummaryMetric label="Tenure" value={`${loan.tenureMonths} months`} />
        <SummaryMetric label="Excess amount paid" value={money(excessPaid)} />

        <div className="loan-summary-block">
          <span className="summary-label">Loan status</span>
          <span className={`status-pill ${loanTone(loan.status)}`}>{loan.status}</span>
          <p className="meta">
            Recent transaction: {formatDate(loan.disbursementDate)}
            <br />
            Amount: {money(loan.emi || loan.principal)} · Type: Disbursement
          </p>
        </div>

        <div className="loan-summary-block">
          <span className="summary-label">Upcoming</span>
          <strong>{loan.nextDueDate ? formatDate(loan.nextDueDate) : 'N/A'}</strong>
          <p className="meta">Due amount: {money(loan.emi)}</p>
        </div>
      </div>

      <div className="loan-extra-grid">
        <DetailField label="Outstanding" value={money(loan.outstanding)} />
        <DetailField label="Remaining amount" value={money(loan.remainingAmount)} />
        <DetailField label="Installments left" value={String(loan.installmentRemaining)} />
        <DetailField label="Current stage" value={loan.currentStage} />
        <DetailField label="Application status" value={loan.applicationStatus} />
        <DetailField label="Lender" value={loan.lenderName} />
      </div>

      {loan.message && <p className="loan-note">{loan.message}</p>}
    </div>
  )
}

export function CustomerOverview({
  profile,
  leadId,
  highlightLoanAccountNumber,
  onSelectLoan
}: {
  profile: CustomerProfileResponse
  leadId: string
  highlightLoanAccountNumber?: string
  onSelectLoan?: (loanAccountNumber: string) => void
}) {
  const { identity, loanAccounts } = profile
  const [revealed, setRevealed] = useState<Partial<Record<PiiField, string>>>({})
  const [revealing, setRevealing] = useState<PiiField | null>(null)

  async function handleReveal(field: PiiField) {
    if (revealed[field]) {
      return
    }
    setRevealing(field)
    try {
      const response = await revealCustomerField(leadId, field, 'Inbound verification')
      setRevealed((current) => ({ ...current, [field]: response.value }))
    } finally {
      setRevealing(null)
    }
  }

  const primaryLoan =
    loanAccounts.find((loan) => loan.status.toLowerCase() === 'active') || loanAccounts[0]
  const activeLoans = loanAccounts.filter((loan) => loan.status.toLowerCase() === 'active').length

  return (
    <>
      <section className="identity-card">
        <div className="identity-card-head">
          <div>
            <p className="section-title">Customer identity</p>
          </div>
          <div className="identity-chips">
            <IdentityChip label="Lead" value={identity.leadId} />
            <IdentityChip label="UCIC" value={identity.ucic || identity.clientId} />
          </div>
        </div>

        <div className="identity-contact-grid">
          <MaskedField
            label="Mobile"
            masked={identity.mobileMasked}
            revealed={revealed.MOBILE}
            revealing={revealing === 'MOBILE'}
            onReveal={() => handleReveal('MOBILE')}
          />
          <MaskedField
            label="Email"
            masked={identity.emailMasked}
            revealed={revealed.EMAIL}
            revealing={revealing === 'EMAIL'}
            onReveal={() => handleReveal('EMAIL')}
          />
          <IdentityFact
            label="Date of birth"
            value={identity.dateOfBirth ? formatDate(identity.dateOfBirth) : 'Not on file'}
          />
          <IdentityFact label="PAN" value={identity.panLast4} />
        </div>

        <MaskedField
          label="Address"
          masked={identity.addressShort || identity.addressMasked}
          revealed={revealed.ADDRESS}
          revealing={revealing === 'ADDRESS'}
          onReveal={() => handleReveal('ADDRESS')}
          wide
        />
      </section>

      {loanAccounts.length > 0 && (
        <section className="loan-accounts-section">
          <div className="loan-accounts-head">
            <p className="section-title">Loan accounts (LAN)</p>
            <p className="identity-subtitle">Select a loan to open full details in the Loans tab</p>
          </div>
          <div className="loan-account-chips">
            {loanAccounts.map((loan) => (
              <button
                className={`loan-account-chip ${
                  highlightLoanAccountNumber === loan.loanAccountNumber ? 'highlighted' : ''
                }`}
                key={loan.loanAccountNumber}
                type="button"
                onClick={() => onSelectLoan?.(loan.loanAccountNumber)}
              >
                <span className={`status-pill compact ${loanTone(loan.status)}`}>{loan.status}</span>
                <span className="loan-partner-name">{loan.product}</span>
                <span className="loan-account-no">{loan.loanAccountNumber}</span>
              </button>
            ))}
          </div>
        </section>
      )}

      {primaryLoan && (
        <section className="loan-glance">
          <div className="loan-glance-head">
            <div>
              <p className="section-title">Loan at a glance</p>
              <p className="identity-subtitle">
                {activeLoans > 0
                  ? `${activeLoans} active loan${activeLoans > 1 ? 's' : ''} on file`
                  : `${loanAccounts.length} loan${loanAccounts.length > 1 ? 's' : ''} on file`}
                {' · '}Open the Loans tab for FR2 detail, schedules, and transactions
              </p>
            </div>
            <span className={`status-pill ${loanTone(primaryLoan.status)}`}>
              {primaryLoan.status}
            </span>
          </div>
          <div className="loan-glance-metrics">
            <SummaryMetric label="Product" value={primaryLoan.product} />
            <SummaryMetric label="Loan Account No" value={primaryLoan.loanAccountNumber} />
            <SummaryMetric label="Outstanding" value={money(primaryLoan.outstanding)} />
            <SummaryMetric label="EMI" value={money(primaryLoan.emi)} />
          </div>
        </section>
      )}
    </>
  )
}

function MaskedField({
  label,
  masked,
  revealed,
  revealing,
  onReveal,
  wide = false
}: {
  label: string
  masked: string
  revealed?: string
  revealing: boolean
  onReveal: () => void
  wide?: boolean
}) {
  return (
    <div className={`masked-field ${wide ? 'wide' : ''}`}>
      <span className="summary-label">{label}</span>
      <div className="masked-field-row">
        <strong>{revealed || masked}</strong>
        {!revealed && masked !== '—' && (
          <button className="ghost compact" disabled={revealing} type="button" onClick={onReveal}>
            {revealing ? 'Loading…' : 'Unmask'}
          </button>
        )}
      </div>
    </div>
  )
}

function IdentityChip({ label, value }: { label: string; value: string }) {
  return (
    <div className="identity-chip">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function IdentityFact({ label, value }: { label: string; value: string }) {
  return (
    <div className="identity-fact">
      <span className="summary-label">{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function statusTone(status: string) {
  const normalized = status.toLowerCase()
  if (normalized.includes('escalated')) return 'danger'
  if (normalized.includes('follow')) return 'warning'
  if (normalized.includes('progress') || normalized.includes('assigned')) return 'success'
  return 'warning'
}

function MetaField({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <span className="summary-label">{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function SummaryMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="summary-metric">
      <span className="summary-label">{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function DetailField({ label, value }: { label: string; value: string }) {
  return (
    <div className="detail-field">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function shortTicketLabel(ticket: CrmLead) {
  const title = ticket.title || `Lead ${ticket.leadId}`
  const words = title.split(/\s+/).slice(0, 3).join(' ')
  return words.length > 22 ? `${words.slice(0, 20)}…` : words
}

function loanTone(status: string) {
  const normalized = status.toLowerCase()
  if (normalized.includes('reject') || normalized.includes('overdue')) return 'danger'
  if (normalized.includes('closed')) return 'success'
  if (normalized.includes('active')) return 'success'
  return 'warning'
}

function formatDate(value?: string | null) {
  if (!value) return 'N/A'
  return new Date(value).toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric'
  })
}

function money(value?: number | null) {
  return `₹${Number(value || 0).toLocaleString('en-IN')}`
}
