"use client"

import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from "react"
import { authApi } from "./api"

interface User {
  id: number
  email: string
  fullName: string
  role: string
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
      localStorage.removeItem("refreshToken")
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    checkAuth()
  }, [checkAuth])

  const login = async (username: string, password: string) => {
    const response = await authApi.login(username, password)
    localStorage.setItem("accessToken", response.accessToken)
    localStorage.setItem("refreshToken", response.refreshToken)
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
      localStorage.removeItem("refreshToken")
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
