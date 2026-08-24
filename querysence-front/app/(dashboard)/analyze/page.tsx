"use client"

import { useEffect, useState, Suspense } from "react"
import { useSearchParams } from "next/navigation"
import { AlertCircle, ArrowRight, CheckCircle, Database, Play, Zap } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { SQLEditor } from "@/components/sql-editor"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { queryApi, projectsApi, dbConnectionsApi, type AnalysisResult, type Schema, type DbConnection } from "@/lib/api"
import Loading from "./loading"

function AnalyzePageContent() {
  const searchParams = useSearchParams()
  const [sql, setSql] = useState("")
  const [projectId, setProjectId] = useState("")
  const [schemaId, setSchemaId] = useState("")
  const [executionTime, setExecutionTime] = useState("")
  const [projects, setProjects] = useState<{ id: number; name: string }[]>([])
  const [schemas, setSchemas] = useState<{ id: number; name: string; projectId: number; projectName: string }[]>([])
  const [connections, setConnections] = useState<DbConnection[]>([])
  const [dbConnectionId, setDbConnectionId] = useState<string>("none")
  const [isLoading, setIsLoading] = useState(false)
  const [isFetchingSchemas, setIsFetchingSchemas] = useState(true)
  const [result, setResult] = useState<AnalysisResult | null>(null)

  useEffect(() => {
    async function fetchSchemas() {
      try {
        const projectsList = await projectsApi.list()
        setProjects(projectsList)
        
        const allSchemas: { id: number; name: string; projectId: number; projectName: string }[] = []
        for (const project of projectsList) {
          const projectData = await projectsApi.get(project.id)
          for (const schema of projectData.schemas) {
            allSchemas.push({
              id: schema.id,
              name: schema.name,
              projectId: project.id,
              projectName: project.name,
            })
          }
        }
        setSchemas(allSchemas)
      } catch (error) {
        console.log("Error fetching schemas:", error)
      } finally {
        setIsFetchingSchemas(false)
      }
    }

    fetchSchemas()
  }, [])

  useEffect(() => {
    const queryId = searchParams.get("id")
    if (queryId !== null) {
      const parsedQueryId = parseInt(queryId, 10)
      async function fetchQuery() {
        try {
          const queryResult = await queryApi.get(parsedQueryId)
          setResult(queryResult)
        } catch (error) {
          console.log("Error fetching query:", error)
        }
      }
      fetchQuery()
    }
  }, [searchParams])

  useEffect(() => {
    async function fetchConnections() {
      if (!projectId) {
        setConnections([])
        return
      }
      try {
        const list = await dbConnectionsApi.list(parseInt(projectId, 10))
        setConnections(list.filter((c) => c.status === "CONNECTED"))
      } catch {
        setConnections([])
      }
    }
    setDbConnectionId("none")
    fetchConnections()
  }, [projectId])

  const handleAnalyze = async () => {
    if (!sql.trim()) {
      toast.error("Please enter a SQL query")
      return
    }

    if (!projectId) {
      toast.error("Please select a project")
      return
    }

    setIsLoading(true)
    setResult(null)

    try {
      const analysisResult = await queryApi.analyze(
        sql,
        parseInt(projectId),
        schemaId ? parseInt(schemaId) : undefined,
        executionTime ? parseInt(executionTime) : undefined,
        dbConnectionId !== "none" ? parseInt(dbConnectionId, 10) : undefined
      )
      setResult(analysisResult)
      toast.success(
        analysisResult.executionPlan?.source === "LIVE_EXPLAIN"
          ? "Analyzed with a live EXPLAIN plan"
          : "Query analyzed successfully"
      )
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Analysis failed")
    } finally {
      setIsLoading(false)
    }
  }

  const getComplexityLevel = (score: number) => {
    if (score <= 25) return { level: "LOW", color: "bg-foreground" }
    if (score <= 50) return { level: "MEDIUM", color: "bg-muted-foreground" }
    if (score <= 75) return { level: "HIGH", color: "bg-foreground" }
    return { level: "CRITICAL", color: "bg-destructive" }
  }

  const getImpactBadge = (impact: string) => {
    switch (impact) {
      case "HIGH":
        return <Badge variant="destructive">High Impact</Badge>
      case "MEDIUM":
        return <Badge variant="secondary">Medium Impact</Badge>
      case "LOW":
        return <Badge variant="outline">Low Impact</Badge>
      default:
        return <Badge>{impact}</Badge>
    }
  }

  return (
    <div className="container max-w-6xl font-mono py-6 space-y-8">
      <div>
        <h1 className="text-3xl font-black tracking-tight">Query Analyzer</h1>
        <p className="text-muted-foreground mt-2">
          Analyze your SQL queries for complexity, performance, and optimization opportunities
        </p>
      </div>

      <Separator />

      <Card>
        <CardHeader>
          <CardTitle>SQL Query Input</CardTitle>
          <CardDescription>Enter your SQL query to analyze</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <SQLEditor value={sql} onChange={setSql} />

          <div className="grid gap-4 md:grid-cols-4">
            <div className="space-y-2">
              <Label htmlFor="project">Project *</Label>
              <Select value={projectId} onValueChange={(value) => {
                setProjectId(value)
                setSchemaId("") 
              }}>
                <SelectTrigger id="project">
                  <SelectValue placeholder="Select a project" />
                </SelectTrigger>
                <SelectContent>
                  {isFetchingSchemas ? (
                    <SelectItem value="loading" disabled>
                      Loading projects...
                    </SelectItem>
                  ) : (
                    projects.map((project) => (
                      <SelectItem key={project.id} value={project.id.toString()}>
                        {project.name}
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="schema">Schema (optional)</Label>
              <Select value={schemaId} onValueChange={setSchemaId} disabled={!projectId}>
                <SelectTrigger id="schema">
                  <SelectValue placeholder="Select a schema" />
                </SelectTrigger>
                <SelectContent>
                  {schemas
                    .filter((schema) => schema.projectId === parseInt(projectId))
                    .map((schema) => (
                      <SelectItem key={schema.id} value={schema.id.toString()}>
                        {schema.name}
                      </SelectItem>
                    ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="db-connection" className="flex items-center gap-1.5">
                Live connection
              </Label>
              <Select value={dbConnectionId} onValueChange={setDbConnectionId} disabled={!projectId}>
                <SelectTrigger id="db-connection">
                  <SelectValue placeholder="Static analysis only" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">Static analysis only</SelectItem>
                  {connections.map((conn) => (
                    <SelectItem key={conn.id} value={conn.id.toString()}>
                      {conn.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="execution-time">Execution Time (ms)</Label>
              <Input
                id="execution-time"
                type="number"
                placeholder="e.g., 150"
                value={executionTime}
                onChange={(e) => setExecutionTime(e.target.value)}
              />
            </div>
          </div>

          <Button size="lg" onClick={handleAnalyze} disabled={isLoading} className="w-full">
            {isLoading ? (
              "Analyzing..."
            ) : (
              <>
                <Play className="w-4 h-4 mr-2" />
                Analyze Query
              </>
            )}
          </Button>
        </CardContent>
      </Card>

      <div className="space-y-6">
        {isLoading ? (
          <Card>
            <CardContent className="pt-6">
              <div className="space-y-4">
                <Skeleton className="h-8 w-48" />
                <Skeleton className="h-32 w-full" />
                <Skeleton className="h-24 w-full" />
              </div>
            </CardContent>
          </Card>
        ) : result ? (
          <>
            <Card>
              <CardHeader>
                <CardTitle>Complexity Analysis</CardTitle>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="space-y-2">
                  <div className="flex items-end gap-4">
                    <div className="text-5xl font-bold">{result.complexity.score}</div>
                    <div className="pb-2">
                      <Badge className={getComplexityLevel(result.complexity.score).color}>
                        {result.complexity.level}
                      </Badge>
                    </div>
                  </div>
                  <Progress value={result.complexity.score} className="h-2" />
                </div>

                <div className="grid gap-4 md:grid-cols-3">
                  <div className="flex items-center gap-3 p-4 rounded-lg border">
                    <Database className="w-8 h-8 text-muted-foreground" />
                    <div>
                      <div className="text-2xl font-bold">{result.complexity.joinCount}</div>
                      <div className="text-sm text-muted-foreground">Joins</div>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 p-4 rounded-lg border">
                    <Zap className="w-8 h-8 text-muted-foreground" />
                    <div>
                      <div className="text-2xl font-bold">{result.complexity.subqueryDepth}</div>
                      <div className="text-sm text-muted-foreground">Subquery Depth</div>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 p-4 rounded-lg border">
                    <ArrowRight className="w-8 h-8 text-muted-foreground" />
                    <div>
                      <div className="text-2xl font-bold">{result.complexity.aggregateCount}</div>
                      <div className="text-sm text-muted-foreground">Aggregates</div>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

            {result.executionPlan && (
              <Card>
                <CardHeader className="flex flex-row items-center justify-between space-y-0">
                  <div>
                    <CardTitle>Execution Plan</CardTitle>
                    <CardDescription>
                      {result.executionPlan.source === "LIVE_EXPLAIN"
                        ? "Real plan pulled from your connected database"
                        : "Estimated from static heuristics"}
                    </CardDescription>
                  </div>
                  <Badge
                    variant={result.executionPlan.source === "LIVE_EXPLAIN" ? "default" : "secondary"}
                    className="gap-1.5"
                  >
                    <Zap className="w-3 h-3" />
                    {result.executionPlan.source === "LIVE_EXPLAIN" ? "Live" : "Static"}
                  </Badge>
                </CardHeader>
                <CardContent className="space-y-4">
                  {result.executionPlan.source === "LIVE_EXPLAIN" && (
                    <div className="grid gap-4 md:grid-cols-3">
                      <div className="p-4 rounded-lg border">
                        <div className="text-2xl font-bold">
                          {result.executionPlan.actualTimeMs?.toFixed(1) ?? "—"} ms
                        </div>
                        <div className="text-sm text-muted-foreground">Actual time</div>
                      </div>
                      <div className="p-4 rounded-lg border">
                        <div className="text-2xl font-bold">{result.executionPlan.actualRows ?? "—"}</div>
                        <div className="text-sm text-muted-foreground">Rows returned</div>
                      </div>
                      <div className="p-4 rounded-lg border">
                        <div className="text-2xl font-bold">
                          {result.executionPlan.fullTableScans.length}
                        </div>
                        <div className="text-sm text-muted-foreground">Full table scans</div>
                      </div>
                    </div>
                  )}

                  {result.executionPlan.fullTableScans.length > 0 && (
                    <div className="flex items-start gap-2 rounded-lg bg-destructive/10 px-3 py-2 text-sm text-destructive">
                      <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
                      Full table scan on: {result.executionPlan.fullTableScans.join(", ")}
                    </div>
                  )}

                  {result.executionPlan.usedIndexes.length > 0 && (
                    <div className="flex items-start gap-2 rounded-lg bg-emerald-500/10 px-3 py-2 text-sm text-emerald-600 dark:text-emerald-400">
                      <CheckCircle className="w-4 h-4 shrink-0 mt-0.5" />
                      Used indexes: {result.executionPlan.usedIndexes.join(", ")}
                    </div>
                  )}

                  <pre className="rounded-lg border bg-muted p-4 text-xs overflow-x-auto whitespace-pre-wrap">
                    {result.executionPlan.planText}
                  </pre>
                </CardContent>
              </Card>
            )}

            {result.indexSuggestions.length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle>Index Suggestions</CardTitle>
                  <CardDescription>Recommended indexes to improve query performance</CardDescription>
                </CardHeader>
                <CardContent className="space-y-4">
                  {result.indexSuggestions.map((suggestion, index) => (
                    <div key={index} className="p-4 rounded-lg border space-y-3">
                      <div className="flex items-start justify-between gap-4">
                        <div className="space-y-1 flex-1">
                          <div className="font-semibold">{suggestion.tableName}</div>
                          <div className="text-sm text-muted-foreground">
                            Columns: {suggestion.columns.join(", ")}
                          </div>
                        </div>
                        {getImpactBadge(suggestion.impactScore)}
                      </div>
                      <p className="text-sm">{suggestion.reasoning}</p>
                      <Badge variant="outline">{suggestion.suggestionType}</Badge>
                    </div>
                  ))}
                </CardContent>
              </Card>
            )}

            {result.warnings.length > 0 && (
              <Card>
                <CardHeader>
                  <CardTitle>Warnings</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-3">
                    {result.warnings.map((warning, index) => (
                      <div key={index} className="flex gap-3 p-3 rounded-lg bg-yellow-50 dark:bg-yellow-950/20">
                        <AlertCircle className="w-5 h-5 text-yellow-600 dark:text-yellow-500 shrink-0 mt-0.5" />
                        <div className="text-sm">{warning}</div>
                      </div>
                    ))}
                  </div>
                </CardContent>
              </Card>
            )}

            {result.indexSuggestions.length === 0 && result.warnings.length === 0 && (
              <Card>
                <CardContent className="flex flex-col items-center justify-center py-12">
                  <CheckCircle className="w-12 h-12 text-green-600 dark:text-green-500 mb-4" />
                  <h3 className="text-lg font-semibold mb-2">Query looks good!</h3>
                  <p className="text-sm text-muted-foreground">No issues or suggestions found</p>
                </CardContent>
              </Card>
            )}
          </>
        ) : (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <Database className="w-12 h-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold mb-2">No analysis yet</h3>
              <p className="text-sm text-muted-foreground">Enter a SQL query and click Analyze to see results</p>
            </CardContent>
          </Card>
        )}
      </div>
    </div>
  )
}

export default function AnalyzePage() {
  return (
    <Suspense fallback={<Loading />}>
      <AnalyzePageContent />
    </Suspense>
  )
}