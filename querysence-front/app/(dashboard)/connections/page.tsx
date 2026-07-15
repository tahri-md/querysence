"use client"

import { useEffect, useState } from "react"
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  CircleSlash,
  Database,
  Loader2,
  Plug,
  Plus,
  RefreshCw,
  ShieldCheck,
  Trash2,
} from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Switch } from "@/components/ui/switch"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { dbConnectionsApi, projectsApi, type DbConnection, type Schema } from "@/lib/api"

interface Project {
  id: number
  name: string
}

const STATUS_CONFIG: Record<
  DbConnection["status"],
  { label: string; dot: string; badge: "default" | "secondary" | "destructive" | "outline" }
> = {
  CONNECTED: { label: "Connected", dot: "bg-emerald-500", badge: "outline" },
  UNTESTED: { label: "Untested", dot: "bg-muted-foreground", badge: "secondary" },
  FAILED: { label: "Failed", dot: "bg-destructive", badge: "destructive" },
  EXPIRED_CREDENTIALS: { label: "Credentials expired", dot: "bg-amber-500", badge: "destructive" },
}

export default function ConnectionsPage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [projectId, setProjectId] = useState<number | null>(null)
  const [connections, setConnections] = useState<DbConnection[]>([])
  const [schemas, setSchemas] = useState<Schema[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [actioningId, setActioningId] = useState<number | null>(null)

  const [syncDialogConnection, setSyncDialogConnection] = useState<DbConnection | null>(null)
  const [syncTargetSchemaId, setSyncTargetSchemaId] = useState<string>("new")
  const [isSyncing, setIsSyncing] = useState(false)

  const [isCreateOpen, setIsCreateOpen] = useState(false)
  const [isCreating, setIsCreating] = useState(false)
  const [form, setForm] = useState({
    name: "",
    host: "",
    port: "5432",
    databaseName: "",
    username: "",
    password: "",
    dialect: "POSTGRESQL",
    sslEnabled: false,
    readOnlyEnforced: true,
  })

  useEffect(() => {
    async function init() {
      try {
        const list = await projectsApi.list()
        setProjects(list)
        if (list.length > 0) setProjectId(list[0].id)
      } catch {
        toast.error("Couldn't load projects")
      } finally {
        setIsLoading(false)
      }
    }
    init()
  }, [])

  useEffect(() => {
    if (projectId === null) return
    void loadConnections(projectId)
    void loadSchemas(projectId)
  }, [projectId])

  async function loadConnections(pid: number) {
    setIsLoading(true)
    try {
      const list = await dbConnectionsApi.list(pid)
      setConnections(list)
    } catch {
      toast.error("Couldn't load connections")
    } finally {
      setIsLoading(false)
    }
  }

  async function loadSchemas(pid: number) {
    try {
      const project = await projectsApi.get(pid)
      setSchemas(project.schemas ?? [])
    } catch {
      setSchemas([])
    }
  }

  async function handleCreate() {
    if (!projectId) return
    if (!form.name || !form.host || !form.databaseName || !form.username || !form.password) {
      toast.error("Fill in all required fields")
      return
    }
    setIsCreating(true)
    try {
      await dbConnectionsApi.create(projectId, {
        name: form.name,
        host: form.host,
        port: parseInt(form.port, 10),
        databaseName: form.databaseName,
        username: form.username,
        password: form.password,
        dialect: form.dialect,
        sslEnabled: form.sslEnabled,
        readOnlyEnforced: form.readOnlyEnforced,
      })
      toast.success("Connection added")
      setIsCreateOpen(false)
      setForm({ ...form, name: "", host: "", databaseName: "", username: "", password: "" })
      void loadConnections(projectId)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Couldn't create connection")
    } finally {
      setIsCreating(false)
    }
  }

  async function handleTest(connectionId: number) {
    if (!projectId) return
    setActioningId(connectionId)
    try {
      const result = await dbConnectionsApi.test(projectId, connectionId)
      if (result.success) {
        toast.success(`Connected in ${result.latencyMs}ms`)
      } else {
        toast.error(result.message || "Connection failed")
      }
      void loadConnections(projectId)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Test failed")
    } finally {
      setActioningId(null)
    }
  }

  function openSyncDialog(conn: DbConnection) {
    // Default to this connection's own last-synced schema if it has one, otherwise "create new"
    const linkedSchema = schemas.find((s) => s.dbConnectionId === conn.id)
    setSyncTargetSchemaId(linkedSchema ? linkedSchema.id.toString() : "new")
    setSyncDialogConnection(conn)
  }

  async function handleSync() {
    if (!projectId || !syncDialogConnection) return
    const connectionId = syncDialogConnection.id
    const schemaId = syncTargetSchemaId !== "new" ? parseInt(syncTargetSchemaId, 10) : undefined

    setIsSyncing(true)
    setActioningId(connectionId)
    try {
      const result = await dbConnectionsApi.syncSchema(projectId, connectionId, schemaId)
      if (result.status === "SUCCESS") {
        toast.success(
          `Synced ${result.tablesDiscovered ?? 0} tables, ${result.columnsDiscovered ?? 0} columns, ${result.indexesDiscovered ?? 0} indexes`
        )
        setSyncDialogConnection(null)
      } else {
        toast.error(result.errorMessage || "Schema sync failed")
      }
      void loadConnections(projectId)
      void loadSchemas(projectId)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Sync failed")
    } finally {
      setIsSyncing(false)
      setActioningId(null)
    }
  }

  async function handleDelete(connectionId: number) {
    if (!projectId) return
    try {
      await dbConnectionsApi.delete(projectId, connectionId)
      toast.success("Connection removed")
      void loadConnections(projectId)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Couldn't remove connection")
    }
  }

  const connectedCount = connections.filter((c) => c.status === "CONNECTED").length
  const syncedCount = connections.filter((c) => c.lastSyncedAt).length

  return (
    <div className="mx-auto max-w-5xl space-y-8">
      {/* Eyebrow + heading, in the same idiom as the rest of the app */}
      <div className="space-y-4">
        <div className="inline-flex items-center gap-2 rounded-full border border-border bg-secondary px-3 py-1 text-xs font-medium text-muted-foreground">
          <Plug className="h-3 w-3 text-primary" />
          Live connections
        </div>
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div className="space-y-1">
            <h1 className="text-2xl font-semibold tracking-tight">
              Connect your <span className="text-primary">database</span>
            </h1>
            <p className="text-sm text-muted-foreground max-w-lg">
              Give QuerySense read-only access to pull real schemas and run live EXPLAIN plans,
              instead of relying on static heuristics alone.
            </p>
          </div>
          <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
            <DialogTrigger asChild>
              <Button className="gap-2 bg-primary text-primary-foreground hover:bg-primary/90" disabled={!projectId}>
                <Plus className="h-4 w-4" />
                Add connection
              </Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-md">
              <DialogHeader>
                <DialogTitle>Add a database connection</DialogTitle>
                <DialogDescription>
                  Credentials are encrypted at rest. Use a read-only database user if you can.
                </DialogDescription>
              </DialogHeader>
              <div className="grid gap-3 py-2">
                <div className="grid gap-1.5">
                  <Label htmlFor="conn-name">Connection name</Label>
                  <Input
                    id="conn-name"
                    placeholder="Production"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                  />
                </div>
                <div className="grid grid-cols-3 gap-3">
                  <div className="col-span-2 grid gap-1.5">
                    <Label htmlFor="conn-host">Host</Label>
                    <Input
                      id="conn-host"
                      placeholder="db.example.com"
                      value={form.host}
                      onChange={(e) => setForm({ ...form, host: e.target.value })}
                    />
                  </div>
                  <div className="grid gap-1.5">
                    <Label htmlFor="conn-port">Port</Label>
                    <Input
                      id="conn-port"
                      value={form.port}
                      onChange={(e) => setForm({ ...form, port: e.target.value })}
                    />
                  </div>
                </div>
                <div className="grid gap-1.5">
                  <Label htmlFor="conn-db">Database name</Label>
                  <Input
                    id="conn-db"
                    placeholder="app_production"
                    value={form.databaseName}
                    onChange={(e) => setForm({ ...form, databaseName: e.target.value })}
                  />
                </div>
                <div className="grid grid-cols-2 gap-3">
                  <div className="grid gap-1.5">
                    <Label htmlFor="conn-user">Username</Label>
                    <Input
                      id="conn-user"
                      value={form.username}
                      onChange={(e) => setForm({ ...form, username: e.target.value })}
                    />
                  </div>
                  <div className="grid gap-1.5">
                    <Label htmlFor="conn-pass">Password</Label>
                    <Input
                      id="conn-pass"
                      type="password"
                      value={form.password}
                      onChange={(e) => setForm({ ...form, password: e.target.value })}
                    />
                  </div>
                </div>
                <div className="grid gap-1.5">
                  <Label>Dialect</Label>
                  <Select value={form.dialect} onValueChange={(v) => setForm({ ...form, dialect: v })}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="POSTGRESQL">PostgreSQL</SelectItem>
                      <SelectItem value="MYSQL">MySQL</SelectItem>
                      <SelectItem value="SQLSERVER">SQL Server</SelectItem>
                      <SelectItem value="ORACLE">Oracle</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="flex items-center justify-between rounded-lg border border-border px-3 py-2.5">
                  <div className="space-y-0.5">
                    <Label className="text-sm">Read-only enforced</Label>
                    <p className="text-xs text-muted-foreground">
                      Blocks EXPLAIN ANALYZE on non-SELECT queries
                    </p>
                  </div>
                  <Switch
                    checked={form.readOnlyEnforced}
                    onCheckedChange={(v) => setForm({ ...form, readOnlyEnforced: v })}
                  />
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsCreateOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleCreate} disabled={isCreating} className="gap-2">
                  {isCreating && <Loader2 className="h-4 w-4 animate-spin" />}
                  Add connection
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      {/* Stat row — real counts, not decoration */}
      <div className="grid grid-cols-3 gap-3">
        <Card className="border-border">
          <CardContent className="flex items-center gap-3 py-4">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10">
              <Database className="h-4 w-4 text-primary" />
            </div>
            <div>
              <p className="text-lg font-semibold leading-none">{connections.length}</p>
              <p className="text-xs text-muted-foreground mt-1">Total connections</p>
            </div>
          </CardContent>
        </Card>
        <Card className="border-border">
          <CardContent className="flex items-center gap-3 py-4">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-emerald-500/10">
              <Activity className="h-4 w-4 text-emerald-500" />
            </div>
            <div>
              <p className="text-lg font-semibold leading-none">{connectedCount}</p>
              <p className="text-xs text-muted-foreground mt-1">Currently connected</p>
            </div>
          </CardContent>
        </Card>
        <Card className="border-border">
          <CardContent className="flex items-center gap-3 py-4">
            <div className="flex h-9 w-9 items-center justify-center rounded-full bg-primary/10">
              <RefreshCw className="h-4 w-4 text-primary" />
            </div>
            <div>
              <p className="text-lg font-semibold leading-none">{syncedCount}</p>
              <p className="text-xs text-muted-foreground mt-1">Schemas synced</p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Project selector */}
      {projects.length > 1 && (
        <div className="flex items-center gap-2">
          <Label className="text-sm text-muted-foreground">Project</Label>
          <Select
            value={projectId?.toString() ?? ""}
            onValueChange={(v) => setProjectId(parseInt(v, 10))}
          >
            <SelectTrigger className="w-56">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {projects.map((p) => (
                <SelectItem key={p.id} value={p.id.toString()}>
                  {p.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      )}

      {/* Connection cards */}
      {isLoading ? (
        <div className="grid gap-3 sm:grid-cols-2">
          {[1, 2].map((i) => (
            <Skeleton key={i} className="h-40 rounded-xl" />
          ))}
        </div>
      ) : connections.length === 0 ? (
        <Card className="border-dashed border-border">
          <CardContent className="flex flex-col items-center gap-3 py-12 text-center">
            <div className="flex h-12 w-12 items-center justify-center rounded-full bg-secondary">
              <CircleSlash className="h-5 w-5 text-muted-foreground" />
            </div>
            <div className="space-y-1">
              <p className="font-medium">No connections yet</p>
              <p className="text-sm text-muted-foreground max-w-sm">
                Add a connection to sync a real schema and run live EXPLAIN plans instead of static
                heuristics.
              </p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-3 sm:grid-cols-2">
          {connections.map((conn) => {
            const status = STATUS_CONFIG[conn.status]
            const isActioning = actioningId === conn.id
            return (
              <Card key={conn.id} className="border-border">
                <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-3">
                  <div className="flex items-center gap-3">
                    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-sm font-semibold text-primary">
                      {conn.name.slice(0, 1).toUpperCase()}
                    </div>
                    <div>
                      <CardTitle className="text-sm">{conn.name}</CardTitle>
                      <CardDescription className="text-xs">
                        {conn.host}:{conn.port}/{conn.databaseName}
                      </CardDescription>
                    </div>
                  </div>
                  <Badge variant={status.badge} className="gap-1.5">
                    <span className={`h-1.5 w-1.5 rounded-full ${status.dot}`} />
                    {status.label}
                  </Badge>
                </CardHeader>
                <CardContent className="space-y-3">
                  <div className="flex flex-wrap gap-2 text-xs text-muted-foreground">
                    <Badge variant="secondary" className="font-normal">
                      {conn.dialect}
                    </Badge>
                    {conn.readOnlyEnforced && (
                      <Badge variant="secondary" className="gap-1 font-normal">
                        <ShieldCheck className="h-3 w-3" />
                        Read-only enforced
                      </Badge>
                    )}
                    {conn.lastSyncedAt && (
                      <span className="self-center">
                        Synced {new Date(conn.lastSyncedAt).toLocaleDateString()}
                      </span>
                    )}
                  </div>
                  {conn.status === "EXPIRED_CREDENTIALS" && (
                    <div className="flex items-start gap-2 rounded-lg bg-amber-500/10 px-2.5 py-2 text-xs text-amber-500">
                      <AlertTriangle className="h-3.5 w-3.5 shrink-0 mt-0.5" />
                      Credentials were rejected — update them and test again.
                    </div>
                  )}
                  <div className="flex items-center gap-2 pt-1">
                    <Button
                      size="sm"
                      variant="outline"
                      className="gap-1.5"
                      disabled={isActioning}
                      onClick={() => handleTest(conn.id)}
                    >
                      {isActioning ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      ) : (
                        <CheckCircle2 className="h-3.5 w-3.5" />
                      )}
                      Test
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      className="gap-1.5"
                      disabled={isActioning}
                      onClick={() => openSyncDialog(conn)}
                    >
                      {isActioning ? (
                        <Loader2 className="h-3.5 w-3.5 animate-spin" />
                      ) : (
                        <RefreshCw className="h-3.5 w-3.5" />
                      )}
                      Sync schema
                    </Button>
                    <AlertDialog>
                      <AlertDialogTrigger asChild>
                        <Button size="sm" variant="ghost" className="ml-auto text-destructive hover:text-destructive">
                          <Trash2 className="h-3.5 w-3.5" />
                        </Button>
                      </AlertDialogTrigger>
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>Remove this connection?</AlertDialogTitle>
                          <AlertDialogDescription>
                            {conn.name} will no longer be usable for live analysis. Schemas already
                            synced from it are kept.
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel>Cancel</AlertDialogCancel>
                          <AlertDialogAction
                            className="bg-destructive text-white hover:bg-destructive/90"
                            onClick={() => handleDelete(conn.id)}
                          >
                            Remove
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                  </div>
                </CardContent>
              </Card>
            )
          })}
        </div>
      )}

      {/* Sync schema target picker */}
      <Dialog open={!!syncDialogConnection} onOpenChange={(open) => !open && setSyncDialogConnection(null)}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Sync schema</DialogTitle>
            <DialogDescription>
              {syncDialogConnection && (
                <>Choose where to write the tables introspected from {syncDialogConnection.name}.</>
              )}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-1.5 py-2">
            <Label>Target schema</Label>
            <Select value={syncTargetSchemaId} onValueChange={setSyncTargetSchemaId}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="new">Create a new schema</SelectItem>
                {schemas.map((schema) => (
                  <SelectItem key={schema.id} value={schema.id.toString()}>
                    {schema.name}
                    {schema.source === "SYNCED" ? " (synced)" : " (manual)"}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="text-xs text-muted-foreground mt-1">
              {syncTargetSchemaId === "new"
                ? "A new schema will be created and marked as synced."
                : "This will replace all tables/columns/indexes currently in that schema with what's actually in the database."}
            </p>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setSyncDialogConnection(null)}>
              Cancel
            </Button>
            <Button onClick={handleSync} disabled={isSyncing} className="gap-2">
              {isSyncing && <Loader2 className="h-4 w-4 animate-spin" />}
              Sync now
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}