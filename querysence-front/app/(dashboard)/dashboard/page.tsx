"use client"

import { useEffect, useState } from "react"
import { Button } from "@/components/ui/button"
import { Activity, AlertTriangle, ArrowRight, ArrowUpRight, Database, Search, TrendingUp } from "lucide-react"
import { Bar, BarChart, CartesianGrid, XAxis, YAxis } from "recharts"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import { PageHeader } from "@/components/page-header"
import { analyticsApi, type AnalyticsOverview, type HistoryEntry, historyApi } from "@/lib/api"
import { toast } from "sonner"
import Link from "next/link"

const chartConfig = {
  count: {
    label: "Queries",
    color: "var(--foreground)",
  },
} satisfies ChartConfig


export default function DashboardPage() {
  const [overview, setOverview] = useState<AnalyticsOverview | null>(null)
  const [recentQueries, setRecentQueries] = useState<HistoryEntry[]>([])

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
      } 
    }
    fetchData()
  }, [])

  return (
    <>

        <div className="space-y-6 font-mono">
          <PageHeader title="Dashboard" description="Overview of your SQL analysis activity" />

          <div className="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
            <Card className="group relative overflow-hidden border transition-all hover:border-primary/50 hover:text-primary  hover:shadow-md">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-xs sm:text-sm font-medium">Total Queries</CardTitle>
                <Database className="h-4 w-4 text-muted-foreground shrink-0" />
              </CardHeader>
              <CardContent>
                <div className="text-xl sm:text-2xl font-bold ">
                  {overview?.totalQueries ?? 0}
                </div>
                <p className="text-xs text-muted-foreground">Queries analyzed</p>
              </CardContent>
            </Card>
            <Card className="group relative overflow-hidden border transition-all hover:border-primary/50 hover:text-primary  hover:shadow-md">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-xs sm:text-sm font-medium">Avg Complexity</CardTitle>
                <Activity className="h-4 w-4 text-muted-foreground shrink-0" />
              </CardHeader>
              <CardContent>
                <div className="text-xl sm:text-2xl font-bold">{overview?.avgComplexity ?? 0}</div>
                <p className="text-xs text-muted-foreground">Complexity score</p>
              </CardContent>
            </Card>
            <Card className="group relative overflow-hidden border transition-all hover:border-primary/50 hover:text-primary  hover:shadow-md">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-xs sm:text-sm font-medium">Top Issue</CardTitle>
                <AlertTriangle className="h-4 w-4 text-muted-foreground shrink-0" />
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
            <Card className="group relative overflow-hidden border transition-all hover:border-primary/50 hover:text-primary  hover:shadow-md">
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <CardTitle className="text-xs sm:text-sm font-medium">This Week</CardTitle>
                <TrendingUp className="h-4 w-4 text-muted-foreground shrink-0" />
              </CardHeader>
              <CardContent>
                <div className="text-xl sm:text-2xl font-bold">
                  {overview?.queryTrend?.reduce((sum, d) => sum + d.count, 0) ?? 0}
                </div>
                <p className="text-xs text-muted-foreground">Queries this period</p>
              </CardContent>
            </Card>
          </div>

          <Card className="group relative overflow-hidden border transition-all hover:border-primary/50 hover:text-primary hover:shadow-md">
            <div className="absolute -right-8 -top-8 h-32 w-32 rounded-full bg-primary/5 transition-transform duration-300 group-hover:scale-150" />

            <CardHeader className="relative">
              <div className="mb-2 flex items-center justify-between">
                <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  <Search className="h-5 w-5" />
                </div>

                <ArrowUpRight className="h-5 w-5 text-muted-foreground transition-transform group-hover:-translate-y-0.5 group-hover:translate-x-0.5 group-hover:text-primary" />
              </div>

              <CardTitle className="text-lg sm:text-xl">
                Query Analyzer
              </CardTitle>

              <CardDescription className="max-w-md text-xs sm:text-sm">
                Understand how your SQL queries perform and find opportunities to
                improve their efficiency.
              </CardDescription>
            </CardHeader>

            <CardContent className="relative flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-center">
              <p className="text-sm text-muted-foreground">
                Analyze execution plans, indexes, and query performance.
              </p>

              <Button size="lg" className="shrink-0">
                <Link
                  href="/analyze"
                  className="flex items-center justify-center gap-2"
                >
                  Open Query Analyzer
                  <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                </Link>
              </Button>
            </CardContent>
          </Card>

          <div className="grid gap-6 grid-cols-1 lg:grid-cols-2">
            <Card className="group relative overflow-hidden border transition-all hover:border-primary/50 hover:text-primary hover:shadow-md">
              <CardHeader>
                <CardTitle className="text-lg sm:text-xl">Query Trend</CardTitle>
                <CardDescription className="text-xs sm:text-sm">Queries analyzed over time</CardDescription>
              </CardHeader>
              <CardContent>
                {overview?.queryTrend && overview.queryTrend.length > 0 ? (
                  <ChartContainer config={chartConfig} className="h-50 sm:h-62.5 w-full">
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
                      <Bar dataKey="count" fill="#7733ff" radius={4} />
                    </BarChart>
                  </ChartContainer>
                ) : (
                  <div className="flex h-50 sm:h-62.5 items-center justify-center text-muted-foreground">
                    No data available
                  </div>
                )}
              </CardContent>
            </Card>

            <Card className="group relative overflow-hidden border transition-all hover:border-primary/50 hover:text-primary hover:shadow-md">
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
                        <div className="text-sm sm:text-base font-medium whitespace-nowrap">
                          {query.complexityScore}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="flex h-50 sm:h-62.5 items-center justify-center text-muted-foreground">
                    No queries analyzed yet
                  </div>
                )}
              </CardContent>
            </Card>
          </div>

          {overview?.topIssues && overview.topIssues.length > 0 && (
            <Card className="group relative overflow-hidden border transition-all hover:border-primary/50 hover:shadow-md">
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
        </div>
    </>
  )
}