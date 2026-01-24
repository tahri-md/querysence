"use client"

import { useState, useEffect } from "react"
import { Download, X, RefreshCw, Wifi, WifiOff } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { usePWA } from "@/hooks/use-pwa"

export function PWAInstallPrompt() {
  const { isInstallable, isInstalled, install } = usePWA()
  const [dismissed, setDismissed] = useState(false)

  useEffect(() => {
    const wasDismissed = localStorage.getItem("pwa-install-dismissed")
    if (wasDismissed) {
      const dismissedTime = parseInt(wasDismissed)
      // Show again after 7 days
      if (Date.now() - dismissedTime < 7 * 24 * 60 * 60 * 1000) {
        setDismissed(true)
      }
    }
  }, [])

  const handleDismiss = () => {
    setDismissed(true)
    localStorage.setItem("pwa-install-dismissed", Date.now().toString())
  }

  const handleInstall = async () => {
    const installed = await install()
    if (installed) {
      setDismissed(true)
    }
  }

  if (isInstalled || !isInstallable || dismissed) {
    return null
  }

  return (
    <Card className="fixed bottom-4 left-4 right-4 z-50 mx-auto max-w-md border-border bg-card shadow-lg md:left-auto md:right-4">
      <CardContent className="flex items-center gap-4 p-4">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary text-primary-foreground">
          <Download className="h-5 w-5" />
        </div>
        <div className="flex-1 space-y-1">
          <p className="text-sm font-medium">Install QuerySense</p>
          <p className="text-xs text-muted-foreground">
            Add to home screen for quick access
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button size="sm" onClick={handleInstall}>
            Install
          </Button>
          <Button size="icon" variant="ghost" onClick={handleDismiss}>
            <X className="h-4 w-4" />
          </Button>
        </div>
      </CardContent>
    </Card>
  )
}

export function PWAUpdatePrompt() {
  const { isUpdateAvailable, update } = usePWA()

  if (!isUpdateAvailable) {
    return null
  }

  return (
    <Card className="fixed bottom-4 left-4 right-4 z-50 mx-auto max-w-md border-border bg-card shadow-lg md:left-auto md:right-4">
      <CardContent className="flex items-center gap-4 p-4">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-muted">
          <RefreshCw className="h-5 w-5" />
        </div>
        <div className="flex-1 space-y-1">
          <p className="text-sm font-medium">Update Available</p>
          <p className="text-xs text-muted-foreground">
            A new version is ready
          </p>
        </div>
        <Button size="sm" onClick={update}>
          Update
        </Button>
      </CardContent>
    </Card>
  )
}

export function OfflineIndicator() {
  const { isOnline } = usePWA()
  const [show, setShow] = useState(false)

  useEffect(() => {
    if (!isOnline) {
      setShow(true)
    } else {
      // Hide after a brief delay when coming back online
      const timeout = setTimeout(() => setShow(false), 2000)
      return () => clearTimeout(timeout)
    }
  }, [isOnline])

  if (!show) {
    return null
  }

  return (
    <div
      className={`fixed left-0 right-0 top-0 z-50 flex items-center justify-center gap-2 px-4 py-2 text-sm font-medium transition-colors ${
        isOnline
          ? "bg-primary text-primary-foreground"
          : "bg-destructive text-destructive-foreground"
      }`}
    >
      {isOnline ? (
        <>
          <Wifi className="h-4 w-4" />
          Back online
        </>
      ) : (
        <>
          <WifiOff className="h-4 w-4" />
          You're offline
        </>
      )}
    </div>
  )
}
