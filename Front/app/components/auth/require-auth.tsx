import { useEffect } from "react"
import { useNavigate } from "react-router"
import { useAuth } from "~/components/auth/auth-provider"

export function RequireAuth({ children }: { children: React.ReactNode }) {
  const navigate = useNavigate()
  const { isAuthenticated } = useAuth()

  useEffect(() => {
    if (!isAuthenticated) {
      navigate("/login", { replace: true })
    }
  }, [isAuthenticated, navigate])

  if (!isAuthenticated) {
    return null
  }

  return <>{children}</>
}
