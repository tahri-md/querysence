const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8082"

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
    if (typeof window !== "undefined") {
      localStorage.removeItem("accessToken")
      window.location.href = "/login"
    }
    throw new Error("Session expired")
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: "An error occurred" }))
    throw new Error(error.message || `API Error: ${response.status}`)
  }

  // Handle empty responses
  const text = await response.text()
  return (text ? JSON.parse(text) : null) as T
}

// Auth API
export const authApi = {
  login: (email: string, password: string) =>
    fetchApi<{ accessToken: string; user: { id?: number; email: string; fullName: string; role?: string; is_active?: boolean } }>("/auth/login", {
      method: "POST",
      body: { email, password },
    }),

  register: (email: string, password: string, fullName: string) =>
    fetchApi<{ id?: number; email: string; fullName: string; role?: string; is_active?: boolean }>("/auth/register", {
      method: "POST",
      body: { email, password, fullName },
    }),

  logout: async () => Promise.resolve(),

  me: () =>
    fetchApi<{ id?: number; email: string; fullName: string; role?: string; is_active?: boolean }>("/auth/me"),

  updateProfile: (fullName: string) =>
    fetchApi<{ id?: number; email: string; fullName: string; role?: string; is_active?: boolean }>("/auth/me", {
      method: "PUT",
      body: { fullName },
    }),

  changePassword: (oldPassword: string, newPassword: string) =>
    fetchApi<{ message: string }>("/auth/change-password", {
      method: "POST",
      body: { oldPassword, newPassword },
    }),

  updateEmail: (newEmail: string) =>
    fetchApi<{ message: string; user: { id?: number; email: string; fullName: string; role?: string; is_active?: boolean } }>("/auth/email", {
      method: "PUT",
      body: { newEmail },
    }),

  deleteAccount: () =>
    fetchApi<{ message: string }>("/auth/me", {
      method: "DELETE",
    }),
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
    fetchApi<ParseResult>(`/queries/parse?sql=${encodeURIComponent(sql)}&dialect=${encodeURIComponent(dialect)}`),

  analyze: (sql: string, projectId?: number, schemaId?: number, executionTimeMs?: number, dbConnectionId?: number) =>
    fetchApi<AnalysisResult>("/queries/analyze", {
      method: "POST",
      body: { sql, projectId, schemaId, executionTimeMs, dbConnectionId },
    }),

  get: (id: number) =>
    fetchApi<AnalysisResult>(`/queries/${id}`),
}

// DB Connections API — live database connections for schema sync + real EXPLAIN plans
export const dbConnectionsApi = {
  list: (projectId: number) =>
    fetchApi<DbConnection[]>(`/projects/${projectId}/db-connections`),

  create: (
    projectId: number,
    data: {
      name: string
      host: string
      port: number
      databaseName: string
      username: string
      password: string
      dialect: string
      sslEnabled?: boolean
      readOnlyEnforced?: boolean
    }
  ) =>
    fetchApi<DbConnection>(`/projects/${projectId}/db-connections`, {
      method: "POST",
      body: data,
    }),

  delete: (projectId: number, connectionId: number) =>
    fetchApi(`/projects/${projectId}/db-connections/${connectionId}`, { method: "DELETE" }),

  test: (projectId: number, connectionId: number) =>
    fetchApi<TestConnectionResult>(`/projects/${projectId}/db-connections/${connectionId}/test`, {
      method: "POST",
    }),

  syncSchema: (projectId: number, connectionId: number, schemaId?: number) =>
    fetchApi<SchemaSyncResult>(
      `/projects/${projectId}/db-connections/${connectionId}/sync-schema${schemaId ? `?schemaId=${schemaId}` : ""}`,
      { method: "POST" }
    ),
}

// AI Features API
export const aiApi = {
  nlToSql: (naturalLanguage: string, schemaId: number) =>
    fetchApi<NLToSQLResponse>("/ai/nl-to-sql", {
      method: "POST",
      body: { query: naturalLanguage, schemaId },
    }),

  explain: (sql: string) =>
    fetchApi<ExplainResponse>(`/ai/explain?sql=${encodeURIComponent(sql)}`, {
      method: "POST",
    }),

  optimize: (sql: string, schemaId?: number) => {
    const searchParams = new URLSearchParams()
    searchParams.set("sql", sql)
    if (schemaId !== undefined) searchParams.set("schemaId", String(schemaId))
    return fetchApi<OptimizeResponse>(`/ai/optimize?${searchParams.toString()}`, {
      method: "POST",
    })
  },

  securityScan: (code: string, context: string = "RAW_SQL") =>
    fetchApi<SecurityScanResponse>("/ai/security-scan", {
      method: "POST",
      body: { code, context },
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
    const queryString = searchParams.toString()
    return fetchApi<HistoryPage>(queryString ? `/history?${queryString}` : "/history")
  },

  get: (id: number) =>
    fetchApi<HistoryEntry>(`/history/${id}`),
}

// Analytics API
export const analyticsApi = {
  overview: () =>
    fetchApi<AnalyticsOverview>("/analytics/overview"),

  slowQueries: () =>
    fetchApi<HistoryPage>("/analytics/slow-queries"),
}

// Types
export interface Schema {
  id: number
  name: string
  dialect: string
  tables: TableDefinition[]
  source?: "MANUAL" | "SYNCED"
  dbConnectionId?: number | null
  lastSyncedAt?: string | null
}

export interface DbConnection {
  id: number
  projectId: number
  name: string
  host: string
  port: number
  databaseName: string
  username: string
  dialect: "POSTGRESQL" | "MYSQL" | "SQLSERVER" | "ORACLE"
  sslEnabled: boolean
  readOnlyEnforced: boolean
  status: "UNTESTED" | "CONNECTED" | "FAILED" | "EXPIRED_CREDENTIALS"
  lastTestedAt?: string | null
  lastSyncedAt?: string | null
  createdAt: string
  updatedAt: string
}

export interface TestConnectionResult {
  success: boolean
  status: string
  message: string
  latencyMs: number
}

export interface SchemaSyncResult {
  syncLogId: number
  schemaId?: number
  status: "RUNNING" | "SUCCESS" | "FAILED"
  tablesDiscovered?: number
  columnsDiscovered?: number
  indexesDiscovered?: number
  errorMessage?: string
  startedAt: string
  finishedAt?: string
}

export interface ExecutionPlan {
  id: number
  source: "STATIC_HEURISTIC" | "LIVE_EXPLAIN"
  planText: string
  estimatedCost?: number
  actualRows?: number
  actualTimeMs?: number
  usedIndexes: string[]
  fullTableScans: string[]
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
  joins: { type: string; table: string; alias?: string; condition: string }[]
  whereConditions: { column: string; table?: string; operator: string; value: string; isParameterized: boolean }[]
  orderBy: string[]
  groupBy: string[]
  subqueryCount: number
  hasDistinct: boolean
  hasHaving: boolean
  aggregateFunctions: string[]
}

export interface AnalysisResult {
  queryId: number
  complexity: {
    score: number
    level: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"
    factors: {
      name: string
      count: number
      points: number
      description: string
    }[]
    joinCount: number
    subqueryDepth: number
    aggregateCount: number
  }
  indexSuggestions: IndexSuggestion[]
  warnings: string[]
  dbConnectionId?: number | null
  executionPlan?: ExecutionPlan | null
}

export interface IndexSuggestion {
  tableName: string
  columns: string[]
  indexName?: string
  suggestionType: string
  impactScore: "HIGH" | "MEDIUM" | "LOW"
  reasoning: string
  createStatement?: string
}

export interface NLToSQLResponse {
  sql: string
  confidence: number
  explanation?: string
  alternativeQueries?: string[]
  valid?: boolean
  errorMessage?: string
  dialect?: string
}

export interface ExplainResponse {
  summary: string
  breakdown: { clause: string; explanation: string }[]
  businessLogic: string
}

export interface OptimizeResponse {
  suggestions?: {
    type: string
    priority?: string
    original?: string
    optimized?: string
    suggestion?: string
    explanation: string
    estimatedImprovement?: string
  }[]
  overallAssessment?: string
}

export interface SecurityScanResponse {
  findings: SecurityFinding[]
  riskScore: number
  summary: string
}

export interface SecurityFinding {
  type: string
  severity: "CRITICAL" | "HIGH" | "MEDIUM" | "LOW"
  line?: string | number
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
