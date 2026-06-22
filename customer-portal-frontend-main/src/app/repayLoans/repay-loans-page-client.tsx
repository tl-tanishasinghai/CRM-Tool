'use client'

import React, { Suspense } from 'react'
import RepayLoans from '@/components/repayLoans/RepayLoans'
import Loader from '@/components/loader/Loader'
import { useShell } from '../shell-context'

export default function RepayLoansPageClient() {
  const { onLogout, setShowNavbar, setFooter, GlobalLeadId, SetGloablLeadId } = useShell()
  return (
    <Suspense
      fallback={
        <div className="loader-container">
          <Loader />
        </div>
      }
    >
      <RepayLoans
        onLogout={onLogout}
        setShowNavbar={setShowNavbar}
        setFooter={setFooter}
        GlobalLeadId={GlobalLeadId}
        SetGloablLeadId={SetGloablLeadId}
      />
    </Suspense>
  )
}
