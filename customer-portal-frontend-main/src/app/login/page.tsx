import LoginPageClient from './login-page-client'

/** Full document load: avoid Flight-only responses behind some CDNs / proxies. */
export const dynamic = 'force-dynamic'

export default function LoginPage() {
  return <LoginPageClient />
}
