'use client'

import { useEffect, useState } from 'react'
import { fetchIntegrationsHealth } from '@/lib/api'
import type { IntegrationsHealthResponse } from '@/lib/types'

export function AdminIntegrationsHealth() {
  const [health, setHealth] = useState<IntegrationsHealthResponse | null>(null)

  useEffect(() => {
    fetchIntegrationsHealth()
      .then(setHealth)
      .catch(() => undefined)
  }, [])

  if (!health) {
    return <p>Loading integration health...</p>
  }

  return (
    <div>
      <div className="page-header">
        <div>
          <p className="eyebrow">Integrations</p>
          <h2>Upstream connectivity status</h2>
        </div>
      </div>

      <div className="grid two">
        <HealthCard
          title="Freshdesk"
          ok={health.freshdeskConfigured}
          detail={
            health.freshdeskConfigured
              ? `Last sync ${formatDate(health.freshdeskLastSync)}`
              : 'Set FRESHDESK_BASE_URL and FRESHDESK_API_KEY'
          }
        />
        <HealthCard
          title="Exotel"
          ok={health.exotelConfigured}
          detail={health.exotelStatus}
        />
        <HealthCard
          title="LOS / LMS"
          ok={health.losLmsLiveData}
          detail={
            health.losLmsLiveData
              ? 'Live data mode enabled (CRM_USE_LIVE_DATA=true)'
              : 'Mock fallback mode (CRM_USE_LIVE_DATA=false)'
          }
        />
      </div>
    </div>
  )
}

function HealthCard({
  title,
  ok,
  detail
}: {
  title: string
  ok: boolean
  detail: string
}) {
  return (
    <section className="panel">
      <p className="eyebrow">{title}</p>
      <h3 style={{ color: ok ? 'var(--success)' : 'var(--warning)' }}>
        {ok ? 'Configured' : 'Not configured'}
      </h3>
      <p className="meta">{detail}</p>
    </section>
  )
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}
