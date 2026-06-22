'use client'

import { useEffect, useState } from 'react'
import { fetchOpsOverview } from '@/lib/api'
import type { OpsOverviewResponse } from '@/lib/types'

type RangeDays = 7 | 30

export function AdminOpsOverview() {
  const [data, setData] = useState<OpsOverviewResponse | null>(null)
  const [rangeDays, setRangeDays] = useState<RangeDays>(7)
  const [loading, setLoading] = useState(true)

  async function refresh(days: RangeDays = rangeDays) {
    setLoading(true)
    try {
      setData(await fetchOpsOverview(days))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    refresh(rangeDays).catch(() => undefined)
  }, [rangeDays])

  if (loading && !data) {
    return <p>Loading operations overview...</p>
  }

  if (!data) {
    return <p>Unable to load operations overview.</p>
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <p className="eyebrow">Operations</p>
          <h2>Org-wide ticket buckets and resolution rates</h2>
        </div>
        <div className="inline-actions">
          <button
            type="button"
            className={rangeDays === 7 ? '' : 'secondary'}
            onClick={() => setRangeDays(7)}
          >
            7 days
          </button>
          <button
            type="button"
            className={rangeDays === 30 ? '' : 'secondary'}
            onClick={() => setRangeDays(30)}
          >
            30 days
          </button>
        </div>
      </div>

      <div className="grid four">
        <Metric
          label="CRM resolution"
          value={`${data.rates.crmResolutionRate}%`}
          hint={`${data.rates.crmOpenBacklog} open`}
        />
        <Metric
          label="Freshdesk resolution"
          value={`${data.rates.freshdeskResolutionRate}%`}
          hint={`${data.rates.freshdeskOpenBacklog} open`}
        />
        <Metric
          label="Escalation rate"
          value={`${data.rates.escalationRate}%`}
          hint="CRM escalated share"
        />
        <Metric
          label="Avg close time"
          value={formatHours(data.rates.avgCrmCloseHours, data.rates.avgFreshdeskCloseHours)}
          hint="CRM / Freshdesk hours"
        />
      </div>

      <div className="grid two" style={{ marginTop: 16 }}>
        <BucketPanel
          title="CRM queue buckets"
          items={[
            { label: 'New', value: data.crmBuckets.newCount },
            { label: 'Assigned', value: data.crmBuckets.assignedCount },
            { label: 'In progress', value: data.crmBuckets.inProgressCount },
            { label: 'Follow-up', value: data.crmBuckets.followUpCount },
            { label: 'Escalated', value: data.crmBuckets.escalatedCount },
            { label: 'Closed', value: data.crmBuckets.closedCount }
          ]}
        />
        <BucketPanel
          title="Freshdesk buckets"
          items={[
            { label: 'Open', value: data.freshdeskBuckets.openCount },
            { label: 'Pending', value: data.freshdeskBuckets.pendingCount },
            { label: 'Resolved', value: data.freshdeskBuckets.resolvedCount },
            { label: 'Closed', value: data.freshdeskBuckets.closedCount }
          ]}
        />
      </div>

      <section className="panel" style={{ marginTop: 16 }}>
        <h3>By team lead</h3>
        <table>
          <thead>
            <tr>
              <th>Team lead</th>
              <th>Agents</th>
              <th>Open CRM</th>
              <th>Open FD</th>
              <th>CRM resolved %</th>
              <th>FD resolved %</th>
              <th>Escalations</th>
            </tr>
          </thead>
          <tbody>
            {data.teams.map((team) => (
              <tr key={team.leadId}>
                <td>{team.leadName}</td>
                <td>{team.agentCount}</td>
                <td>{team.openCrm}</td>
                <td>{team.openFreshdesk}</td>
                <td>{team.crmResolutionRate}%</td>
                <td>{team.freshdeskResolutionRate}%</td>
                <td>{team.escalations}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>

      <section className="panel" style={{ marginTop: 16 }}>
        <h3>By agent</h3>
        <table>
          <thead>
            <tr>
              <th>Agent</th>
              <th>Team lead</th>
              <th>Open CRM</th>
              <th>Open FD</th>
              <th>Resolved CRM (7d)</th>
              <th>Resolved FD (7d)</th>
              <th>Avg handle (hrs)</th>
            </tr>
          </thead>
          <tbody>
            {data.agents.map((agent) => (
              <tr key={agent.agentId}>
                <td>{agent.agentName}</td>
                <td>{agent.teamLeadName || '-'}</td>
                <td>{agent.openCrm}</td>
                <td>{agent.openFreshdesk}</td>
                <td>{agent.resolvedCrm7d}</td>
                <td>{agent.resolvedFreshdesk7d}</td>
                <td>{agent.avgHandleHours ?? '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  )
}

function Metric({ label, value, hint }: { label: string; value: string; hint: string }) {
  return (
    <section className="panel">
      <p className="eyebrow">{label}</p>
      <h2>{value}</h2>
      <p className="meta">{hint}</p>
    </section>
  )
}

function BucketPanel({
  title,
  items
}: {
  title: string
  items: { label: string; value: number }[]
}) {
  return (
    <section className="panel">
      <h3>{title}</h3>
      <div className="bucket-strip">
        {items.map((item) => (
          <div key={item.label} className="bucket-chip">
            <p className="eyebrow">{item.label}</p>
            <strong>{item.value}</strong>
          </div>
        ))}
      </div>
    </section>
  )
}

function formatHours(crm?: number | null, fd?: number | null) {
  const crmText = crm == null ? '-' : `${crm}h`
  const fdText = fd == null ? '-' : `${fd}h`
  return `${crmText} / ${fdText}`
}
