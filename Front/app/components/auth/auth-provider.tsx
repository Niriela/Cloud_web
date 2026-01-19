import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
} from "react"
import type { AuthResponse } from "~/lib/api"
import {
  clearAuthSession,
  getAuthToken,
  getAuthUser,
  setAuthSession,
} from "~/lib/auth"

type AuthContextValue = {
  user: AuthResponse | null
  token: string | null
  isAuthenticated: boolean
  setSession: (auth: AuthResponse) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AuthResponse | null>(() => getAuthUser())
  const [token, setToken] = useState<string | null>(() => getAuthToken())

  const setSession = useCallback((auth: AuthResponse) => {
    setAuthSession(auth)
    setUser(auth)
    setToken(auth.token)
  }, [])

  const logout = useCallback(() => {
    clearAuthSession()
    setUser(null)
    setToken(null)
  }, [])

  const value = useMemo(
    () => ({
      user,
      token,
      isAuthenticated: Boolean(token),
      setSession,
      logout,
    }),
    [user, token, setSession, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider")
  }
  return context
}
