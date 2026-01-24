"use client"

import { useState } from "react"
import { AlertTriangle, CheckCircle, Download, Shield, ShieldAlert, ShieldCheck, ShieldX } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Progress } from "@/components/ui/progress"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { Skeleton } from "@/components/ui/skeleton"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { aiApi, type SecurityScanResponse } from "@/lib/api"

const languages = [
  { value: "SQL", label: "Raw SQL" },
  { value: "JAVA", label: "Java" },
  { value: "PYTHON", label: "Python" },
  { value: "JAVASCRIPT", label: "JavaScript" },
  { value: "CSHARP", label: "C#" },
]

export default function SecurityScannerPage() {
  const [code, setCode] = useState("")
  const [language, setLanguage] = useState("SQL")
  const [isLoading, setIsLoading] = useState(false)
  const [result, setResult] = useState<SecurityScanResponse | null>(null)

  const handleScan = async () => {
    if (!code.trim()) {
      toast.error("Please enter code to scan")
      return
    }

    setIsLoading(true)
    setResult(null)

    try {
      const scanResult = await aiApi.securityScan(code, language)
      setResult(scanResult)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Security scan failed")
    } finally {
      setIsLoading(false)
    }
  }

  const getSeverityIcon = (severity: string) => {
    switch (severity) {
      case "CRITICAL":
        return <ShieldX className="h-5 w-5" />
      case "HIGH":
        return <ShieldAlert className="h-5 w-5" />
      case "MEDIUM":
        return <Shield className="h-5 w-5" />
      case "LOW":
        return <ShieldCheck className="h-5 w-5" />
      default:
        return <Shield className="h-5 w-5" />
    }
  }

  const getSeverityBadge = (severity: string) => {
    switch (severity) {
      case "CRITICAL":
        return <Badge variant="destructive">Critical</Badge>
      case "HIGH":
        return <Badge variant="default">High</Badge>
      case "MEDIUM":
        return <Badge variant="secondary">Medium</Badge>
      case "LOW":
        return <Badge variant="outline">Low</Badge>
      default:
        return <Badge variant="outline">{severity}</Badge>
    }
  }

  const getRiskColor = (score: number) => {
    if (score >= 75) return "text-destructive"
    if (score >= 50) return "text-foreground"
    if (score >= 25) return "text-muted-foreground"
    return "text-foreground"
  }

  const exportReport = () => {
    if (!result) return

    const report = `
SQL Security Scan Report
========================
Generated: ${new Date().toISOString()}

Risk Score: ${result.riskScore}/100
Summary: ${result.summary}

Findings:
${result.findings.map((f, i) => `
${i + 1}. [${f.severity}] ${f.type}
   ${f.description}
   ${f.line ? `Line: ${f.line}` : ""}
   Recommendation: ${f.recommendation}
   ${f.secureExample ? `Secure Example:\n   ${f.secureExample}` : ""}
`).join("\n")}
    `.trim()

    const blob = new Blob([report], { type: "text/plain" })
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `security-scan-${Date.now()}.txt`
    a.click()
    URL.revokeObjectURL(url)
    toast.success("Report exported")
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Security Scanner</h1>
        <p className="text-muted-foreground">
          Scan your SQL queries and code for security vulnerabilities
        </p>
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              Code Input
            </CardTitle>
            <CardDescription>
              Enter SQL queries or code that interacts with your database
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-2">
              <Label>Language / Context</Label>
              <Select value={language} onValueChange={setLanguage}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {languages.map((lang) => (
                    <SelectItem key={lang.value} value={lang.value}>
                      {lang.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Code</Label>
              <Textarea
                placeholder={language === "SQL" 
                  ? `SELECT * FROM users WHERE id = ${"`userId`"}`
                  : `String query = "SELECT * FROM users WHERE id = " + userId;
stmt.executeQuery(query);`}
                value={code}
                onChange={(e) => setCode(e.target.value)}
                className="min-h-[280px] font-mono text-sm"
              />
            </div>

            <Button onClick={handleScan} disabled={isLoading} className="w-full">
              {isLoading ? "Scanning..." : "Scan for Vulnerabilities"}
            </Button>
          </CardContent>
        </Card>

        <div className="space-y-6">
          {isLoading ? (
            <Card>
              <CardHeader>
                <Skeleton className="h-6 w-32" />
              </CardHeader>
              <CardContent className="space-y-4">
                <Skeleton className="h-24" />
                <Skeleton className="h-32" />
                <Skeleton className="h-32" />
              </CardContent>
            </Card>
          ) : result ? (
            <>
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center justify-between">
                    <span className="flex items-center gap-2">
                      <AlertTriangle className="h-5 w-5" />
                      Risk Assessment
                    </span>
                    <Button variant="outline" size="sm" onClick={exportReport}>
                      <Download className="h-4 w-4 mr-2" />
                      Export
                    </Button>
                  </CardTitle>
                </CardHeader>
                <CardContent className="space-y-4">
                  <div className="flex items-center justify-between">
                    <span className={`text-4xl font-bold ${getRiskColor(result.riskScore)}`}>
                      {result.riskScore}
                    </span>
                    <span className="text-sm text-muted-foreground">/ 100 Risk Score</span>
                  </div>
                  <Progress value={result.riskScore} className="h-3" />
                  <p className="text-sm text-muted-foreground">{result.summary}</p>
                </CardContent>
              </Card>

              {result.findings.length > 0 ? (
                <Card>
                  <CardHeader>
                    <CardTitle>Security Findings</CardTitle>
                    <CardDescription>
                      {result.findings.length} issue{result.findings.length !== 1 ? "s" : ""} found
                    </CardDescription>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    {result.findings.map((finding, index) => (
                      <div key={index} className="rounded-lg border p-4 space-y-3">
                        <div className="flex items-start justify-between gap-4">
                          <div className="flex items-start gap-3">
                            {getSeverityIcon(finding.severity)}
                            <div>
                              <p className="font-medium">{finding.type.replace(/_/g, " ")}</p>
                              {finding.line && (
                                <p className="text-xs text-muted-foreground">Line {finding.line}</p>
                              )}
                            </div>
                          </div>
                          {getSeverityBadge(finding.severity)}
                        </div>

                        <p className="text-sm text-muted-foreground">{finding.description}</p>

                        <Separator />

                        <div className="space-y-2">
                          <p className="text-sm font-medium">Recommendation</p>
                          <p className="text-sm text-muted-foreground">{finding.recommendation}</p>
                        </div>

                        {finding.secureExample && (
                          <div className="space-y-2">
                            <p className="text-sm font-medium">Secure Example</p>
                            <pre className="rounded-lg bg-muted p-3 text-xs font-mono overflow-x-auto">
                              {finding.secureExample}
                            </pre>
                          </div>
                        )}
                      </div>
                    ))}
                  </CardContent>
                </Card>
              ) : (
                <Card>
                  <CardContent className="flex flex-col items-center justify-center py-8">
                    <CheckCircle className="h-12 w-12 text-foreground mb-4" />
                    <p className="text-lg font-medium">No vulnerabilities found</p>
                    <p className="text-sm text-muted-foreground">Your code passed the security scan</p>
                  </CardContent>
                </Card>
              )}
            </>
          ) : (
            <Card>
              <CardContent className="flex flex-col items-center justify-center py-16">
                <Shield className="h-12 w-12 text-muted-foreground mb-4" />
                <p className="text-lg font-medium">No scan results</p>
                <p className="text-sm text-muted-foreground">
                  Enter code and click Scan to check for vulnerabilities
                </p>
              </CardContent>
            </Card>
          )}
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Security Best Practices</CardTitle>
          <CardDescription>
            Follow these guidelines to write secure database code
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-lg border p-4 space-y-2">
              <p className="font-medium">Use Parameterized Queries</p>
              <p className="text-sm text-muted-foreground">
                Never concatenate user input directly into SQL strings
              </p>
            </div>
            <div className="rounded-lg border p-4 space-y-2">
              <p className="font-medium">Validate Input</p>
              <p className="text-sm text-muted-foreground">
                Always validate and sanitize user input before processing
              </p>
            </div>
            <div className="rounded-lg border p-4 space-y-2">
              <p className="font-medium">Least Privilege</p>
              <p className="text-sm text-muted-foreground">
                Use database accounts with minimal required permissions
              </p>
            </div>
            <div className="rounded-lg border p-4 space-y-2">
              <p className="font-medium">Avoid SELECT *</p>
              <p className="text-sm text-muted-foreground">
                Explicitly list columns to prevent data exposure
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  )
}
