'use client'

import { useState } from 'react'
import { AdminIntegrationsHealth } from '@/components/admin-integrations-health'
import { AdminOpsOverview } from '@/components/admin-ops-overview'
import { AdminTicketExplorer } from '@/components/admin-ticket-explorer'
import { AdminUsers } from '@/components/admin-users'

type AdminTab = 'overview' | 'tickets' | 'users' | 'health'

export default function AdminPage() {
  const [tab, setTab] = useState<AdminTab>('overview')

  return (
    <div>
      <div className="tab-bar" style={{ marginBottom: 16 }}>
        <button
          type="button"
          className={tab === 'overview' ? '' : 'secondary'}
          onClick={() => setTab('overview')}
        >
          Overview
        </button>
        <button
          type="button"
          className={tab === 'tickets' ? '' : 'secondary'}
          onClick={() => setTab('tickets')}
        >
          Tickets
        </button>
        <button
          type="button"
          className={tab === 'users' ? '' : 'secondary'}
          onClick={() => setTab('users')}
        >
          Users
        </button>
        <button
          type="button"
          className={tab === 'health' ? '' : 'secondary'}
          onClick={() => setTab('health')}
        >
          Health
        </button>
      </div>

      {tab === 'overview' && <AdminOpsOverview />}
      {tab === 'tickets' && <AdminTicketExplorer />}
      {tab === 'users' && <AdminUsers />}
      {tab === 'health' && <AdminIntegrationsHealth />}
    </div>
  )
}
