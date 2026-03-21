"use client"

import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from "react"
import { authApi } from "./api"

interface User {
  id?: number
  email: string
  fullName: string
  role?: string
}

interface AuthContextType {
  user: User | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, fullName: string) => Promise<void>
  logout: () => Promise<void>
  checkAuth: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const checkAuth = useCallback(async () => {
    const token = localStorage.getItem("accessToken")
    if (!token) {
      setIsLoading(false)
      return
    }

    try {
      const userData = await authApi.me()
      setUser(userData)
    } catch {
      localStorage.removeItem("accessToken")
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    checkAuth()
  }, [checkAuth])

  const login = async (email: string, password: string) => {
    const response = await authApi.login(email, password)
    localStorage.setItem("accessToken", response.accessToken)
    setUser(response.user)
    await checkAuth()
  }

  const register = async (email: string, password: string, fullName: string) => {
    await authApi.register(email, password, fullName)
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } catch {
      // Ignore logout errors
    } finally {
      localStorage.removeItem("accessToken")
      setUser(null)
    }
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register, logout, checkAuth }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider")
  }
  return context
}
