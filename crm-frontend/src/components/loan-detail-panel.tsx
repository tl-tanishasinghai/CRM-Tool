'use client'

import { useEffect, useState } from 'react'
import { fetchLoanDetail } from '@/lib/api'
import type { LoanDetailResponse } from '@/lib/types'

type LoanDetailTab = 'summary' | 'rps' | 'transactions'

export function LoanDetailPanel({
  leadId,
  loanAccountNumber
}: {
  leadId: string
  loanAccountNumber: string
}) {
  const [detailTab, setDetailTab] = useState<LoanDetailTab>('summary')
  const [detail, setDetail] = useState<LoanDetailResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [rpsOpen, setRpsOpen] = useState(true)

  useEffect(() => {
    setLoading(true)
    setDetail(null)
    setDetailTab('summary')
    fetchLoanDetail(leadId, loanAccountNumber)
      .then(setDetail)
      .catch(() => setDetail(null))
      .finally(() => setLoading(false))
  }, [leadId, loanAccountNumber])

  if (loading) {
    return <p className="meta">Loading loan details…</p>
  }

  if (!detail) {
    return <p className="empty-state">Loan details unavailable for this account.</p>
  }

  return (
    <div className="loan-detail-stack">
      <section className="loan-fr2-header">
        <div>
          <p className="eyebrow">Loan account</p>
          <h3>{detail.productName}</h3>
          <p className="meta">
            {detail.loanAccountNumber} · {detail.lenderName} · Source {detail.dataSource}
          </p>
        </div>
        <span className={`status-pill ${tone(detail.normalizedStatus)}`}>
          {detail.normalizedStatus}
        </span>
      </section>

      <div className="loan-detail-tabs" role="tablist">
        {(['summary', 'rps', 'transactions'] as LoanDetailTab[]).map((tab) => (
          <button
            className={`loan-detail-tab ${detailTab === tab ? 'active' : ''}`}
            key={tab}
            type="button"
            onClick={() => setDetailTab(tab)}
          >
            {tab === 'summary' ? 'Summary' : tab === 'rps' ? 'RPS' : 'Transactions'}
          </button>
        ))}
      </div>

      {detailTab === 'summary' && (
        <div className="loan-detail-panel">
          <div className="fr2-grid">
            <Fr2Block title="Loan summary">
              <Fr2Row label="Disbursed" value={money(detail.summary.disbursedAmount)} />
              <Fr2Row label="Net disbursed" value={money(detail.summary.netDisbursedAmount)} />
              <Fr2Row label="PF / charges" value={money(detail.summary.processingFeeAndCharges)} />
              <Fr2Row label="Tenure" value={`${detail.summary.tenureMonths || '—'} months`} />
              <Fr2Row label="ROI" value={`${detail.summary.roiPercent}%`} />
              <Fr2Row label="Disbursement date" value={detail.summary.disbursementDate || '—'} />
            </Fr2Block>

            <Fr2Block title="Current position">
              <Fr2Row label="Principal O/S" value={money(detail.currentPosition.principalOutstanding)} />
              <Fr2Row label="Total O/S" value={money(detail.currentPosition.totalOutstanding)} />
              <Fr2Row label="Next due date" value={detail.currentPosition.nextInstallmentDate || 'N/A'} />
              <Fr2Row label="Next EMI" value={money(detail.currentPosition.nextInstallmentAmount)} />
              <Fr2Row
                label="Next EMI breakup"
                value={breakup(detail.currentPosition.nextInstallmentBreakup)}
              />
              <Fr2Row label="Overdue" value={money(detail.currentPosition.overdueAmount)} />
              <Fr2Row label="DPD" value={String(detail.currentPosition.dpdDays ?? detail.dpdDays ?? 0)} />
            </Fr2Block>

            <Fr2Block title="Lifetime totals">
              <Fr2Row label="Principal paid" value={money(detail.lifetimeTotals.principalPaid)} />
              <Fr2Row label="Interest paid" value={money(detail.lifetimeTotals.interestPaid)} />
              <Fr2Row label="Charges paid" value={money(detail.lifetimeTotals.chargesPaid)} />
              <Fr2Row label="Penalties paid" value={money(detail.lifetimeTotals.penaltiesPaid)} />
              <Fr2Row label="Total paid" value={money(detail.lifetimeTotals.totalPaid)} />
            </Fr2Block>

            <Fr2Block title="Application">
              <Fr2Row label="Application ID" value={detail.applicationStatus.loanApplicationId || '—'} />
              <Fr2Row label="Received" value={detail.applicationStatus.receivedDate || '—'} />
              <Fr2Row label="LSP" value={detail.applicationStatus.lspName || '—'} />
              <Fr2Row label="Stage" value={detail.applicationStatus.stage || '—'} />
            </Fr2Block>
          </div>

          {detail.foreclosureQuote.available && (
            <div className="fr2-callout">
              <strong>Foreclosure quote</strong>
              <p>
                Total due {money(detail.foreclosureQuote.totalDue)} · Valid until{' '}
                {detail.foreclosureQuote.validUntil}
              </p>
              {detail.foreclosureQuote.breakup && (
                <p className="meta">{breakup(detail.foreclosureQuote.breakup)}</p>
              )}
            </div>
          )}

          {detail.recentPayments.length > 0 && (
            <div className="fr2-block">
              <p className="section-title">Recent payments</p>
              <div className="recent-payments">
                {detail.recentPayments.map((payment) => (
                  <div className="recent-payment-row" key={`${payment.date}-${payment.amount}`}>
                    <strong>{payment.date}</strong>
                    <span>{money(payment.amount)}</span>
                    <span>{payment.type}</span>
                    <span>{payment.status}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {detail.documentsAvailable.length > 0 && (
            <div className="document-actions">
              {detail.documentsAvailable.map((doc) => (
                <button className="secondary compact" key={doc} type="button">
                  Send {doc.replace('_', ' ')}
                </button>
              ))}
            </div>
          )}
        </div>
      )}

      {detailTab === 'rps' && (
        <div className="loan-detail-panel">
          <button
            className="collapse-toggle"
            type="button"
            onClick={() => setRpsOpen((value) => !value)}
          >
            <span>Repayment schedule</span>
            <span>{rpsOpen ? 'Hide' : 'Show'}</span>
          </button>
          {rpsOpen && <RpsTable schedule={detail.repaymentSchedule} />}
        </div>
      )}

      {detailTab === 'transactions' && (
        <div className="loan-detail-panel">
          <TransactionsTable history={detail.transactions} />
        </div>
      )}
    </div>
  )
}

function Fr2Block({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div className="fr2-block">
      <p className="section-title">{title}</p>
      <div className="fr2-rows">{children}</div>
    </div>
  )
}

function Fr2Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="fr2-row">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function RpsTable({ schedule }: { schedule: LoanDetailResponse['repaymentSchedule'] }) {
  if (!schedule.periods.length) {
    return <p className="empty-state">Repayment schedule unavailable.</p>
  }

  return (
    <div className="data-table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th>Period</th>
            <th>Date</th>
            <th>Principal</th>
            <th>Interest</th>
            <th>Due</th>
            <th>Paid</th>
            <th>Outstanding</th>
          </tr>
        </thead>
        <tbody>
          {schedule.periods.map((row, index) => (
            <tr key={`${row.period ?? 'd'}-${index}`}>
              <td>{row.period ?? '—'}</td>
              <td>{row.date}</td>
              <td>{money(row.principal)}</td>
              <td>{money(row.interest)}</td>
              <td>{money(row.due)}</td>
              <td>{money(row.paid)}</td>
              <td>{money(row.outstanding)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function TransactionsTable({ history }: { history: LoanDetailResponse['transactions'] }) {
  if (!history.transactions.length) {
    return <p className="empty-state">No transactions found for this loan.</p>
  }

  return (
    <div className="data-table-wrap">
      <table className="data-table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Type</th>
            <th>Amount</th>
            <th>Status</th>
          </tr>
        </thead>
        <tbody>
          {history.transactions.map((row, index) => (
            <tr key={`${row.transactionType}-${row.transactionDate}-${index}`}>
              <td>{row.transactionDate}</td>
              <td>{row.transactionType}</td>
              <td>{money(row.amount)}</td>
              <td>{row.transactionStatus}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function breakup(value: {
  principal: number
  interest: number
  charges: number
  penalties: number
}) {
  return `P ${money(value.principal)} · I ${money(value.interest)} · C ${money(value.charges)} · Pen ${money(value.penalties)}`
}

function tone(status: string) {
  const normalized = status.toLowerCase()
  if (normalized.includes('overdue') || normalized.includes('reject')) return 'danger'
  if (normalized.includes('closed') || normalized.includes('active')) return 'success'
  return 'warning'
}

function money(value?: number | null) {
  return `₹${Number(value || 0).toLocaleString('en-IN')}`
}
