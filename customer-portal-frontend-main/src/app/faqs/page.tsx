import Faqs from '@/components/faqs/Faqs'

/** Full document load: avoid Flight-only responses behind some CDNs / proxies. */
export const dynamic = 'force-dynamic'

export default function FaqsPage() {
  return <Faqs />
}
