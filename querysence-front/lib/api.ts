const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://192.168.1.101:8081"

interface ApiOptions {
  method?: string
  body?: unknown
  headers?: Record<string, string>
}

async function fetchApi<T>(endpoint: string, options: ApiOptions = {}): Promise<T> {
  const { method = "GET", body, headers = {} } = options

  const token = typeof window !== "undefined" ? localStorage.getItem("accessToken") : null

  const config: RequestInit = {
    method,
    headers: {
      "Content-Type": "application/json",
      ...(token && { Authorization: `Bearer ${token}` }),
      ...headers,
    },
  }

  if (body) {
    config.body = JSON.stringify(body)
  }

  const response = await fetch(`${API_BASE_URL}${endpoint}`, config)

  if (response.status === 401) {
    // Try to refresh token
    const refreshed = await refreshToken()
    if (refreshed) {
      // Retry the original request
      const newToken = localStorage.getItem("accessToken")
      config.headers = {
        ...config.headers,
        Authorization: `Bearer ${newToken}`,
      }
      const retryResponse = await fetch(`${API_BASE_URL}${endpoint}`, config)
      if (!retryResponse.ok) {
        throw new Error(`API Error: ${retryResponse.status}`)
      }
      return retryResponse.json()
    } else {
      // Redirect to login
      if (typeof window !== "undefined") {
        localStorage.removeItem("accessToken")
        localStorage.removeItem("refreshToken")
        window.location.href = "/login"
      }
      throw new Error("Session expired")
    }
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: "An error occurred" }))
    throw new Error(error.message || `API Error: ${response.status}`)
  }

  // Handle empty responses
  const text = await response.text()
  return text ? JSON.parse(text) : null
}

async function refreshToken(): Promise<boolean> {
  const refreshToken = localStorage.getItem("refreshToken")
  if (!refreshToken) return false

  try {
    const response = await fetch(`${API_BASE_URL}/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken }),
    })

    if (!response.ok) return false

    const data = await response.json()
    localStorage.setItem("accessToken", data.accessToken)
    if (data.refreshToken) {
      localStorage.setItem("refreshToken", data.refreshToken)
    }
    return true
  } catch {
    return false
  }
}

// Auth API
export const authApi = {
  login: (username: string, password: string) =>
    fetchApi<{ accessToken: string; refreshToken: string; expiresIn: number }>("/auth/login", {
      method: "POST",
      body: { username, password },
    }),

  register: (email: string, password: string, fullName: string) =>
    fetchApi<{ id: number; email: string; fullName: string; role: string }>("/auth/register", {
      method: "POST",
      body: { email, password, fullName },
    }),

  logout: () => {
    const refreshToken = localStorage.getItem("refreshToken")
    return fetchApi("/auth/logout", {
      method: "POST",
      body: { refreshToken },
    })
  },

  me: () =>
    fetchApi<{ id: number; email: string; fullName: string; role: string }>("/auth/me"),
}

// Projects API
export const projectsApi = {
  list: () =>
    fetchApi<{ id: number; name: string; description: string; schemaCount: number }[]>("/projects"),

  get: (id: number) =>
    fetchApi<{ id: number; name: string; description: string; schemas: Schema[] }>(`/projects/${id}`),

  create: (name: string, description: string) =>
    fetchApi<{ id: number; name: string; description: string; createdAt: string }>("/projects", {
      method: "POST",
      body: { name, description },
    }),

  delete: (id: number) =>
    fetchApi(`/projects/${id}`, { method: "DELETE" }),

  createSchema: (projectId: number, name: string, dialect: string, ddlScript?: string) =>
    fetchApi<Schema>(`/projects/${projectId}/schemas`, {
      method: "POST",
      body: { name, dialect, ddlScript },
    }),
}

// Schemas API
export const schemasApi = {
  get: (id: number) =>
    fetchApi<Schema>(`/schemas/${id}`),

  delete: (id: number) =>
    fetchApi(`/schemas/${id}`, { method: "DELETE" }),

  addTable: (schemaId: number, tableName: string, columns: ColumnDefinition[], indexes?: IndexDefinition[]) =>
    fetchApi<TableDefinition>(`/schemas/${schemaId}/tables`, {
      method: "POST",
      body: { tableName, columns, indexes },
    }),
}

// Query Analysis API
export const queryApi = {
  parse: (sql: string, dialect: string = "POSTGRESQL") =>
    fetchApi<ParseResult>("/queries/parse", {
      method: "POST",
      body: { sql, dialect },
    }),

  analyze: (sql: string,projectId:number, schemaId?: number, executionTimeMs?: number) =>
    fetchApi<AnalysisResult>("/queries/analyze", {
      method: "POST",
      body: { sql,projectId, schemaId, executionTimeMs },
    }),

  get: (id: number) =>
    fetchApi<AnalysisResult>(`/queries/${id}`),
}

// AI Features API
export const aiApi = {
  nlToSql: (naturalLanguage: string, schemaId: number) =>
    fetchApi<NLToSQLResponse>("/ai/nl-to-sql", {
      method: "POST",
      body: { naturalLanguage, schemaId },
    }),

  explain: (sql: string) =>
    fetchApi<ExplainResponse>("/ai/explain", {
      method: "POST",
      body: { sql },
    }),

  optimize: (sql: string, schemaId?: number, currentExecutionMs?: number) =>
    fetchApi<OptimizeResponse>("/ai/optimize", {
      method: "POST",
      body: { sql, schemaId, currentExecutionMs },
    }),

  securityScan: (sql: string, context: string = "SQL") =>
    fetchApi<SecurityScanResponse>("/ai/security-scan", {
      method: "POST",
      body: { sql, context },
    }),
}

// History API
export const historyApi = {
  list: (params?: { page?: number; size?: number; projectId?: number; startDate?: string; endDate?: string }) => {
    const searchParams = new URLSearchParams()
    if (params?.page !== undefined) searchParams.set("page", String(params.page))
    if (params?.size !== undefined) searchParams.set("size", String(params.size))
    if (params?.projectId !== undefined) searchParams.set("projectId", String(params.projectId))
    if (params?.startDate) searchParams.set("startDate", params.startDate)
    if (params?.endDate) searchParams.set("endDate", params.endDate)
    return fetchApi<HistoryPage>(`/history?${searchParams.toString()}`)
  },

  get: (id: number) =>
    fetchApi<HistoryEntry>(`/history/${id}`),
}

// Analytics API
export const analyticsApi = {
  overview: () =>
    fetchApi<AnalyticsOverview>("/analytics/overview"),

  slowQueries: () =>
    fetchApi<SlowQuery[]>("/analytics/slow-queries"),
}

// Types
export interface Schema {
  id: number
  name: string
  dialect: string
  tables: TableDefinition[]
}

export interface TableDefinition {
  id: number
  tableName: string
  estimatedRows: number
  description?: string
  columns: ColumnDefinition[]
  indexes?: IndexDefinition[]
}

export interface ColumnDefinition {
  id?: number
  columnName: string
  dataType: string
  isNullable: boolean
  isPrimaryKey: boolean
  isForeignKey: boolean
  referencesTable?: string
  referencesColumn?: string
}

export interface IndexDefinition {
  id?: number
  indexName: string
  columns: string[]
  isUnique: boolean
  indexType: string
}

export interface ParseResult {
  valid: boolean
  queryType: string
  tables: string[]
  columns: string[]
  joins: { type: string; table: string; condition: string }[]
  whereConditions: string[]
  orderBy: string[]
  groupBy: string[]
  subqueries: string[]
}

export interface AnalysisResult {
  queryId: number
  complexity: {
    score: number
    level: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"
    factors: {
      joinCount: number
      subqueryDepth: number
      aggregateCount: number
    }
  }
  indexSuggestions: IndexSuggestion[]
  warnings: string[]
}

export interface IndexSuggestion {
  table: string
  columns: string[]
  type: string
  impact: "HIGH" | "MEDIUM" | "LOW"
  reasoning: string
}

export interface NLToSQLResponse {
  sql: string
  confidence: number
  explanation: string
  alternativeQueries: string[]
}

export interface ExplainResponse {
  summary: string
  breakdown: { clause: string; explanation: string }[]
  businessLogic: string
}

export interface OptimizeResponse {
  suggestions: {
    type: string
    original?: string
    optimized?: string
    suggestion?: string
    explanation: string
    estimatedImprovement?: string
  }[]
}

export interface SecurityScanResponse {
  findings: SecurityFinding[]
  riskScore: number
  summary: string
}

export interface SecurityFinding {
  type: string
  severity: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW"
  line?: number
  description: string
  recommendation: string
  secureExample?: string
}

export interface HistoryPage {
  content: HistoryEntry[]
  totalElements: number
  totalPages: number
}

export interface HistoryEntry {
  id: number
  queryText: string
  queryType: string
  complexityScore: number
  executionTimeMs?: number
  analyzedAt: string
  projectName?: string
}

export interface AnalyticsOverview {
  totalQueries: number
  avgComplexity: number
  topIssues: { type: string; count: number }[]
  queryTrend: { date: string; count: number }[]
}

export interface SlowQuery {
  queryId: number
  baseline: number
  current: number
  increase: string
  possibleCauses: string[]
}
