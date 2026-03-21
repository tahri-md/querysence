import React, { useState, useEffect } from 'react'
import { Loader2, Share2, Plus, Trash2, Users, Copy, Check } from 'lucide-react'
import { toast } from 'sonner'

import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'

interface ProjectMember {
  id: number
  userId: number
  email: string
  fullName: string
  role: 'OWNER' | 'EDITOR' | 'VIEWER'
  joinedAt: string
}

interface ProjectInvite {
  id: number
  inviteCode: string
  email: string
  role: 'OWNER' | 'EDITOR' | 'VIEWER'
  createdByEmail: string
  expiresAt: string
  createdAt: string
  isUsed: boolean
}

interface ProjectSharingDialogProps {
  projectId: number
  isOpen: boolean
  onClose: () => void
}

export function ProjectSharingDialog({
  projectId,
  isOpen,
  onClose,
}: ProjectSharingDialogProps) {
  const [members, setMembers] = useState<ProjectMember[]>([])
  const [invites, setInvites] = useState<ProjectInvite[]>([])
  const [loading, setLoading] = useState(false)
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState('VIEWER')
  const [copiedCode, setCopiedCode] = useState<string | null>(null)

  useEffect(() => {
    if (isOpen) {
      fetchMembers()
      fetchInvites()
    }
  }, [isOpen])

  const fetchMembers = async () => {
    try {
      const token = localStorage.getItem('accessToken')
      const response = await fetch(`http://localhost:8081/projects/${projectId}/members`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (response.ok) {
        setMembers(await response.json())
      }
    } catch (error) {
      toast.error('Failed to load members')
    }
  }

  const fetchInvites = async () => {
    try {
      const token = localStorage.getItem('accessToken')
      const response = await fetch(`http://localhost:8081/projects/${projectId}/invites`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (response.ok) {
        setInvites(await response.json())
      }
    } catch (error) {
      toast.error('Failed to load invites')
    }
  }

  const handleCreateInvite = async (e: React.FormEvent) => {
    e.preventDefault()

    if (!inviteEmail) {
      toast.error('Email is required')
      return
    }

    setLoading(true)

    try {
      const token = localStorage.getItem('accessToken')
      const response = await fetch(
        `http://localhost:8081/projects/${projectId}/members/invite`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            email: inviteEmail,
            role: inviteRole,
          }),
        }
      )

      if (response.ok) {
        toast.success('Invite created successfully')
        setInviteEmail('')
        setInviteRole('VIEWER')
        await fetchInvites()
      } else {
        const error = await response.json()
        toast.error(error.message || 'Failed to create invite')
      }
    } catch (error) {
      toast.error('Failed to create invite')
    } finally {
      setLoading(false)
    }
  }

  const handleCopyInviteCode = (code: string) => {
    navigator.clipboard.writeText(
      `Accept invite at your dashboard: ${window.location.origin}/dashboard?invite=${code}`
    )
    setCopiedCode(code)
    toast.success('Invite link copied')
    setTimeout(() => setCopiedCode(null), 2000)
  }

  const handleRemoveMember = async (memberId: number) => {
    if (!confirm('Are you sure you want to remove this member?')) return

    try {
      const token = localStorage.getItem('accessToken')
      const response = await fetch(
        `http://localhost:8081/projects/${projectId}/members/${memberId}`,
        {
          method: 'DELETE',
          headers: { Authorization: `Bearer ${token}` },
        }
      )

      if (response.ok) {
        toast.success('Member removed')
        await fetchMembers()
      } else {
        toast.error('Failed to remove member')
      }
    } catch (error) {
      toast.error('Failed to remove member')
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={onClose}>
      <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Share2 className="h-5 w-5" />
            Share Project
          </DialogTitle>
          <DialogDescription>
            Manage team members and send invitations
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-6">
          {/* Create Invite Section */}
          <div className="space-y-3">
            <h3 className="font-semibold">Send Invitation</h3>
            <form onSubmit={handleCreateInvite} className="space-y-3 bg-muted p-4 rounded-lg">
              <div className="grid gap-3 grid-cols-3">
                <div className="col-span-2 space-y-2">
                  <Label htmlFor="email">Email</Label>
                  <Input
                    id="email"
                    type="email"
                    value={inviteEmail}
                    onChange={(e) => setInviteEmail(e.target.value)}
                    placeholder="colleague@example.com"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="role">Role</Label>
                  <Select value={inviteRole} onValueChange={setInviteRole}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="VIEWER">Viewer</SelectItem>
                      <SelectItem value="EDITOR">Editor</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <Button type="submit" disabled={loading} size="sm" className="w-full">
                {loading && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                Send Invite
              </Button>
            </form>
          </div>

          <Separator />

          {/* Members Section */}
          <div className="space-y-3">
            <h3 className="font-semibold flex items-center gap-2">
              <Users className="h-4 w-4" />
              Members ({members.length})
            </h3>
            {members.length > 0 ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Name</TableHead>
                    <TableHead>Email</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead>Joined</TableHead>
                    <TableHead className="w-12"></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {members.map((member) => (
                    <TableRow key={member.id}>
                      <TableCell>{member.fullName}</TableCell>
                      <TableCell>{member.email}</TableCell>
                      <TableCell>
                        <Badge
                          variant={
                            member.role === 'OWNER'
                              ? 'default'
                              : member.role === 'EDITOR'
                                ? 'secondary'
                                : 'outline'
                          }
                        >
                          {member.role}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {new Date(member.joinedAt).toLocaleDateString()}
                      </TableCell>
                      <TableCell>
                        {member.role !== 'OWNER' && (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleRemoveMember(member.id)}
                          >
                            <Trash2 className="h-4 w-4 text-destructive" />
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            ) : (
              <p className="text-sm text-muted-foreground">No members yet</p>
            )}
          </div>

          <Separator />

          {/* Pending Invites Section */}
          <div className="space-y-3">
            <h3 className="font-semibold">Pending Invitations ({invites.filter(i => !i.isUsed).length})</h3>
            {invites.filter(i => !i.isUsed).length > 0 ? (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Email</TableHead>
                    <TableHead>Role</TableHead>
                    <TableHead>Expires</TableHead>
                    <TableHead className="w-40"></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {invites
                    .filter((i) => !i.isUsed)
                    .map((invite) => (
                      <TableRow key={invite.id}>
                        <TableCell className="text-sm">{invite.email}</TableCell>
                        <TableCell>
                          <Badge variant="outline">{invite.role}</Badge>
                        </TableCell>
                        <TableCell className="text-sm text-muted-foreground">
                          {new Date(invite.expiresAt).toLocaleDateString()}
                        </TableCell>
                        <TableCell>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleCopyInviteCode(invite.inviteCode)}
                          >
                            {copiedCode === invite.inviteCode ? (
                              <>
                                <Check className="h-4 w-4 mr-2" />
                                Copied
                              </>
                            ) : (
                              <>
                                <Copy className="h-4 w-4 mr-2" />
                                Copy Link
                              </>
                            )}
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                </TableBody>
              </Table>
            ) : (
              <p className="text-sm text-muted-foreground">No pending invitations</p>
            )}
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}
