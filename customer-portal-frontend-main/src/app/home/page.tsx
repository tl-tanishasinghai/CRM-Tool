import HomePageClient from './home-page-client'

/** Avoid Flight-only document responses on full page load (seen on /home behind some CDNs / proxies). */
export const dynamic = 'force-dynamic'

export default function HomePage() {
  return <HomePageClient />
}
