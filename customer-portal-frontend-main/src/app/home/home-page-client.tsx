'use client'

import Home from '@/components/home/Home'
import { useShell } from '../shell-context'

export default function HomePageClient() {
  const { onLogout, setShowNavbar, setFooter, GlobalLeadId, SetGloablLeadId } = useShell()
  return (
    <Home
      onLogout={onLogout}
      setShowNavbar={setShowNavbar}
      setFooter={setFooter}
      GlobalLeadId={GlobalLeadId}
      SetGloablLeadId={SetGloablLeadId}
    />
  )
}
