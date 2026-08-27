"use client"

import { useEffect } from "react"
import { useAuth } from "@/lib/auth-context"
import { setGetTokenFn } from "@/lib/api"

/**
 * Initializes the token getter function for the API client.
 * This allows the API client to always get fresh tokens from the auth context.
 * 
 * This component must be placed inside the AuthProvider and before any API calls.
 */
export function TokenInitializer() {
  const { getToken } = useAuth()

  useEffect(() => {
    // Inject the getToken function from auth context into the API client
    setGetTokenFn(getToken)
  }, [getToken])

  return null
}