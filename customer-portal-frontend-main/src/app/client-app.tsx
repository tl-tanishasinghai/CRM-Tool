'use client'
import React, { useCallback, useEffect, useRef, useState } from 'react'
import { useRouter } from 'next/navigation'
import Navbar from '@/components/navbar/Navbar'
import Footer from '@/components/footer/Footer'
import { ToastProvider, useToast } from '@/components/toast/ToastContext'
import { ShellProvider } from './shell-context'
import axios from 'axios'
import { CoralogixRum } from '@coralogix/browser'
type CoralogixDomain = 'EU1' | 'EU2' | 'US1' | 'US2' | 'AP1' | 'AP2' | 'AP3' | 'STAGING'
type SessionTimeoutProps = {
  onLogout: () => Promise<void> | void
}

const SessionTimeout = ({ onLogout }: SessionTimeoutProps) => {
  const router = useRouter()
  const { addToast } = useToast()

  useEffect(() => {
    let timer: NodeJS.Timeout
    const handleMouseMove = () => {
      clearTimeout(timer)
      timer = setTimeout(async () => {
        if (localStorage.getItem('authToken')) {
          addToast('Session Expired', 'error', null, true)
          await Promise.resolve(onLogout())
          router.push('/login')
        }
      }, 900000)
    }
    document.addEventListener('mousemove', handleMouseMove)
    return () => {
      clearTimeout(timer)
      document.removeEventListener('mousemove', handleMouseMove)
    }
  }, [addToast, onLogout, router])

  return null
}

export default function ClientApp({
  children
}: {
  children: React.ReactNode
}) {
  const router = useRouter()
  const coralogixInitialized = useRef(false)
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [showNavbar, setShowNavbar] = useState(true)
  const [showFooter, setFooter] = useState(true)
  const [leadId, setLeadId] = useState('')
  const handleLogin = () => {
    setIsLoggedIn(true)
  }
  const baseUrl = process.env.NEXT_PUBLIC_CUSTOMER_PORTAL_ENDPOINT
  const handleLogout = useCallback(async () => {
    const mobileNumber = JSON.parse(localStorage.getItem('phoneNumber') || '{}')
    if (JSON.parse(localStorage.getItem('phoneNumber') || 'null')) {
      try {
        await axios.post(
          `${baseUrl}/customer/auth/logout`,
          { mobileNumber },
          { withCredentials: true }
        )
        localStorage.clear()
        setIsLoggedIn(false)
        router.push('/login')
        return
      } catch (error: any) {
        if (error?.response?.status === 403) {
          localStorage.clear()
          setIsLoggedIn(false)
          router.push('/login')
        }
        console.log('Logout error occurred')
      }
    }
    localStorage.clear()
    setIsLoggedIn(false)
    setLeadId('')
    router.push('/login')
  }, [baseUrl, router])

  useEffect(() => {
    const authToken = localStorage.getItem('authToken')
    if (authToken) {
      setIsLoggedIn(true)
    }
  }, [])

  useEffect(() => {
    if (coralogixInitialized.current) {
      return
    }
    const publicKey = process.env.NEXT_PUBLIC_CORALOGIX_KEY
    const coralogixDomain = process.env
      .NEXT_PUBLIC_CORALOGIX_DOMAIN as CoralogixDomain | undefined
    if (!publicKey || !coralogixDomain) {
      return
    }
    CoralogixRum.init({
      application: 'customer-portal',
      environment: 'development',
      public_key: publicKey,
      coralogixDomain,
      version: '1.0.0',
      sessionRecordingConfig: {
        enable: true, // Required
        autoStartSessionRecording: true, // Start automatically
        recordConsoleEvents: true, // Optional: capture console logs
        sessionRecordingSampleRate: 100, // % of sessions to record
        },
    })
    coralogixInitialized.current = true
  }, [])
  return (
    <ToastProvider>
      <ShellProvider
        value={{
          onLogout: handleLogout,
          onLogin: handleLogin,
          setShowNavbar,
          setFooter,
          GlobalLeadId: leadId,
          SetGloablLeadId: setLeadId,
          isLoggedIn
        }}
      >
        <SessionTimeout onLogout={handleLogout} />
        <div className="app-container">
          {showNavbar && <Navbar onLogout={handleLogout} />}
          <main className="app-content">{children}</main>
          {showFooter && <Footer />}
        </div>
      </ShellProvider>
    </ToastProvider>
  )
}