"use client"

import React, { useState } from "react"
import Link from "next/link"
import { Terminal, Github, Loader2 } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import {
  Card,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from "@/components/ui/card"
import { useAuth } from "@/lib/auth-context"
import { ThemeToggle } from "@/components/theme-toggle"
import { Separator } from "@/components/ui/separator"

export default function LoginPage() {
  const [isLoading, setIsLoading] = useState(false)
  const { login } = useAuth()

  const handleLogin = async () => {
    setIsLoading(true)

    try {
      await login()
    } catch (error) {
      console.error(error)
      toast.error("Login failed")
      setIsLoading(false)
    }
  }

  return (
    <div className="flex min-h-screen flex-col items-center font-mono justify-center p-4">

      <div className="absolute right-4 top-4">
        <ThemeToggle />
      </div>

      <Card className="w-full max-w-md">

        <CardHeader className="space-y-1">

          <div className="flex items-center justify-center gap-2 mb-4">
            <Terminal className="h-8 w-8" />

            <span className="text-2xl font-bold">
              QuerySense
            </span>
          </div>

          <CardTitle className="text-2xl text-center">
            Welcome back
          </CardTitle>

          <CardDescription className="text-center">
            Sign in to access your account
          </CardDescription>

        </CardHeader>

        <CardFooter className="flex flex-col gap-4 py-4">

          <Button
            size="lg"
            type="button"
            className="w-full"
            onClick={handleLogin}
            disabled={isLoading}
          >
            {isLoading && (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            )}

            {isLoading
              ? "Redirecting..."
              : "Sign in"
            }
          </Button>

          <div className="relative w-full">

            <div className="absolute inset-0 flex items-center">
              <Separator className="w-full" />
            </div>

            <div className="relative flex justify-center text-xs uppercase">
              <span className="bg-background px-2 text-muted-foreground">
                Secure authentication
              </span>
            </div>

          </div>

          <p className="text-center text-sm text-muted-foreground">
            Don't have an account?{" "}

            <Link
              href="/register"
              className="underline underline-offset-4 hover:text-primary"
            >
              Sign up
            </Link>
          </p>

        </CardFooter>

      </Card>
    </div>
  )
}