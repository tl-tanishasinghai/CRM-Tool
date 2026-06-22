'use client'

import React, { createContext, useContext } from 'react'

type ShellContextValue = {
  onLogout: () => void | Promise<void>
  onLogin: () => void
  setShowNavbar: (value: boolean) => void
  setFooter: (value: boolean) => void
  GlobalLeadId: string
  SetGloablLeadId: (value: string) => void
  isLoggedIn: boolean
}

const ShellContext = createContext<ShellContextValue | undefined>(undefined)

export const ShellProvider = ShellContext.Provider

export const useShell = () => {
  const context = useContext(ShellContext)
  if (!context) {
    throw new Error('useShell must be used within ShellProvider')
  }
  return context
}
