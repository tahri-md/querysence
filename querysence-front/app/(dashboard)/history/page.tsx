"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import { Calendar, ChevronLeft, ChevronRight, Download, Eye, Filter, History, Search } from "lucide-react"
import { format } from "date-fns"
import { toast } from "sonner"
import { useSearchParams } from "next/navigation"
import { Suspense } from "react"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Calendar as CalendarComponent } from "@/components/ui/calendar"
import { historyApi, projectsApi, queryApi, type HistoryEntry, type AnalysisResult } from "@/lib/api"

const Loading = () => null

export default function HistoryPage() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const [history, setHistory] = useState<HistoryEntry[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [currentPage, setCurrentPage] = useState(0)
  const [pageSize] = useState(10)

  const [projects, setProjects] = useState<{ id: number; name: string }[]>([])
  const [selectedProject, setSelectedProject] = useState<string>("")
  const [startDate, setStartDate] = useState<Date | undefined>()
  const [endDate, setEndDate] = useState<Date | undefined>()

  const [selectedEntry, setSelectedEntry] = useState<HistoryEntry | null>(null)
  const [entryDetails, setEntryDetails] = useState<AnalysisResult | null>(null)
  const [isLoadingDetails, setIsLoadingDetails] = useState(false)

  useEffect(() => {
    async function fetchProjects() {
      try {
        const data = await projectsApi.list()
        setProjects(data.map((p) => ({ id: p.id, name: p.name })))
      } catch (error) {
        console.log("Error fetching projects:", error)
      }
    }
    fetchProjects()
  }, [])

  const fetchHistory = async () => {
    setIsLoading(true)
    try {
      const data = await historyApi.list({
        page: currentPage,
        size: pageSize,
        projectId: selectedProject ? parseInt(selectedProject) : undefined,
        startDate: startDate ? format(startDate, "yyyy-MM-dd") : undefined,
        endDate: endDate ? format(endDate, "yyyy-MM-dd") : undefined,
      })
      setHistory(data.content)
      setTotalPages(data.totalPages)
      setTotalElements(data.totalElements)
    } catch (error) {
      console.log("Error fetching history:", error)
      toast.error("Failed to load history")
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchHistory()
  }, [currentPage, selectedProject, startDate, endDate])

  const handleViewDetails = async (entry: HistoryEntry) => {
    setSelectedEntry(entry)
    setIsLoadingDetails(true)
    try {
      const details = await queryApi.get(entry.id)
      setEntryDetails(details)
    } catch (error) {
      toast.error("Failed to load details")
    } finally {
      setIsLoadingDetails(false)
    }
  }

  const handleAnalyzeAgain = (entry: HistoryEntry) => {
    router.push(`/analyze?query=${encodeURIComponent(entry.queryText)}`)
  }

  const getComplexityBadge = (score: number) => {
    if (score <= 25) return <Badge variant="outline">Low ({score})</Badge>
    if (score <= 50) return <Badge variant="secondary">Medium ({score})</Badge>
    if (score <= 75) return <Badge variant="default">High ({score})</Badge>
    return <Badge variant="destructive">Critical ({score})</Badge>
  }

  const exportToCSV = () => {
    if (history.length === 0) {
      toast.error("No data to export")
      return
    }

    const headers = ["ID", "Query Type", "Query Text", "Complexity Score", "Execution Time (ms)", "Analyzed At"]
    const rows = history.map((entry) => [
      entry.id,
      entry.queryType,
      `"${entry.queryText.replace(/"/g, '""')}"`,
      entry.complexityScore,
      entry.executionTimeMs || "",
      entry.analyzedAt,
    ])

    const csv = [headers.join(","), ...rows.map((r) => r.join(","))].join("\n")
    const blob = new Blob([csv], { type: "text/csv" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `query-history-${format(new Date(), "yyyy-MM-dd")}.csv`
    a.click()
    URL.revokeObjectURL(url)
    toast.success("Exported to CSV")
  }

  const clearFilters = () => {
    setSelectedProject("")
    setStartDate(undefined)
    setEndDate(undefined)
    setCurrentPage(0)
  }

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Query History</h1>
          <p className="text-muted-foreground">
            View and analyze your past SQL queries
          </p>
        </div>
        <Button variant="outline" onClick={exportToCSV}>
          <Download className="h-4 w-4 mr-2" />
          Export CSV
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Filter className="h-4 w-4" />
            Filters
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-4">
            <div className="space-y-2">
              <Label>Project</Label>
              <Select value={selectedProject} onValueChange={(v) => { setSelectedProject(v); setCurrentPage(0); }}>
                <SelectTrigger>
                  <SelectValue placeholder="All projects" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">All projects</SelectItem>
                  {projects.map((project) => (
                    <SelectItem key={project.id} value={String(project.id)}>
                      {project.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Start Date</Label>
              <Popover>
                <PopoverTrigger asChild>
                  <Button variant="outline" className="w-full justify-start text-left font-normal bg-transparent">
                    <Calendar className="mr-2 h-4 w-4" />
                    {startDate ? format(startDate, "PPP") : "Pick a date"}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0">
                  <CalendarComponent
                    mode="single"
                    selected={startDate}
                    onSelect={(d) => { setStartDate(d); setCurrentPage(0); }}
                    initialFocus
                  />
                </PopoverContent>
              </Popover>
            </div>

            <div className="space-y-2">
              <Label>End Date</Label>
              <Popover>
                <PopoverTrigger asChild>
                  <Button variant="outline" className="w-full justify-start text-left font-normal bg-transparent">
                    <Calendar className="mr-2 h-4 w-4" />
                    {endDate ? format(endDate, "PPP") : "Pick a date"}
                  </Button>
                </PopoverTrigger>
                <PopoverContent className="w-auto p-0">
                  <CalendarComponent
                    mode="single"
                    selected={endDate}
                    onSelect={(d) => { setEndDate(d); setCurrentPage(0); }}
                    initialFocus
                  />
                </PopoverContent>
              </Popover>
            </div>

            <div className="flex items-end">
              <Button variant="ghost" onClick={clearFilters} className="w-full">
                Clear Filters
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <History className="h-5 w-5" />
            Query History
          </CardTitle>
          <CardDescription>
            {totalElements} total queries
          </CardDescription>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-12" />
              ))}
            </div>
          ) : history.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <Search className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-lg font-medium">No queries found</p>
              <p className="text-sm text-muted-foreground">
                {selectedProject || startDate || endDate
                  ? "Try adjusting your filters"
                  : "Start analyzing queries to see them here"}
              </p>
            </div>
          ) : (
            <>
              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Query</TableHead>
                      <TableHead className="w-[100px]">Type</TableHead>
                      <TableHead className="w-[120px]">Complexity</TableHead>
                      <TableHead className="w-[100px]">Time (ms)</TableHead>
                      <TableHead className="w-[150px]">Analyzed</TableHead>
                      <TableHead className="w-[80px]">Actions</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {history.map((entry) => (
                      <TableRow key={entry.id}>
                        <TableCell className="font-mono text-xs max-w-[300px] truncate">
                          {entry.queryText}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline">{entry.queryType}</Badge>
                        </TableCell>
                        <TableCell>{getComplexityBadge(entry.complexityScore)}</TableCell>
                        <TableCell className="text-muted-foreground">
                          {entry.executionTimeMs ?? "-"}
                        </TableCell>
                        <TableCell className="text-muted-foreground text-sm">
                          {format(new Date(entry.analyzedAt), "MMM d, yyyy HH:mm")}
                        </TableCell>
                        <TableCell>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleViewDetails(entry)}
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {currentPage + 1} of {totalPages}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                      disabled={currentPage === 0}
                    >
                      <ChevronLeft className="h-4 w-4" />
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setCurrentPage((p) => Math.min(totalPages - 1, p + 1))}
                      disabled={currentPage === totalPages - 1}
                    >
                      Next
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      <Suspense fallback={<Loading />}>
        <Dialog open={!!selectedEntry} onOpenChange={() => { setSelectedEntry(null); setEntryDetails(null); }}>
          <DialogContent className="max-w-2xl">
            <DialogHeader>
              <DialogTitle>Query Details</DialogTitle>
              <DialogDescription>
                Analyzed on {selectedEntry && format(new Date(selectedEntry.analyzedAt), "PPP 'at' HH:mm")}
              </DialogDescription>
            </DialogHeader>
            {isLoadingDetails ? (
              <div className="space-y-4 py-4">
                <Skeleton className="h-24" />
                <Skeleton className="h-16" />
              </div>
            ) : selectedEntry && (
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label>Query</Label>
                  <pre className="rounded-lg bg-muted p-4 text-sm font-mono overflow-x-auto max-h-[200px]">
                    {selectedEntry.queryText}
                  </pre>
                </div>

                <div className="grid grid-cols-3 gap-4">
                  <div className="space-y-1">
                    <Label className="text-muted-foreground">Type</Label>
                    <p className="font-medium">{selectedEntry.queryType}</p>
                  </div>
                  <div className="space-y-1">
                    <Label className="text-muted-foreground">Complexity</Label>
                    <p>{getComplexityBadge(selectedEntry.complexityScore)}</p>
                  </div>
                  <div className="space-y-1">
                    <Label className="text-muted-foreground">Execution Time</Label>
                    <p className="font-medium">{selectedEntry.executionTimeMs ?? "-"} ms</p>
                  </div>
                </div>

                {entryDetails && (
                  <>
                    {entryDetails.indexSuggestions.length > 0 && (
                      <div className="space-y-2">
                        <Label>Index Suggestions</Label>
                        <div className="space-y-2">
                          {entryDetails.indexSuggestions.map((suggestion, i) => (
                            <div key={i} className="rounded border p-3 text-sm">
                              <p className="font-mono">{suggestion.table} ({suggestion.columns.join(", ")})</p>
                              <p className="text-muted-foreground text-xs mt-1">{suggestion.reasoning}</p>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}

                    {entryDetails.warnings.length > 0 && (
                      <div className="space-y-2">
                        <Label>Warnings</Label>
                        <ul className="space-y-1 text-sm">
                          {entryDetails.warnings.map((warning, i) => (
                            <li key={i} className="text-muted-foreground">- {warning}</li>
                          ))}
                        </ul>
                      </div>
                    )}
                  </>
                )}

                <div className="flex justify-end gap-2 pt-4">
                  <Button variant="outline" onClick={() => { setSelectedEntry(null); setEntryDetails(null); }}>
                    Close
                  </Button>
                  <Button onClick={() => handleAnalyzeAgain(selectedEntry)}>
                    Analyze Again
                  </Button>
                </div>
              </div>
            )}
          </DialogContent>
        </Dialog>
      </Suspense>
    </div>
  )
}
