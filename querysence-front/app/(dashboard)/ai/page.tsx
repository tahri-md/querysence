"use client"

import { useEffect, useState } from "react"
import { Check, Copy, MessageSquare, Sparkles, Wand2, Zap } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Textarea } from "@/components/ui/textarea"
import { Skeleton } from "@/components/ui/skeleton"
import { SQLEditor } from "@/components/sql-editor"
import { Label } from "@/components/ui/label"
import { aiApi, projectsApi, type NLToSQLResponse, type ExplainResponse, type OptimizeResponse } from "@/lib/api"

export default function AIAssistantPage() {
  const [schemas, setSchemas] = useState<{ id: number; name: string; projectName: string }[]>([])
  const [isFetchingSchemas, setIsFetchingSchemas] = useState(true)

  const [nlQuery, setNlQuery] = useState("")
  const [nlSchemaId, setNlSchemaId] = useState<string>("")
  const [nlResult, setNlResult] = useState<NLToSQLResponse | null>(null)
  const [isNlLoading, setIsNlLoading] = useState(false)

  const [explainQuery, setExplainQuery] = useState("")
  const [explainResult, setExplainResult] = useState<ExplainResponse | null>(null)
  const [isExplainLoading, setIsExplainLoading] = useState(false)

  const [optimizeQuery, setOptimizeQuery] = useState("")
  const [optimizeSchemaId, setOptimizeSchemaId] = useState<string>("")
  const [optimizeResult, setOptimizeResult] = useState<OptimizeResponse | null>(null)
  const [isOptimizeLoading, setIsOptimizeLoading] = useState(false)

  const [copiedIndex, setCopiedIndex] = useState<number | null>(null)

  useEffect(() => {
    async function fetchSchemas() {
      try {
        const projects = await projectsApi.list()
        const allSchemas: { id: number; name: string; projectName: string }[] = []

        for (const project of projects) {
          const projectData = await projectsApi.get(project.id)
          for (const schema of projectData.schemas) {
            allSchemas.push({
              id: schema.id,
              name: schema.name,
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

  const handleNlToSql = async () => {
    if (!nlQuery.trim()) {
      toast.error("Please enter a description")
      return
    }
    if (!nlSchemaId) {
      toast.error("Please select a schema")
      return
    }

    setIsNlLoading(true)
    setNlResult(null)

    try {
      const result = await aiApi.nlToSql(nlQuery, parseInt(nlSchemaId))
      setNlResult(result)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to generate SQL")
    } finally {
      setIsNlLoading(false)
    }
  }

  const handleExplain = async () => {
    if (!explainQuery.trim()) {
      toast.error("Please enter a SQL query")
      return
    }

    setIsExplainLoading(true)
    setExplainResult(null)

    try {
      const result = await aiApi.explain(explainQuery)
      setExplainResult(result)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to explain query")
    } finally {
      setIsExplainLoading(false)
    }
  }

  const handleOptimize = async () => {
    if (!optimizeQuery.trim()) {
      toast.error("Please enter a SQL query")
      return
    }

    setIsOptimizeLoading(true)
    setOptimizeResult(null)

    try {
      const result = await aiApi.optimize(
        optimizeQuery,
        optimizeSchemaId ? parseInt(optimizeSchemaId) : undefined
      )
      setOptimizeResult(result)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to optimize query")
    } finally {
      setIsOptimizeLoading(false)
    }
  }

  const copyToClipboard = async (text: string, index: number) => {
    try {
      await navigator.clipboard.writeText(text)
      setCopiedIndex(index)
      toast.success("Copied to clipboard")
      setTimeout(() => setCopiedIndex(null), 2000)
    } catch {
      toast.error("Failed to copy")
    }
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">AI Assistant</h1>
        <p className="text-muted-foreground">
          Use AI to generate, explain, and optimize your SQL queries
        </p>
      </div>

      <Tabs defaultValue="nl-to-sql" className="space-y-6">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="nl-to-sql" className="flex items-center gap-2">
            <Wand2 className="h-4 w-4" />
            <span className="hidden sm:inline">Natural Language to SQL</span>
            <span className="sm:hidden">NL to SQL</span>
          </TabsTrigger>
          <TabsTrigger value="explain" className="flex items-center gap-2">
            <MessageSquare className="h-4 w-4" />
            <span className="hidden sm:inline">Explain Query</span>
            <span className="sm:hidden">Explain</span>
          </TabsTrigger>
          <TabsTrigger value="optimize" className="flex items-center gap-2">
            <Zap className="h-4 w-4" />
            <span className="hidden sm:inline">Optimize Query</span>
            <span className="sm:hidden">Optimize</span>
          </TabsTrigger>
        </TabsList>

        <TabsContent value="nl-to-sql" className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Wand2 className="h-5 w-5" />
                  Describe Your Query
                </CardTitle>
                <CardDescription>
                  Describe what data you want in plain English
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="space-y-2">
                  <Label>Schema</Label>
                  <Select value={nlSchemaId} onValueChange={setNlSchemaId}>
                    <SelectTrigger>
                      <SelectValue placeholder={isFetchingSchemas ? "Loading..." : "Select schema"} />
                    </SelectTrigger>
                    <SelectContent>
                      {schemas.map((schema) => (
                        <SelectItem key={schema.id} value={String(schema.id)}>
                          {schema.projectName} / {schema.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <div className="space-y-2">
                  <Label>Description</Label>
                  <Textarea
                    placeholder="Find all customers who placed more than 5 orders in the last month and show their total spending..."
                    value={nlQuery}
                    onChange={(e) => setNlQuery(e.target.value)}
                    className="min-h-[150px]"
                  />
                </div>

                <Button onClick={handleNlToSql} disabled={isNlLoading} className="w-full">
                  {isNlLoading ? "Generating..." : "Generate SQL"}
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Sparkles className="h-5 w-5" />
                  Generated SQL
                </CardTitle>
              </CardHeader>
              <CardContent>
                {isNlLoading ? (
                  <div className="space-y-4">
                    <Skeleton className="h-32" />
                    <Skeleton className="h-16" />
                  </div>
                ) : nlResult ? (
                  <div className="space-y-4">
                    <div className="relative">
                      <pre className="rounded-lg bg-muted p-4 text-sm font-mono overflow-x-auto">
                        {nlResult.sql}
                      </pre>
                      <Button
                        variant="ghost"
                        size="icon"
                        className="absolute top-2 right-2 h-8 w-8"
                        onClick={() => copyToClipboard(nlResult.sql, 0)}
                      >
                        {copiedIndex === 0 ? (
                          <Check className="h-4 w-4" />
                        ) : (
                          <Copy className="h-4 w-4" />
                        )}
                      </Button>
                    </div>

                    <div className="flex items-center gap-2">
                      <span className="text-sm text-muted-foreground">Confidence:</span>
                      <Badge variant="secondary">
                        {Math.round(nlResult.confidence * 100)}%
                      </Badge>
                    </div>

                    <div className="space-y-2">
                      <p className="text-sm font-medium">Explanation</p>
                      <p className="text-sm text-muted-foreground">{nlResult.explanation}</p>
                    </div>

                    {nlResult.alternativeQueries && nlResult.alternativeQueries.length > 0 && (
                      <div className="space-y-2">
                        <p className="text-sm font-medium">Alternative Queries</p>
                        {nlResult.alternativeQueries.map((alt, index) => (
                          <div key={index} className="relative">
                            <pre className="rounded-lg bg-muted p-3 text-xs font-mono overflow-x-auto">
                              {alt}
                            </pre>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="absolute top-1 right-1 h-6 w-6"
                              onClick={() => copyToClipboard(alt, index + 1)}
                            >
                              {copiedIndex === index + 1 ? (
                                <Check className="h-3 w-3" />
                              ) : (
                                <Copy className="h-3 w-3" />
                              )}
                            </Button>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-16 text-center">
                    <Wand2 className="h-12 w-12 text-muted-foreground mb-4" />
                    <p className="text-sm text-muted-foreground">
                      Describe what you want and AI will generate the SQL
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="explain" className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <MessageSquare className="h-5 w-5" />
                  SQL Query
                </CardTitle>
                <CardDescription>
                  Enter a SQL query to get a plain English explanation
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <SQLEditor
                  value={explainQuery}
                  onChange={setExplainQuery}
                  placeholder="SELECT c.name, SUM(o.total) as revenue
FROM customers c
JOIN orders o ON c.id = o.customer_id
WHERE o.created_at >= '2024-01-01'
GROUP BY c.id, c.name
HAVING SUM(o.total) > 1000
ORDER BY revenue DESC;"
                  minHeight="200px"
                />

                <Button onClick={handleExplain} disabled={isExplainLoading} className="w-full">
                  {isExplainLoading ? "Explaining..." : "Explain Query"}
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Sparkles className="h-5 w-5" />
                  Explanation
                </CardTitle>
              </CardHeader>
              <CardContent>
                {isExplainLoading ? (
                  <div className="space-y-4">
                    <Skeleton className="h-16" />
                    <Skeleton className="h-32" />
                    <Skeleton className="h-16" />
                  </div>
                ) : explainResult ? (
                  <div className="space-y-6">
                    <div className="space-y-2">
                      <p className="text-sm font-medium">Summary</p>
                      <p className="text-sm text-muted-foreground">{explainResult.summary}</p>
                    </div>

                    <div className="space-y-3">
                      <p className="text-sm font-medium">Breakdown</p>
                      {explainResult.breakdown.map((item, index) => (
                        <div key={index} className="rounded-lg border p-3 space-y-1">
                          <Badge variant="outline" className="font-mono text-xs">
                            {item.clause}
                          </Badge>
                          <p className="text-sm text-muted-foreground">{item.explanation}</p>
                        </div>
                      ))}
                    </div>

                    <div className="space-y-2">
                      <p className="text-sm font-medium">Business Logic</p>
                      <p className="text-sm text-muted-foreground">{explainResult.businessLogic}</p>
                    </div>
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-16 text-center">
                    <MessageSquare className="h-12 w-12 text-muted-foreground mb-4" />
                    <p className="text-sm text-muted-foreground">
                      Enter a query to get a detailed explanation
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </TabsContent>

        <TabsContent value="optimize" className="space-y-6">
          <div className="grid gap-6 lg:grid-cols-2">
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Zap className="h-5 w-5" />
                  SQL Query
                </CardTitle>
                <CardDescription>
                  Enter a SQL query to get optimization suggestions
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <SQLEditor
                  value={optimizeQuery}
                  onChange={setOptimizeQuery}
                  placeholder="SELECT * FROM orders WHERE customer_id IN (SELECT id FROM customers WHERE country = 'USA');"
                  minHeight="180px"
                />

                <div className="space-y-2">
                  <Label>Schema (optional)</Label>
                  <Select value={optimizeSchemaId} onValueChange={setOptimizeSchemaId}>
                    <SelectTrigger>
                      <SelectValue placeholder={isFetchingSchemas ? "Loading..." : "Select schema"} />
                    </SelectTrigger>
                    <SelectContent>
                      {schemas.map((schema) => (
                        <SelectItem key={schema.id} value={String(schema.id)}>
                          {schema.projectName} / {schema.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                <Button onClick={handleOptimize} disabled={isOptimizeLoading} className="w-full">
                  {isOptimizeLoading ? "Optimizing..." : "Get Optimization Suggestions"}
                </Button>
              </CardContent>
            </Card>

            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Sparkles className="h-5 w-5" />
                  Optimization Suggestions
                </CardTitle>
              </CardHeader>
              <CardContent>
                {isOptimizeLoading ? (
                  <div className="space-y-4">
                    <Skeleton className="h-32" />
                    <Skeleton className="h-32" />
                  </div>
                ) : optimizeResult ? (
                  <div className="space-y-4">
                    {optimizeResult.suggestions.map((suggestion, index) => (
                      <div key={index} className="rounded-lg border p-4 space-y-3">
                        <div className="flex items-center justify-between">
                          <Badge variant="secondary">{suggestion.type}</Badge>
                          {suggestion.estimatedImprovement && (
                            <span className="text-sm font-medium text-foreground">
                              ~{suggestion.estimatedImprovement} faster
                            </span>
                          )}
                        </div>

                        {suggestion.optimized && (
                          <div className="relative">
                            <pre className="rounded-lg bg-muted p-3 text-xs font-mono overflow-x-auto">
                              {suggestion.optimized}
                            </pre>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="absolute top-1 right-1 h-6 w-6"
                              onClick={() => copyToClipboard(suggestion.optimized!, index + 100)}
                            >
                              {copiedIndex === index + 100 ? (
                                <Check className="h-3 w-3" />
                              ) : (
                                <Copy className="h-3 w-3" />
                              )}
                            </Button>
                          </div>
                        )}

                        {suggestion.suggestion && (
                          <pre className="rounded-lg bg-muted p-3 text-xs font-mono overflow-x-auto">
                            {suggestion.suggestion}
                          </pre>
                        )}

                        <p className="text-sm text-muted-foreground">{suggestion.explanation}</p>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center py-16 text-center">
                    <Zap className="h-12 w-12 text-muted-foreground mb-4" />
                    <p className="text-sm text-muted-foreground">
                      Enter a query to get optimization suggestions
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  )
}
