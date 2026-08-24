"use client"

import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
} from "react"
import keycloak from "@/lib/keycloak"

type AuthContextType = {
  authenticated: boolean
  loading: boolean
  user: { username?: string; email?: string; name?: string } | null
  login: () => Promise<void>
  register: () => Promise<void>
  logout: () => Promise<void>
  getToken: () => Promise<string | undefined>
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({
  children,
}: {
  children: React.ReactNode
}) {
  const [authenticated, setAuthenticated] = useState(false)
  const [loading, setLoading] = useState(true)
  const [user, setUser] = useState<{ username?: string; email?: string; name?: string } | null>(null)

  // Initialize Keycloak and set up token refresh
  useEffect(() => {
    const initKeycloak = async () => {
      try {
        const isAuthenticated = await keycloak.init({
          onLoad: "check-sso",
          pkceMethod: "S256",
          checkLoginIframe: false,
        })

        console.log("Keycloak authenticated:", isAuthenticated)

        if (isAuthenticated) {
          // Store token in localStorage immediately after auth
          if (keycloak.token) {
            localStorage.setItem("accessToken", keycloak.token)
          }

          // Extract user info from token
          if (keycloak.tokenParsed) {
            setUser({
              username: keycloak.tokenParsed.preferred_username,
              email: keycloak.tokenParsed.email,
              name: keycloak.tokenParsed.name,
            })
          }
        }

        setAuthenticated(isAuthenticated)
      } catch (error) {
        console.error("Keycloak initialization failed:", error)
        setAuthenticated(false)
      } finally {
        setLoading(false)
      }
    }

    initKeycloak()
  }, [])

  // Set up token refresh interval (refresh every 4 minutes if authenticated)
  useEffect(() => {
    if (!authenticated) return

    const tokenRefreshInterval = setInterval(async () => {
      try {
        const refreshed = await keycloak.updateToken(30) // Try to refresh 30 seconds before expiry

        if (refreshed) {
          // Token was refreshed, update localStorage
          if (keycloak.token) {
            localStorage.setItem("accessToken", keycloak.token)
            console.log("Token refreshed successfully")
          }
        } else if (!keycloak.authenticated) {
          // Token couldn't be refreshed and user is no longer authenticated
          console.warn("Token could not be refreshed, user logged out")
          setAuthenticated(false)
          localStorage.removeItem("accessToken")
        }
      } catch (error) {
        console.error("Token refresh failed:", error)
        setAuthenticated(false)
        localStorage.removeItem("accessToken")
      }
    }, 4 * 60 * 1000) // Refresh every 4 minutes

    return () => clearInterval(tokenRefreshInterval)
  }, [authenticated])

  const login = useCallback(async () => {
    await keycloak.login({
      redirectUri: `${window.location.origin}/dashboard`,
    })
  }, [])

  const register = useCallback(async () => {
    await keycloak.register({
      redirectUri: `${window.location.origin}/dashboard`,
    })
  }, [])

  const logout = useCallback(async () => {
    localStorage.removeItem("accessToken")
    setUser(null)
    await keycloak.logout({
      redirectUri: window.location.origin,
    })
  }, [])

  const getToken = useCallback(async (): Promise<string | undefined> => {
    if (!keycloak.authenticated) {
      return undefined
    }

    try {
      // Try to refresh token if it's about to expire
      await keycloak.updateToken(30)

      if (keycloak.token) {
        // Update localStorage with fresh token
        localStorage.setItem("accessToken", keycloak.token)
        return keycloak.token
      }
    } catch (error) {
      console.error("Failed to refresh token:", error)
      localStorage.removeItem("accessToken")
      setAuthenticated(false)
    }

    return undefined
  }, [])

  return (
    <AuthContext.Provider
      value={{
        authenticated,
        loading,
        user,
        login,
        register,
        logout,
        getToken,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)

  if (!context) {
    throw new Error(
      "useAuth must be used inside AuthProvider"
    )
  }

  return context
}