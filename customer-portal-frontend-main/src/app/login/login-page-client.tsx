'use client'

import Login from '@/components/login/Login'
import { useShell } from '../shell-context'

export default function LoginPageClient() {
  const { onLogin, setShowNavbar, setFooter, GlobalLeadId, SetGloablLeadId } = useShell()
  return (
    <Login
      onLogin={onLogin}
      setShowNavbar={setShowNavbar}
      setFooter={setFooter}
      GlobalLeadId={GlobalLeadId}
      SetGloablLeadId={SetGloablLeadId}
    />
  )
}
