'use client'

import { FormEvent, useState } from 'react'
import { useRouter } from 'next/navigation'
import { login } from '@/lib/api'
import { useSession } from '@/components/session-provider'
import type { Role } from '@/lib/types'

const demoRoles = [
  {
    role: 'AGENT' as Role,
    title: 'Agent workspace',
    email: 'agent1@trillionloans.com',
    href: '/agent',
    description: 'Assigned tickets, Customer 360, loans, IVR call log, and Freshdesk support.'
  },
  {
    role: 'LEAD' as Role,
    title: 'Lead oversight',
    email: 'lead@trillionloans.com',
    href: '/lead',
    description: 'Team queue, CRM + Freshdesk buckets, resolution rates, and reassignment.'
  },
  {
    role: 'ADMIN' as Role,
    title: 'Admin operations',
    email: 'admin@trillionloans.com',
    href: '/admin',
    description: 'Org-wide ticket buckets, resolution metrics, users, and integration health.'
  }
]

export default function LoginPage() {
  const router = useRouter()
  const { setUser } = useSession()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [demoLoading, setDemoLoading] = useState<string | null>(null)
  const [showDemo, setShowDemo] = useState(true)

  async function completeLogin(demoEmail: string, demoPassword: string, href: string) {
    const response = await login(demoEmail, demoPassword)
    localStorage.setItem('crmToken', response.token)
    setUser(response.user)
    router.replace(href)
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      const response = await login(email, password)
      localStorage.setItem('crmToken', response.token)
      setUser(response.user)
      if (response.user.role === 'ADMIN') {
        router.replace('/admin')
      } else if (response.user.role === 'LEAD') {
        router.replace('/lead')
      } else {
        router.replace('/agent')
      }
    } catch {
      setError('Invalid credentials. Check your email and password.')
    } finally {
      setLoading(false)
    }
  }

  async function handleDemoLogin(demoEmail: string, href: string) {
    setDemoLoading(demoEmail)
    setError('')
    try {
      await completeLogin(demoEmail, 'password', href)
    } catch {
      setError('Demo login failed. Check that the CRM backend is running on port 8092.')
    } finally {
      setDemoLoading(null)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card auth-card-wide">
        <div className="auth-brand">
          <div className="auth-logo-wrap">
            <img alt="BP Capital" src="/bp-capital-logo.png" />
          </div>
        </div>

        <div className="auth-body">
          <header className="auth-header">
            <span className="auth-badge">Internal operations</span>
            <h1>CRM Tool</h1>
          </header>

          <form className="auth-form" onSubmit={handleSubmit}>
            <label>
              Work email
              <input
                autoComplete="email"
                placeholder="you@trillionloans.com"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </label>
            <label>
              Password
              <input
                autoComplete="current-password"
                placeholder="Enter password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </label>
            {error && <p className="error">{error}</p>}
            <button disabled={loading || !email || !password} type="submit">
              {loading ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <footer className="auth-footer">
            <button className="auth-demo-toggle" type="button" onClick={() => setShowDemo((v) => !v)}>
              {showDemo ? 'Hide demo access' : 'Show demo access'}
            </button>
            {showDemo && (
              <div className="auth-demo-panel">
                <p className="auth-demo-lead">
                  Open any role instantly. All demo accounts use password{' '}
                  <strong>password</strong>.
                </p>
                <div className="auth-demo-grid">
                  {demoRoles.map((demo) => (
                    <article className="auth-demo-card" key={demo.role}>
                      <p className="eyebrow">{demo.role}</p>
                      <h3>{demo.title}</h3>
                      <p className="meta">{demo.description}</p>
                      <p className="auth-demo-email">{demo.email}</p>
                      <button
                        className="secondary"
                        disabled={demoLoading !== null}
                        onClick={() => handleDemoLogin(demo.email, demo.href)}
                        type="button"
                      >
                        {demoLoading === demo.email ? 'Opening…' : 'Open demo'}
                      </button>
                    </article>
                  ))}
                </div>
              </div>
            )}
          </footer>
        </div>
      </div>
    </div>
  )
}
