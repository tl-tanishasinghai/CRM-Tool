'use client'

import EmbeddedModalPage from "@/components/embeddedModal/EmbeddedModalPage"
import { useShell } from '../shell-context'

export default function EmbeddedModal() {
  const { onLogout, setShowNavbar, setFooter } = useShell()
  return (
    <EmbeddedModalPage
      onLogout={onLogout}
      setShowNavbar={setShowNavbar}
      setFooter={setFooter}
    />
  )
}
