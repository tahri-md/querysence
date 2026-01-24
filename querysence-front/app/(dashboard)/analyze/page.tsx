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
import { queryApi, projectsApi, type AnalysisResult, type Schema } from "@/lib/api"
import Loading from "./loading"

function AnalyzePageContent() {
  const searchParams = useSearchParams()
  const [sql, setSql] = useState("")
  const [projectId, setProjectId] = useState("")
  const [schemaId, setSchemaId] = useState("")
  const [executionTime, setExecutionTime] = useState("")
  const [projects, setProjects] = useState<{ id: number; name: string }[]>([])
  const [schemas, setSchemas] = useState<{ id: number; name: string; projectId: number; projectName: string }[]>([])
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
    if (queryId) {
      async function fetchQuery() {
        try {
          const queryResult = await queryApi.get(parseInt(queryId))
          setResult(queryResult)
        } catch (error) {
          console.log("Error fetching query:", error)
        }
      }
      fetchQuery()
    }
  }, [searchParams])

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
        executionTime ? parseInt(executionTime) : undefined
      )
      setResult(analysisResult)
      toast.success("Query analyzed successfully")
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
    <div className="container max-w-6xl py-6 space-y-8">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Query Analyzer</h1>
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

          <div className="grid gap-4 md:grid-cols-3">
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

          <Button onClick={handleAnalyze} disabled={isLoading} className="w-full">
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
                      <div className="text-2xl font-bold">{result.complexity.factors.joinCount}</div>
                      <div className="text-sm text-muted-foreground">Joins</div>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 p-4 rounded-lg border">
                    <Zap className="w-8 h-8 text-muted-foreground" />
                    <div>
                      <div className="text-2xl font-bold">{result.complexity.factors.subqueryDepth}</div>
                      <div className="text-sm text-muted-foreground">Subquery Depth</div>
                    </div>
                  </div>

                  <div className="flex items-center gap-3 p-4 rounded-lg border">
                    <ArrowRight className="w-8 h-8 text-muted-foreground" />
                    <div>
                      <div className="text-2xl font-bold">{result.complexity.factors.aggregateCount}</div>
                      <div className="text-sm text-muted-foreground">Aggregates</div>
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>

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
                          <div className="font-semibold">{suggestion.table}</div>
                          <div className="text-sm text-muted-foreground">
                            Columns: {suggestion.columns.join(", ")}
                          </div>
                        </div>
                        {getImpactBadge(suggestion.impact)}
                      </div>
                      <p className="text-sm">{suggestion.reasoning}</p>
                      <Badge variant="outline">{suggestion.type}</Badge>
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
                        <AlertCircle className="w-5 h-5 text-yellow-600 dark:text-yellow-500 flex-shrink-0 mt-0.5" />
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