'use client'

import { useState } from 'react'

const stages = [
  {
    id: 'sources',
    title: 'Customer touchpoints',
    summary: 'Calls, tickets, and apps enter the CRM.',
    detail:
      'A customer may reach us through an inbound Exotel call, a Freshdesk support ticket, or an application already in LOS. Each touchpoint creates or links a lead using the mobile number or loan ID.',
    outputs: ['Mobile number', 'Lead / loan ID', 'Ticket subject']
  },
  {
    id: 'intake',
    title: 'CRM intake',
    summary: 'Leads are captured and deduplicated.',
    detail:
      'The CRM service stores the lead, checks for an existing customer profile, and attaches source metadata (call, ticket, manual search). Duplicate mobiles map to the same customer record.',
    outputs: ['Lead record', 'Priority & source', 'Assignment rules']
  },
  {
    id: 'queue',
    title: 'Agent queue',
    summary: 'Work is routed to the right agent.',
    detail:
      'New and follow-up items appear in the agent queue using round-robin assignment. Agents see only their assigned tickets and can also open a customer directly from search or an inbound call popup.',
    outputs: ['Assigned ticket', 'Status: New → In progress', 'Team oversight view']
  },
  {
    id: '360',
    title: 'Customer 360',
    summary: 'One screen for profile, loans, and history.',
    detail:
      'When an agent selects a ticket, the CRM pulls a unified dashboard: customer profile from LOS, all loans from LMS, Freshdesk tickets, Exotel call logs, and prior agent notes — without switching tools.',
    outputs: ['Profile & KYC hints', 'All loans & statuses', 'Tickets + calls + notes']
  },
  {
    id: 'actions',
    title: 'Agent actions',
    summary: 'Every interaction is logged in context.',
    detail:
      'Agents log call dispositions, add notes, create Freshdesk tickets, mark follow-ups, or escalate. Actions stay tied to the customer and ticket so the next agent sees full history.',
    outputs: ['Call disposition', 'Notes & follow-up', 'Escalation flag']
  },
  {
    id: 'sync',
    title: 'Data sync & reporting',
    summary: 'Stakeholders see outcomes, not raw systems.',
    detail:
      'Call metadata syncs nightly from Exotel. Ticket updates flow through Freshdesk. Loan balances refresh from LMS. Leads and team leads use this CRM view for operational reporting — no need to open four separate systems.',
    outputs: ['Daily call sync', 'Ticket status', 'Loan snapshots']
  }
]

export function WorkflowPanel() {
  const [activeStage, setActiveStage] = useState(stages[2].id)
  const selected = stages.find((stage) => stage.id === activeStage) || stages[0]

  return (
    <div className="workflow-panel">
      <div className="workflow-intro panel">
        <p className="eyebrow">How this tool works</p>
        <h3>Lead to resolution — in one flow</h3>
        <p className="meta">
          Click a step below to see what happens behind the scenes. This view is meant for
          operations and business stakeholders — no technical setup required.
        </p>
      </div>

      <div className="flow-track panel">
        {stages.map((stage, index) => (
          <div className="flow-segment" key={stage.id}>
            <button
              className={`flow-node ${activeStage === stage.id ? 'active' : ''}`}
              type="button"
              onClick={() => setActiveStage(stage.id)}
            >
              <span className="flow-index">{index + 1}</span>
              <strong>{stage.title}</strong>
              <span className="meta">{stage.summary}</span>
            </button>
            {index < stages.length - 1 && <span aria-hidden="true" className="flow-arrow" />}
          </div>
        ))}
      </div>

      <div className="workflow-detail panel">
        <p className="eyebrow">Step {stages.indexOf(selected) + 1}</p>
        <h3>{selected.title}</h3>
        <p>{selected.detail}</p>
        <div className="workflow-outputs">
          {selected.outputs.map((item) => (
            <span className="workflow-tag" key={item}>
              {item}
            </span>
          ))}
        </div>
      </div>

      <div className="workflow-map panel">
        <p className="eyebrow">System map</p>
        <div className="system-map">
          <MapGroup label="Inputs" items={['Exotel calls', 'Freshdesk', 'LOS / LMS']} tone="input" />
          <span className="map-arrow">→</span>
          <MapGroup label="CRM core" items={['Lead store', 'Assignment', 'Customer 360']} tone="core" />
          <span className="map-arrow">→</span>
          <MapGroup label="Outputs" items={['Agent actions', 'Reports', 'Follow-ups']} tone="output" />
        </div>
      </div>
    </div>
  )
}

function MapGroup({
  items,
  label,
  tone
}: {
  items: string[]
  label: string
  tone: 'input' | 'core' | 'output'
}) {
  return (
    <div className={`map-group map-group-${tone}`}>
      <span className="map-label">{label}</span>
      {items.map((item) => (
        <span className="map-item" key={item}>
          {item}
        </span>
      ))}
    </div>
  )
}
