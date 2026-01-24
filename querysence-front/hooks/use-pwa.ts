"use client"

import { useState, useEffect, useCallback } from "react"

interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>
  userChoice: Promise<{ outcome: "accepted" | "dismissed" }>
}

interface PWAState {
  isInstallable: boolean
  isInstalled: boolean
  isOnline: boolean
  isUpdateAvailable: boolean
}

export function usePWA() {
  const [state, setState] = useState<PWAState>({
    isInstallable: false,
    isInstalled: false,
    isOnline: typeof navigator !== "undefined" ? navigator.onLine : true,
    isUpdateAvailable: false,
  })
  const [deferredPrompt, setDeferredPrompt] = useState<BeforeInstallPromptEvent | null>(null)

  useEffect(() => {
    // Check if already installed
    const isStandalone = window.matchMedia("(display-mode: standalone)").matches
    const isIOSStandalone = (navigator as Navigator & { standalone?: boolean }).standalone === true

    setState((prev) => ({
      ...prev,
      isInstalled: isStandalone || isIOSStandalone,
    }))

    // Listen for install prompt
    const handleBeforeInstall = (e: Event) => {
      e.preventDefault()
      setDeferredPrompt(e as BeforeInstallPromptEvent)
      setState((prev) => ({ ...prev, isInstallable: true }))
    }

    // Listen for app installed
    const handleAppInstalled = () => {
      setDeferredPrompt(null)
      setState((prev) => ({ ...prev, isInstallable: false, isInstalled: true }))
    }

    // Listen for online/offline
    const handleOnline = () => setState((prev) => ({ ...prev, isOnline: true }))
    const handleOffline = () => setState((prev) => ({ ...prev, isOnline: false }))

    window.addEventListener("beforeinstallprompt", handleBeforeInstall)
    window.addEventListener("appinstalled", handleAppInstalled)
    window.addEventListener("online", handleOnline)
    window.addEventListener("offline", handleOffline)

    // Register service worker
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker
        .register("/sw.js")
        .then((registration) => {
          // Check for updates
          registration.addEventListener("updatefound", () => {
            const newWorker = registration.installing
            if (newWorker) {
              newWorker.addEventListener("statechange", () => {
                if (newWorker.state === "installed" && navigator.serviceWorker.controller) {
                  setState((prev) => ({ ...prev, isUpdateAvailable: true }))
                }
              })
            }
          })
        })
        .catch((error) => {
          console.error("Service worker registration failed:", error)
        })
    }

    return () => {
      window.removeEventListener("beforeinstallprompt", handleBeforeInstall)
      window.removeEventListener("appinstalled", handleAppInstalled)
      window.removeEventListener("online", handleOnline)
      window.removeEventListener("offline", handleOffline)
    }
  }, [])

  const install = useCallback(async () => {
    if (!deferredPrompt) return false

    await deferredPrompt.prompt()
    const { outcome } = await deferredPrompt.userChoice

    if (outcome === "accepted") {
      setDeferredPrompt(null)
      setState((prev) => ({ ...prev, isInstallable: false }))
      return true
    }
    return false
  }, [deferredPrompt])

  const update = useCallback(() => {
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.ready.then((registration) => {
        registration.waiting?.postMessage({ type: "SKIP_WAITING" })
        window.location.reload()
      })
    }
  }, [])

  return {
    ...state,
    install,
    update,
  }
}
