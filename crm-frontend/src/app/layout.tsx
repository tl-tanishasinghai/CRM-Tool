import type { Metadata } from 'next'
import './globals.css'
import { SessionProvider } from '@/components/session-provider'
import { AppShell } from '@/components/app-shell'

export const metadata: Metadata = {
  title: 'Loan CRM',
  description: 'Internal CRM for loan servicing operations'
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <SessionProvider>
          <AppShell>{children}</AppShell>
        </SessionProvider>
      </body>
    </html>
  )
}
