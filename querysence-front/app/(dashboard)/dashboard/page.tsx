"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogDescription } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Activity, AlertTriangle, Database, Search, TrendingUp } from "lucide-react"
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts"
import { Suspense } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import { SQLEditor } from "@/components/sql-editor"
import { PageHeader } from "@/components/page-header"
import { analyticsApi, queryApi, type AnalyticsOverview, type HistoryEntry, historyApi } from "@/lib/api"
import { toast } from "sonner"

const chartConfig = {
  count: {
    label: "Queries",
    color: "var(--foreground)",
  },
} satisfies ChartConfig

const Loading = () => null

export default function DashboardPage() {
  // Invitation acceptance state
  const [inviteDialogOpen, setInviteDialogOpen] = useState(false)
  const [inviteCode, setInviteCode] = useState<string | null>(null)
  const [acceptingInvite, setAcceptingInvite] = useState(false)
  const [inviteAccepted, setInviteAccepted] = useState(false)
  const [inviteError, setInviteError] = useState<string | null>(null)
  const [overview, setOverview] = useState<AnalyticsOverview | null>(null)
  const [recentQueries, setRecentQueries] = useState<HistoryEntry[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [quickQuery, setQuickQuery] = useState("")
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const router = useRouter()

  // Pending invites for the user
  const [pendingInvites, setPendingInvites] = useState<any[]>([])

  useEffect(() => {
    // Invitation code from URL
    const params = new URLSearchParams(window.location.search)
    const code = params.get("invite")
    if (code) {
      setInviteCode(code)
      setInviteDialogOpen(true)
    }

    // Fetch pending invites for the user
    async function fetchInvites() {
      try {
        const token = localStorage.getItem("accessToken")
        const response = await fetch("http://localhost:8081/auth/me/invites", {
          headers: { Authorization: `Bearer ${token}` },
        })
        if (response.ok) {
          setPendingInvites(await response.json())
        }
      } catch (error) {
        // ignore
      }
    }
    fetchInvites()
  }, [])

  useEffect(() => {
    async function fetchData() {
      try {
        const [overviewData, historyData] = await Promise.all([
          analyticsApi.overview(),
          historyApi.list({ page: 0, size: 5 }),
        ])
        setOverview(overviewData)
        setRecentQueries(historyData.content)
      } catch (error) {
        console.log("Error fetching dashboard data:", error)
        toast.error("Failed to load dashboard data")
      } finally {
        setIsLoading(false)
      }
    }
    fetchData()
  }, [])

  const handleAcceptInvite = async () => {
    if (!inviteCode) return
    setAcceptingInvite(true)
    setInviteError(null)
    try {
      const token = localStorage.getItem("accessToken")
      const response = await fetch(`http://localhost:8081/projects/0/members/accept-invite`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ inviteCode }),
      })
      if (response.ok) {
        setInviteAccepted(true)
        toast.success("Invitation accepted! You are now a project member.")
        setTimeout(() => {
          setInviteDialogOpen(false)
          setInviteAccepted(false)
          // Remove invite param from URL
          const url = new URL(window.location.href)
          url.searchParams.delete("invite")
          window.history.replaceState({}, document.title, url.pathname)
        }, 2000)
      } else {
        const error = await response.json()
        setInviteError(error.message || "Failed to accept invite")
      }
    } catch (error) {
      setInviteError("Failed to accept invite")
    } finally {
      setAcceptingInvite(false)
    }
  }

  const handleQuickAnalyze = async () => {
    if (!quickQuery.trim()) {
      toast.error("Please enter a SQL query")
      return
    }

    setIsAnalyzing(true)
    try {
      const result = await queryApi.analyze(quickQuery, undefined)
      router.push(`/analyze?id=${result.queryId}`)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Analysis failed")
    } finally {
      setIsAnalyzing(false)
    }
  }

  const handleAcceptInviteFromList = async (inviteCode: string) => {
    setAcceptingInvite(true)
    setInviteError(null)
    try {
      const token = localStorage.getItem("accessToken")
      const response = await fetch(`http://localhost:8081/projects/0/members/accept-invite`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ inviteCode }),
      })
      if (response.ok) {
        toast.success("Invitation accepted! You are now a project member.")
        setPendingInvites((prev) => prev.filter((i) => i.inviteCode !== inviteCode))
      } else {
        const error = await response.json()
        setInviteError(error.message || "Failed to accept invite")
      }
    } catch (error) {
      setInviteError("Failed to accept invite")
    } finally {
      setAcceptingInvite(false)
    }
  }

  const getComplexityColor = (level: string) => {
    switch (level) {
      case "LOW":
        return "text-foreground"
      case "MEDIUM":
        return "text-muted-foreground"
      case "HIGH":
        return "text-foreground"
      case "CRITICAL":
        return "text-destructive"
      default:
        return "text-muted-foreground"
    }
  }

  return (
    <>
      <Dialog open={inviteDialogOpen} onOpenChange={setInviteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Accept Project Invitation</DialogTitle>
            <DialogDescription>
              {inviteAccepted
                ? "You have joined the project!"
                : "You have been invited to join a project. Accept to become a member."}
            </DialogDescription>
          </DialogHeader>
          {!inviteAccepted && (
            <div className="space-y-4">
              <Input value={inviteCode ?? ""} readOnly />
              {inviteError && <div className="text-destructive text-sm">{inviteError}</div>}
              <Button onClick={handleAcceptInvite} disabled={acceptingInvite} className="w-full">
                {acceptingInvite ? "Accepting..." : "Accept Invitation"}
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
      <Suspense fallback={<Loading />}>
        <div className="space-y-6 font-mono">
          <PageHeader title="Dashboard" description="Overview of your SQL analysis activity" />

          <div className="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-xs sm:text-sm font-medium">Total Queries</CardTitle>
                <Database className="h-4 w-4 text-muted-foreground flex-shrink-0" />
              </CardHeader>
              <CardContent>
                <div className="text-xl sm:text-2xl font-bold">{overview?.totalQueries ?? 0}</div>
                <p className="text-xs text-muted-foreground">Queries analyzed</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-xs sm:text-sm font-medium">Avg Complexity</CardTitle>
                <Activity className="h-4 w-4 text-muted-foreground flex-shrink-0" />
              </CardHeader>
              <CardContent>
                <div className="text-xl sm:text-2xl font-bold">{overview?.avgComplexity ?? 0}</div>
                <p className="text-xs text-muted-foreground">Complexity score</p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-xs sm:text-sm font-medium">Top Issue</CardTitle>
                <AlertTriangle className="h-4 w-4 text-muted-foreground flex-shrink-0" />
              </CardHeader>
              <CardContent>
                <div className="text-xl sm:text-2xl font-bold truncate">
                  {overview?.topIssues?.[0]?.type?.replace(/_/g, " ") ?? "None"}
                </div>
                <p className="text-xs text-muted-foreground">
                  {overview?.topIssues?.[0]?.count ?? 0} occurrences
                </p>
              </CardContent>
            </Card>
            <Card>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-xs sm:text-sm font-medium">This Week</CardTitle>
                <TrendingUp className="h-4 w-4 text-muted-foreground flex-shrink-0" />
              </CardHeader>
              <CardContent>
                <div className="text-xl sm:text-2xl font-bold">
                  {overview?.queryTrend?.reduce((sum, d) => sum + d.count, 0) ?? 0}
                </div>
                <p className="text-xs text-muted-foreground">Queries this period</p>
              </CardContent>
            </Card>
          </div>

          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-lg sm:text-xl">
                <Search className="h-4 sm:h-5 w-4 sm:w-5" />
                Quick Analysis
              </CardTitle>
              <CardDescription className="text-xs sm:text-sm">
                Enter a SQL query for instant analysis
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <SQLEditor
                value={quickQuery}
                onChange={setQuickQuery}
                placeholder="SELECT * FROM users WHERE ..."
                minHeight="100px"
              />
              <Button size="lg" onClick={handleQuickAnalyze} disabled={isAnalyzing} className="w-full">
                {isAnalyzing ? "Analyzing..." : "Analyze Query"}
              </Button>
            </CardContent>
          </Card>

          <div className="grid gap-6 grid-cols-1 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="text-lg sm:text-xl">Query Trend</CardTitle>
                <CardDescription className="text-xs sm:text-sm">Queries analyzed over time</CardDescription>
              </CardHeader>
              <CardContent>
                {overview?.queryTrend && overview.queryTrend.length > 0 ? (
                  <ChartContainer config={chartConfig} className="h-[200px] sm:h-[250px] w-full">
                    <BarChart data={overview.queryTrend} accessibilityLayer>
                      <CartesianGrid vertical={false} strokeDasharray="3 3" />
                      <XAxis
                        dataKey="date"
                        tickLine={false}
                        tickMargin={10}
                        axisLine={false}
                        tick={{ fontSize: 12 }}
                        tickFormatter={(value) => {
                          const date = new Date(value)
                          return date.toLocaleDateString("en-US", { month: "short", day: "numeric" })
                        }}
                      />
                      <YAxis tickLine={false} axisLine={false} tick={{ fontSize: 12 }} />
                      <ChartTooltip content={<ChartTooltipContent />} />
                      <Bar dataKey="count" fill="var(--color-count)" radius={4} />
                    </BarChart>
                  </ChartContainer>
                ) : (
                  <div className="flex h-[200px] sm:h-[250px] items-center justify-center text-muted-foreground">
                    No data available
                  </div>
                )}
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="text-lg sm:text-xl">Recent Queries</CardTitle>
                <CardDescription className="text-xs sm:text-sm">Your latest analyzed queries</CardDescription>
              </CardHeader>
              <CardContent>
                {recentQueries.length > 0 ? (
                  <div className="space-y-3 sm:space-y-4">
                    {recentQueries.map((query) => (
                      <div
                        key={query.id}
                        className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-2 sm:gap-4 border-b border-border pb-3 sm:pb-4 last:border-0 last:pb-0"
                      >
                        <div className="flex-1 min-w-0">
                          <p className="font-mono text-xs sm:text-sm truncate">{query.queryText}</p>
                          <p className="text-xs text-muted-foreground mt-1">
                            {query.queryType} - {new Date(query.analyzedAt).toLocaleDateString()}
                          </p>
                        </div>
                        <div className={`text-sm sm:text-base font-medium whitespace-nowrap ${getComplexityColor(query.complexityScore > 50 ? "HIGH" : "LOW")}`}>
                          {query.complexityScore}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="flex h-[150px] sm:h-[200px] items-center justify-center text-muted-foreground">
                    No queries analyzed yet
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {overview?.topIssues && overview.topIssues.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="text-lg sm:text-xl">Top Issues</CardTitle>
                <CardDescription className="text-xs sm:text-sm">Most common issues found in your queries</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="grid gap-3 sm:gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
                  {overview.topIssues.slice(0, 4).map((issue) => (
                    <div key={issue.type} className="flex items-center justify-between rounded-lg border p-3 sm:p-4">
                      <span className="text-xs sm:text-sm font-medium">{issue.type.replace(/_/g, " ")}</span>
                      <span className="text-xl sm:text-2xl font-bold">{issue.count}</span>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Pending Invitations Section */}
          {pendingInvites.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="text-lg sm:text-xl">Pending Invitations</CardTitle>
                <CardDescription className="text-xs sm:text-sm">Accept invitations to join projects</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  {pendingInvites.map((invite) => (
                    <div key={invite.inviteCode} className="flex items-center justify-between border rounded p-3">
                      <div>
                        <div className="font-medium">{invite.projectName || invite.projectId}</div>
                        <div className="text-xs text-muted-foreground">Invited as: {invite.role}</div>
                        <div className="text-xs text-muted-foreground">From: {invite.createdByEmail}</div>
                      </div>
                      <Button size="lg" onClick={() => handleAcceptInviteFromList(invite.inviteCode)} disabled={acceptingInvite}>
                        Accept
                      </Button>
                    </div>
                  ))}
                </div>
                {inviteError && <div className="text-destructive text-sm mt-2">{inviteError}</div>}
              </CardContent>
            </Card>
          )}
        </div>
      </Suspense>
    </>
  )
}