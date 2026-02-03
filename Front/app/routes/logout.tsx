import { useEffect } from "react"
import { useNavigate } from "react-router"
import { useAuth } from "~/components/auth/auth-provider"

export default function LogoutPage() {
  const navigate = useNavigate()
  const { logout } = useAuth()

  useEffect(() => {
    logout()
    navigate("/login", { replace: true })
  }, [logout, navigate])

  return null
}
