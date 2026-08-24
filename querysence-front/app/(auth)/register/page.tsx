"use client"

import React, { useState } from "react"
import Link from "next/link"
import { Terminal, Loader2 } from "lucide-react"
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

export default function RegisterPage() {
  const [isLoading, setIsLoading] = useState(false)
  const { register } = useAuth()

  const handleRegister = async () => {
    setIsLoading(true)

    try {
      await register()
    } catch (error) {
      console.error(error)
      toast.error("Registration failed")
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
            Create an account
          </CardTitle>

          <CardDescription className="text-center">
            Create your QuerySense account securely with Keycloak
          </CardDescription>

        </CardHeader>

        <CardFooter className="flex flex-col gap-4 py-4">

          <Button
            size="lg"
            type="button"
            className="w-full"
            onClick={handleRegister}
            disabled={isLoading}
          >
            {isLoading && (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            )}

            {isLoading
              ? "Redirecting..."
              : "Create account"
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
            Already have an account?{" "}

            <Link
              href="/login"
              className="underline underline-offset-4 hover:text-primary"
            >
              Sign in
            </Link>
          </p>

        </CardFooter>

      </Card>
    </div>
  )
}

