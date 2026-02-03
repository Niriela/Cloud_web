import type { AuthResponse } from "~/lib/api"

const TOKEN_KEY = "auth_token"
const USER_KEY = "auth_user"

export function setAuthSession(auth: AuthResponse) {
  localStorage.setItem(TOKEN_KEY, auth.token)
  localStorage.setItem(USER_KEY, JSON.stringify(auth))
}

export function setAuthToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearAuthSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

export function getAuthToken() {
  const user = getAuthUser()
  if (!user) {
    return null
  }
  return localStorage.getItem(TOKEN_KEY)
}

export function getAuthUser(): AuthResponse | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as AuthResponse
    if (parsed?.expiresAt) {
      const expiresAt = Date.parse(parsed.expiresAt)
      if (!Number.isNaN(expiresAt) && Date.now() > expiresAt) {
        clearAuthSession()
        return null
      }
    }
    return parsed
  } catch {
    return null
  }
}

export function isAuthenticated() {
  return Boolean(getAuthToken())
}
