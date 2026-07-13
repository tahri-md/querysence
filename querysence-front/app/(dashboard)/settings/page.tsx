"use client"

import React from "react"

import { useState } from "react"
import { Loader2, Settings, User, AlertTriangle } from "lucide-react"
import { toast } from "sonner"
import { useRouter } from "next/navigation"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Separator } from "@/components/ui/separator"
import { Badge } from "@/components/ui/badge"
import { PageHeader } from "@/components/page-header"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { useAuth } from "@/lib/auth-context"
import { authApi } from "@/lib/api"
import { ThemeToggle } from "@/components/theme-toggle"

export default function SettingsPage() {
  const { user, checkAuth, logout } = useAuth()
  const router = useRouter()
  const [isUpdating, setIsUpdating] = useState(false)
  const [fullName, setFullName] = useState("")
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [isDeleting, setIsDeleting] = useState(false)
  
  // Password state
  const [showPasswordForm, setShowPasswordForm] = useState(true)
  const [oldPassword, setOldPassword] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [confirmPassword, setConfirmPassword] = useState("")
  const [isChangingPassword, setIsChangingPassword] = useState(false)

  // Email state
  const [showEmailForm, setShowEmailForm] = useState(true)
  const [newEmail, setNewEmail] = useState("")
  const [isUpdatingEmail, setIsUpdatingEmail] = useState(false)

  React.useEffect(() => {
    setFullName(user?.fullName ?? "")
  }, [user?.fullName])

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!fullName.trim()) {
      toast.error("Full name is required")
      return
    }

    setIsUpdating(true)

    try {
      await authApi.updateProfile(fullName.trim())
      await checkAuth()
      toast.success("Profile updated successfully")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to update profile")
    } finally {
      setIsUpdating(false)
    }
  }

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!oldPassword || !newPassword || !confirmPassword) {
      toast.error("All password fields are required")
      return
    }

    if (newPassword !== confirmPassword) {
      toast.error("New passwords do not match")
      return
    }

    if (newPassword.length < 6) {
      toast.error("Password must be at least 6 characters")
      return
    }

    setIsChangingPassword(true)

    try {
      await authApi.changePassword(oldPassword, newPassword)
      toast.success("Password changed successfully")
      setOldPassword("")
      setNewPassword("")
      setConfirmPassword("")
      setShowPasswordForm(false)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to change password")
    } finally {
      setIsChangingPassword(false)
    }
  }

  const handleUpdateEmail = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!newEmail || !newEmail.includes("@")) {
      toast.error("Valid email is required")
      return
    }

    if (newEmail === user?.email) {
      toast.error("New email must be different from current email")
      return
    }

    setIsUpdatingEmail(true)

    try {
      const response = await authApi.updateEmail(newEmail)
      await checkAuth()
      toast.success("Email updated successfully")
      setNewEmail("")
      setShowEmailForm(false)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to update email")
    } finally {
      setIsUpdatingEmail(false)
    }
  }

  const handleDeleteAccount = async () => {
    setIsDeleting(true)

    try {
      await authApi.deleteAccount()
      toast.success("Account deleted successfully")
      setShowDeleteDialog(false)
      await logout()
      router.push("/login")
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to delete account")
      setIsDeleting(false)
    }
  }

  return (
    <div className="space-y-6 font-mono">
      <PageHeader title="Settings" description="Manage your account settings and preferences" />

      <div className="grid gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <User className="h-5 w-5" />
              Profile
            </CardTitle>
            <CardDescription>
              Your personal information
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleUpdateProfile} className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="fullName">Full Name</Label>
                  <Input
                    id="fullName"
                    value={fullName}
                    onChange={(e) => setFullName(e.target.value)}
                    placeholder="Your name"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <Input
                    id="email"
                    type="email"
                    defaultValue={user?.email}
                    disabled
                    className="bg-muted"
                  />
                </div>
              </div>
              <div className="flex items-center gap-4">
                <div className="space-y-1">
                  <Label>Role</Label>
                  <div>
                    <Badge variant="secondary">{user?.role ?? "VIEWER"}</Badge>
                  </div>
                </div>
              </div>
              <Button type="submit" disabled={isUpdating}>
                {isUpdating && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                Save Changes
              </Button>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Settings className="h-5 w-5" />
              Appearance
            </CardTitle>
            <CardDescription>
              Customize the look and feel
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <p className="font-medium">Theme</p>
                <p className="text-sm text-muted-foreground">
                  Switch between light and dark mode
                </p>
              </div>
              <ThemeToggle />
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Settings className="h-5 w-5" />
              Security
            </CardTitle>
            <CardDescription>
              Manage your password and email
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-6">
            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="space-y-1">
                  <p className="font-medium">Change Password</p>
                  <p className="text-sm text-muted-foreground">
                    Update your password regularly for security
                  </p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowPasswordForm(!showPasswordForm)}
                >
                  {showPasswordForm ? "Cancel" : "Change"}
                </Button>
              </div>

              {showPasswordForm && (
                <form onSubmit={handleChangePassword} className="space-y-3 bg-muted p-4 rounded-lg">
                  <div className="space-y-2">
                    <Label htmlFor="oldPassword">Current Password</Label>
                    <Input
                      id="oldPassword"
                      type="password"
                      value={oldPassword}
                      onChange={(e) => setOldPassword(e.target.value)}
                      placeholder="Your current password"
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="newPassword">New Password</Label>
                    <Input
                      id="newPassword"
                      type="password"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      placeholder="Your new password"
                      required
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="confirmPassword">Confirm New Password</Label>
                    <Input
                      id="confirmPassword"
                      type="password"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Confirm your new password"
                      required
                    />
                  </div>
                  <Button type="submit" disabled={isChangingPassword} size="sm">
                    {isChangingPassword && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                    {isChangingPassword ? "Changing..." : "Change Password"}
                  </Button>
                </form>
              )}
            </div>

            <Separator />

            <div>
              <div className="flex items-center justify-between mb-4">
                <div className="space-y-1">
                  <p className="font-medium">Update Email</p>
                  <p className="text-sm text-muted-foreground">
                    Change your email address
                  </p>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowEmailForm(!showEmailForm)}
                >
                  {showEmailForm ? "Cancel" : "Update"}
                </Button>
              </div>

              {showEmailForm && (
                <form onSubmit={handleUpdateEmail} className="space-y-3 bg-muted p-4 rounded-lg">
                  <div className="space-y-2">
                    <Label htmlFor="currentEmail">Current Email</Label>
                    <Input
                      id="currentEmail"
                      type="email"
                      value={user?.email}
                      disabled
                      className="bg-muted-foreground/10"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="newEmail">New Email</Label>
                    <Input
                      id="newEmail"
                      type="email"
                      value={newEmail}
                      onChange={(e) => setNewEmail(e.target.value)}
                      placeholder="your.new.email@example.com"
                      required
                    />
                  </div>
                  <Button type="submit" disabled={isUpdatingEmail} size="sm">
                    {isUpdatingEmail && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                    {isUpdatingEmail ? "Updating..." : "Update Email"}
                  </Button>
                </form>
              )}
            </div>
          </CardContent>
        </Card>


        <Card className="border-destructive/50">
          <CardHeader>
            <CardTitle className="text-destructive flex items-center gap-2">
              <AlertTriangle className="h-5 w-5" />
              Danger Zone
            </CardTitle>
            <CardDescription>
              Irreversible actions for your account
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <Separator />
            <div className="flex items-center justify-between">
              <div className="space-y-1">
                <p className="font-medium">Delete Account</p>
                <p className="text-sm text-muted-foreground">
                  Permanently delete your account and all data
                </p>
              </div>
              <Button
                variant="destructive"
                onClick={() => setShowDeleteDialog(true)}
              >
                Delete Account
              </Button>
            </div>
          </CardContent>
        </Card>

        <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>Delete Account?</AlertDialogTitle>
              <AlertDialogDescription>
                This action cannot be undone. All your data, projects, and schemas will be permanently deleted.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <div className="AlertDialogFooter">
              <AlertDialogCancel>Cancel</AlertDialogCancel>
              <AlertDialogAction
                onClick={handleDeleteAccount}
                disabled={isDeleting}
                className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              >
                {isDeleting && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                {isDeleting ? "Deleting..." : "Delete Account"}
              </AlertDialogAction>
            </div>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </div>
  )
}
