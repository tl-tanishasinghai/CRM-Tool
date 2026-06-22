import type { Metadata } from 'next'
import './globals.css'
import './app.css'
import ClientApp from './client-app'

export const metadata: Metadata = {
  title: 'Customer Portal',
  description: "Trillionloans Customer Portal – Your one-stop solution for all loan-related queries.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body>
        <ClientApp>{children}</ClientApp>
      </body>
    </html>
  )
}