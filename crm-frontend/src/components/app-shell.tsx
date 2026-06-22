'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useEffect } from 'react'
import { useSession } from './session-provider'
import type { Role } from '@/lib/types'

const navItems: { href: string; label: string; roles: Role[] }[] = [
  { href: '/agent', label: 'Agent', roles: ['AGENT', 'LEAD', 'ADMIN'] },
  { href: '/lead', label: 'Lead', roles: ['LEAD', 'ADMIN'] },
  { href: '/admin', label: 'Admin', roles: ['ADMIN'] }
]

export function AppShell({ children }: { children: React.ReactNode }) {
  const { user, loading, logout } = useSession()
  const pathname = usePathname()
  const router = useRouter()

  useEffect(() => {
    if (loading || !user) {
      return
    }
    if (pathname === '/admin' && user.role !== 'ADMIN') {
      router.replace('/agent')
    }
    if (pathname === '/lead' && user.role === 'AGENT') {
      router.replace('/agent')
    }
  }, [loading, pathname, router, user])

  if (pathname === '/login') {
    return <>{children}</>
  }

  if (loading || !user) {
    return <main className="main">Loading workspace…</main>
  }

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="sidebar-logo-wrap">
            <img alt="BP Capital" src="/bp-capital-logo.png" />
          </div>
        </div>
        <nav>
          {navItems
            .filter((item) => item.roles.includes(user.role))
            .map((item) => (
              <Link
                className={pathname.startsWith(item.href) ? 'active' : undefined}
                href={item.href}
                key={item.href}
              >
                {item.label}
              </Link>
            ))}
        </nav>
        <div className="user-card">
          <strong>{user.name.split(' ')[0]}</strong>
          <span>{user.role}</span>
          <button className="secondary" onClick={logout}>
            Sign out
          </button>
        </div>
      </aside>
      <main className="main">{children}</main>
    </div>
  )
}
