"use client"

import { WifiOff, RefreshCw } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"

export default function OfflinePage() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-md text-center">
        <CardHeader className="space-y-4">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-muted">
            <WifiOff className="h-8 w-8 text-muted-foreground" />
          </div>
          <CardTitle className="text-2xl">You're Offline</CardTitle>
          <CardDescription className="text-base">
            It looks like you've lost your internet connection. Some features may not be available.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="text-sm text-muted-foreground">
            QuerySense works best with an active internet connection to analyze queries and communicate with your backend.
          </p>
          <Button 
            onClick={() => window.location.reload()} 
            className="w-full"
          >
            <RefreshCw className="mr-2 h-4 w-4" />
            Try Again
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
