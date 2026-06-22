'use client'
import { useRouter as useNextRouter, usePathname } from 'next/navigation'

export const useNavigate = () => {
  const router = useNextRouter()
  return (path: string) => router.push(path)
}

export const useLocation = () => {
  const pathname = usePathname()
  return { pathname }
}
