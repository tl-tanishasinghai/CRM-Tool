'use client'

import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { fetchMe, logout as logoutApi } from '@/lib/api'
import type { StaffUser } from '@/lib/types'

type SessionContextValue = {
  user: StaffUser | null
  loading: boolean
  setUser: (user: StaffUser | null) => void
  logout: () => Promise<void>
}

const SessionContext = createContext<SessionContextValue | undefined>(undefined)

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<StaffUser | null>(null)
  const [loading, setLoading] = useState(true)
  const router = useRouter()
  const pathname = usePathname()

  useEffect(() => {
    const token = localStorage.getItem('crmToken')
    if (!token) {
      setLoading(false)
      if (pathname !== '/login') {
        router.replace('/login')
      }
      return
    }

    fetchMe()
      .then(setUser)
      .catch(() => {
        localStorage.removeItem('crmToken')
        router.replace('/login')
      })
      .finally(() => setLoading(false))
  }, [pathname, router])

  const value = useMemo<SessionContextValue>(
    () => ({
      user,
      loading,
      setUser,
      logout: async () => {
        await logoutApi().catch(() => undefined)
        localStorage.removeItem('crmToken')
        setUser(null)
        router.replace('/login')
      }
    }),
    [loading, router, user]
  )

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}

export function useSession() {
  const context = useContext(SessionContext)
  if (!context) {
    throw new Error('useSession must be used inside SessionProvider')
  }
  return context
}
