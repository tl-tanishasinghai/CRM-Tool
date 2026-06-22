'use client'

import { FormEvent, useEffect, useState } from 'react'
import { assignLead, fetchFreshdeskTeamBucket, fetchOpsOverview, fetchTeamQueue } from '@/lib/api'
import type { FreshdeskBucketSummary, OpsOverviewResponse, TeamQueueResponse } from '@/lib/types'

export function LeadOversight() {
  const [data, setData] = useState<TeamQueueResponse | null>(null)
  const [ops, setOps] = useState<OpsOverviewResponse | null>(null)
  const [fdBuckets, setFdBuckets] = useState<FreshdeskBucketSummary | null>(null)
  const [selectedLead, setSelectedLead] = useState('')
  const [selectedAgent, setSelectedAgent] = useState('')

  async function refresh() {
    const [queue, overview, freshdesk] = await Promise.all([
      fetchTeamQueue(),
      fetchOpsOverview(7),
      fetchFreshdeskTeamBucket()
    ])
    setData(queue)
    setOps(overview)
    setFdBuckets(freshdesk.summary)
    setSelectedLead(queue.leads[0]?.id || '')
    setSelectedAgent(queue.agents[0]?.id || '')
  }

  useEffect(() => {
    refresh().catch(() => undefined)
  }, [])

  async function handleAssign(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedLead || !selectedAgent) {
      return
    }
    await assignLead(selectedLead, selectedAgent)
    await refresh()
  }

  if (!data) {
    return <p>Loading team queue...</p>
  }

  const crmBuckets = data.crmBuckets

  return (
    <div>
      <div className="page-header">
        <div>
          <p className="eyebrow">Lead oversight</p>
          <h2>Team workload, buckets, and reassignment</h2>
        </div>
      </div>

      {ops && (
        <div className="grid four">
          <Metric label="CRM resolution (7d)" value={`${ops.rates.crmResolutionRate}%`} />
          <Metric label="FD resolution (7d)" value={`${ops.rates.freshdeskResolutionRate}%`} />
          <Metric label="Open CRM backlog" value={ops.rates.crmOpenBacklog} />
          <Metric label="Open FD backlog" value={ops.rates.freshdeskOpenBacklog} />
        </div>
      )}

      <div className="grid two" style={{ marginTop: 16 }}>
        {crmBuckets && (
          <section className="panel">
            <h3>CRM queue buckets</h3>
            <div className="bucket-strip">
              <BucketChip label="New" value={crmBuckets.newCount} />
              <BucketChip label="Assigned" value={crmBuckets.assignedCount} />
              <BucketChip label="In progress" value={crmBuckets.inProgressCount} />
              <BucketChip label="Follow-up" value={crmBuckets.followUpCount} />
              <BucketChip label="Escalated" value={crmBuckets.escalatedCount} />
              <BucketChip label="Closed" value={crmBuckets.closedCount} />
            </div>
          </section>
        )}
        {fdBuckets && (
          <section className="panel">
            <h3>Freshdesk buckets (team)</h3>
            <div className="bucket-strip">
              <BucketChip label="Open" value={fdBuckets.openCount} />
              <BucketChip label="Pending" value={fdBuckets.pendingCount} />
              <BucketChip label="Resolved" value={fdBuckets.resolvedCount} />
              <BucketChip label="Closed" value={fdBuckets.closedCount} />
            </div>
          </section>
        )}
      </div>

      <div className="grid two" style={{ marginTop: 16 }}>
        <section className="panel">
          <h3>Manual reassignment</h3>
          <form className="grid" onSubmit={handleAssign}>
            <select value={selectedLead} onChange={(event) => setSelectedLead(event.target.value)}>
              {data.leads.map((lead) => (
                <option key={lead.id} value={lead.id}>
                  {lead.leadId} · {lead.status} · current owner {lead.assignedAgentId || 'unassigned'}
                </option>
              ))}
            </select>
            <select value={selectedAgent} onChange={(event) => setSelectedAgent(event.target.value)}>
              {data.agents.map((agent) => (
                <option key={agent.id} value={agent.id}>
                  {agent.name}
                </option>
              ))}
            </select>
            <button>Reassign</button>
          </form>
        </section>

        <section className="panel">
          <h3>Agent load</h3>
          <table>
            <thead>
              <tr>
                <th>Agent</th>
                <th>Assigned</th>
              </tr>
            </thead>
            <tbody>
              {data.agents.map((agent) => (
                <tr key={agent.id}>
                  <td>{agent.name}</td>
                  <td>{data.leads.filter((lead) => lead.assignedAgentId === agent.id).length}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </section>
      </div>

      {ops && ops.agents.length > 0 && (
        <section className="panel" style={{ marginTop: 16 }}>
          <h3>Agent performance (7d)</h3>
          <table>
            <thead>
              <tr>
                <th>Agent</th>
                <th>Open CRM</th>
                <th>Open FD</th>
                <th>Resolved CRM</th>
                <th>Resolved FD</th>
                <th>Avg handle (hrs)</th>
              </tr>
            </thead>
            <tbody>
              {ops.agents.map((agent) => (
                <tr key={agent.agentId}>
                  <td>{agent.agentName}</td>
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
      )}

      <section className="panel" style={{ marginTop: 16 }}>
        <h3>Team queue</h3>
        <table>
          <thead>
            <tr>
              <th>Lead</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Source</th>
              <th>Agent</th>
            </tr>
          </thead>
          <tbody>
            {data.leads.map((lead) => (
              <tr key={lead.id}>
                <td>{lead.leadId}</td>
                <td>{lead.status}</td>
                <td>{lead.priority}</td>
                <td>{lead.source}</td>
                <td>{lead.assignedAgentId || 'Unassigned'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </div>
  )
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <section className="panel">
      <p className="eyebrow">{label}</p>
      <h2>{value}</h2>
    </section>
  )
}

function BucketChip({ label, value }: { label: string; value: number }) {
  return (
    <div className="bucket-chip">
      <p className="eyebrow">{label}</p>
      <strong>{value}</strong>
    </div>
  )
}
