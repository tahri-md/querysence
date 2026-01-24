"use client"

import { useEffect, useState } from "react"
import { ChevronRight, Database, FolderPlus, Key, Loader2, Plus, Table2, Trash2, Edit2, X } from "lucide-react"
import { toast } from "sonner"

import { Button } from "@/components/ui/button"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
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
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Skeleton } from "@/components/ui/skeleton"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Checkbox } from "@/components/ui/checkbox"
import { projectsApi, schemasApi, type Schema, type TableDefinition, type ColumnDefinition, type IndexDefinition } from "@/lib/api"

interface Project {
  id: number
  name: string
  description: string
  schemaCount: number
  schemas?: Schema[]
}

export default function SchemasPage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [expandedProjects, setExpandedProjects] = useState<Set<number>>(new Set())
  const [selectedSchema, setSelectedSchema] = useState<Schema | null>(null)

  const [isCreateProjectOpen, setIsCreateProjectOpen] = useState(false)
  const [newProjectName, setNewProjectName] = useState("")
  const [newProjectDescription, setNewProjectDescription] = useState("")
  const [isCreatingProject, setIsCreatingProject] = useState(false)

  const [isCreateSchemaOpen, setIsCreateSchemaOpen] = useState(false)
  const [schemaProjectId, setSchemaProjectId] = useState<number | null>(null)
  const [newSchemaName, setNewSchemaName] = useState("")
  const [newSchemaDialect, setNewSchemaDialect] = useState("POSTGRESQL")
  const [newSchemaDdl, setNewSchemaDdl] = useState("")
  const [isCreatingSchema, setIsCreatingSchema] = useState(false)

  const [isAddTableOpen, setIsAddTableOpen] = useState(false)
  const [selectedSchemaForTable, setSelectedSchemaForTable] = useState<number | null>(null)
  const [newTableName, setNewTableName] = useState("")
  const [newColumns, setNewColumns] = useState<ColumnDefinition[]>([
    { columnName: "id", dataType: "INTEGER", isNullable: false, isPrimaryKey: true, isForeignKey: false },
  ])
  const [newIndexes, setNewIndexes] = useState<IndexDefinition[]>([])
  const [isAddingTable, setIsAddingTable] = useState(false)

  const [isAddColumnOpen, setIsAddColumnOpen] = useState(false)
  const [selectedTableForColumn, setSelectedTableForColumn] = useState<{ schemaId: number; tableName: string } | null>(null)
  const [newColumnName, setNewColumnName] = useState("")
  const [newColumnType, setNewColumnType] = useState("VARCHAR(255)")
  const [newColumnNullable, setNewColumnNullable] = useState(true)
  const [newColumnPk, setNewColumnPk] = useState(false)
  const [newColumnFk, setNewColumnFk] = useState(false)
  const [isAddingColumn, setIsAddingColumn] = useState(false)

  const [isAddIndexOpen, setIsAddIndexOpen] = useState(false)
  const [selectedTableForIndex, setSelectedTableForIndex] = useState<{ schemaId: number; tableName: string } | null>(null)
  const [newIndexName, setNewIndexName] = useState("")
  const [newIndexColumns, setNewIndexColumns] = useState<string[]>([])
  const [newIndexUnique, setNewIndexUnique] = useState(false)
  const [newIndexType, setNewIndexType] = useState("BTREE")
  const [isAddingIndex, setIsAddingIndex] = useState(false)

  const fetchProjects = async () => {
    try {
      const data = await projectsApi.list()
      setProjects(data)
    } catch (error) {
      console.log("Error fetching projects:", error)
      toast.error("Failed to load projects")
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    fetchProjects()
  }, [])

  const toggleProject = async (projectId: number) => {
    const newExpanded = new Set(expandedProjects)
    
    if (newExpanded.has(projectId)) {
      newExpanded.delete(projectId)
    } else {
      newExpanded.add(projectId)
      const project = projects.find((p) => p.id === projectId)
      if (project && !project.schemas) {
        try {
          const projectData = await projectsApi.get(projectId)
          setProjects((prev) =>
            prev.map((p) =>
              p.id === projectId ? { ...p, schemas: projectData.schemas } : p
            )
          )
        } catch (error) {
          console.log("Error fetching project:", error)
        }
      }
    }
    
    setExpandedProjects(newExpanded)
  }

  const handleCreateProject = async () => {
    if (!newProjectName.trim()) {
      toast.error("Project name is required")
      return
    }

    setIsCreatingProject(true)
    try {
      await projectsApi.create(newProjectName, newProjectDescription)
      toast.success("Project created successfully")
      setIsCreateProjectOpen(false)
      setNewProjectName("")
      setNewProjectDescription("")
      fetchProjects()
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to create project")
    } finally {
      setIsCreatingProject(false)
    }
  }

  const handleCreateSchema = async () => {
    if (!newSchemaName.trim()) {
      toast.error("Schema name is required")
      return
    }
    if (!schemaProjectId) {
      toast.error("Please select a project")
      return
    }

    setIsCreatingSchema(true)
    try {
      await projectsApi.createSchema(
        schemaProjectId,
        newSchemaName,
        newSchemaDialect,
        newSchemaDdl || undefined
      )
      toast.success("Schema created successfully")
      setIsCreateSchemaOpen(false)
      setNewSchemaName("")
      setNewSchemaDdl("")
      
      const projectData = await projectsApi.get(schemaProjectId)
      setProjects((prev) =>
        prev.map((p) =>
          p.id === schemaProjectId
            ? { ...p, schemas: projectData.schemas, schemaCount: projectData.schemas.length }
            : p
        )
      )
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to create schema")
    } finally {
      setIsCreatingSchema(false)
    }
  }

  const handleDeleteProject = async (projectId: number) => {
    try {
      await projectsApi.delete(projectId)
      toast.success("Project deleted")
      setProjects((prev) => prev.filter((p) => p.id !== projectId))
      if (selectedSchema && projects.find((p) => p.id === projectId)?.schemas?.some((s) => s.id === selectedSchema.id)) {
        setSelectedSchema(null)
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to delete project")
    }
  }

  const handleDeleteSchema = async (schemaId: number, projectId: number) => {
    try {
      await schemasApi.delete(schemaId)
      toast.success("Schema deleted")
      
      if (selectedSchema?.id === schemaId) {
        setSelectedSchema(null)
      }
      
      setProjects((prev) =>
        prev.map((p) =>
          p.id === projectId
            ? {
                ...p,
                schemas: p.schemas?.filter((s) => s.id !== schemaId),
                schemaCount: (p.schemaCount || 1) - 1,
              }
            : p
        )
      )
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to delete schema")
    }
  }

  const handleAddTable = async () => {
    if (!newTableName.trim() || !selectedSchemaForTable) {
      toast.error("Table name and schema are required")
      return
    }

    setIsAddingTable(true)
    try {
      await schemasApi.addTable(selectedSchemaForTable, newTableName, newColumns, newIndexes)
      toast.success("Table added successfully")
      setIsAddTableOpen(false)
      setNewTableName("")
      setNewColumns([
        { columnName: "id", dataType: "INTEGER", isNullable: false, isPrimaryKey: true, isForeignKey: false },
      ])
      setNewIndexes([])

      const schema = await schemasApi.get(selectedSchemaForTable)
      if (selectedSchema?.id === selectedSchemaForTable) {
        setSelectedSchema(schema)
      }
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to add table")
    } finally {
      setIsAddingTable(false)
    }
  }

  const handleAddColumn = async () => {
    if (!newColumnName.trim() || !selectedTableForColumn) {
      toast.error("Column name is required")
      return
    }

    toast.success("Column settings prepared - implement in backend")
    setIsAddColumnOpen(false)
    setNewColumnName("")
    setNewColumnType("VARCHAR(255)")
  }

  const handleAddIndex = async () => {
    if (!newIndexName.trim() || !selectedTableForIndex || newIndexColumns.length === 0) {
      toast.error("Index name and columns are required")
      return
    }

    toast.success("Index settings prepared - implement in backend")
    setIsAddIndexOpen(false)
    setNewIndexName("")
    setNewIndexColumns([])
  }

  const addColumnToTable = () => {
    setNewColumns([
      ...newColumns,
      { columnName: "", dataType: "VARCHAR(255)", isNullable: true, isPrimaryKey: false, isForeignKey: false },
    ])
  }

  const removeColumnFromTable = (index: number) => {
    setNewColumns(newColumns.filter((_, i) => i !== index))
  }

  const updateColumn = (index: number, field: string, value: unknown) => {
    const updated = [...newColumns]
    updated[index] = { ...updated[index], [field]: value }
    setNewColumns(updated)
  }

  const addIndexToTable = () => {
    setNewIndexes([
      ...newIndexes,
      { indexName: "", columns: [], isUnique: false, indexType: "BTREE" },
    ])
  }

  const removeIndexFromTable = (index: number) => {
    setNewIndexes(newIndexes.filter((_, i) => i !== index))
  }

  const updateIndex = (index: number, field: string, value: unknown) => {
    const updated = [...newIndexes]
    updated[index] = { ...updated[index], [field]: value }
    setNewIndexes(updated)
  }

  const viewSchemaDetails = async (schemaId: number) => {
    try {
      const schema = await schemasApi.get(schemaId)
      setSelectedSchema(schema)
    } catch (error) {
      toast.error(error instanceof Error ? error.message : "Failed to load schema")
    }
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <Skeleton className="h-8 w-48" />
          <Skeleton className="h-10 w-32" />
        </div>
        <div className="grid gap-4 lg:grid-cols-3">
          <Skeleton className="h-96" />
          <Skeleton className="h-96 lg:col-span-2" />
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6 px-4 sm:px-0">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold tracking-tight">Schema Registry</h1>
          <p className="text-xs sm:text-sm text-muted-foreground">
            Manage your database schemas and table definitions
          </p>
        </div>
        <div className="flex flex-col sm:flex-row gap-2 w-full sm:w-auto">
          <Dialog open={isAddTableOpen} onOpenChange={setIsAddTableOpen}>
            <DialogTrigger asChild>
              <Button variant="outline" className="w-full sm:w-auto justify-center sm:justify-start bg-transparent">
                <Table2 className="h-4 w-4 mr-2" />
                New Table
              </Button>
            </DialogTrigger>
            <DialogContent className="max-h-[90vh] overflow-y-auto max-w-2xl w-[95vw] sm:w-auto">
              <DialogHeader>
                <DialogTitle className="text-lg sm:text-xl">Create Table</DialogTitle>
                <DialogDescription className="text-xs sm:text-sm">
                  Add a new table with columns and indexes
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label>Schema</Label>
                  <Select
                    value={selectedSchemaForTable?.toString() || ""}
                    onValueChange={(v) => setSelectedSchemaForTable(parseInt(v))}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select schema" />
                    </SelectTrigger>
                    <SelectContent>
                      {projects.flatMap((p) =>
                        p.schemas?.map((s) => (
                          <SelectItem key={s.id} value={String(s.id)}>
                            {p.name} / {s.name}
                          </SelectItem>
                        )) || []
                      )}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Table Name</Label>
                  <Input
                    placeholder="e.g., users, products"
                    value={newTableName}
                    onChange={(e) => setNewTableName(e.target.value)}
                  />
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <Label>Columns</Label>
                    <Button size="sm" variant="outline" onClick={addColumnToTable}>
                      <Plus className="h-3 w-3 mr-1" />
                      Add Column
                    </Button>
                  </div>
                  <div className="space-y-3 max-h-[200px] overflow-y-auto">
                    {newColumns.map((col, idx) => (
                      <div key={idx} className="flex flex-col sm:flex-row sm:gap-2 sm:items-end gap-2">
                        <Input
                          placeholder="Column name"
                          value={col.columnName}
                          onChange={(e) => updateColumn(idx, "columnName", e.target.value)}
                          className="flex-1 text-xs sm:text-sm"
                        />
                        <Select
                          value={col.dataType}
                          onValueChange={(v) => updateColumn(idx, "dataType", v)}
                        >
                          <SelectTrigger className="w-full sm:w-28">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="INTEGER">INTEGER</SelectItem>
                            <SelectItem value="VARCHAR(255)">VARCHAR(255)</SelectItem>
                            <SelectItem value="TEXT">TEXT</SelectItem>
                            <SelectItem value="BOOLEAN">BOOLEAN</SelectItem>
                            <SelectItem value="TIMESTAMP">TIMESTAMP</SelectItem>
                            <SelectItem value="DECIMAL(10,2)">DECIMAL</SelectItem>
                          </SelectContent>
                        </Select>
                        <div className="flex gap-1">
                          <Checkbox
                            checked={col.isPrimaryKey}
                            onCheckedChange={(v) => updateColumn(idx, "isPrimaryKey", v)}
                            title="Primary Key"
                          />
                          <Checkbox
                            checked={!col.isNullable}
                            onCheckedChange={(v) => updateColumn(idx, "isNullable", !v)}
                            title="Not Null"
                          />
                        </div>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => removeColumnFromTable(idx)}
                          className="w-full sm:w-auto"
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <Label>Indexes</Label>
                    <Button size="sm" variant="outline" onClick={addIndexToTable}>
                      <Plus className="h-3 w-3 mr-1" />
                      Add Index
                    </Button>
                  </div>
                  <div className="space-y-3 max-h-[150px] overflow-y-auto">
                    {newIndexes.map((idx, i) => (
                      <div key={i} className="flex flex-col sm:flex-row sm:gap-2 sm:items-end gap-2">
                        <Input
                          placeholder="Index name"
                          value={idx.indexName}
                          onChange={(e) => updateIndex(i, "indexName", e.target.value)}
                          className="flex-1 text-xs sm:text-sm"
                        />
                        <Select
                          value={idx.indexType}
                          onValueChange={(v) => updateIndex(i, "indexType", v)}
                        >
                          <SelectTrigger className="w-full sm:w-24">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="BTREE">BTREE</SelectItem>
                            <SelectItem value="HASH">HASH</SelectItem>
                          </SelectContent>
                        </Select>
                        <div className="flex gap-1">
                          <Checkbox
                            checked={idx.isUnique}
                            onCheckedChange={(v) => updateIndex(i, "isUnique", v)}
                            title="Unique"
                          />
                        </div>
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={() => removeIndexFromTable(i)}
                          className="w-full sm:w-auto"
                        >
                          <X className="h-4 w-4" />
                        </Button>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsAddTableOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleAddTable} disabled={isAddingTable}>
                  {isAddingTable && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                  Create Table
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>

          <Dialog open={isCreateSchemaOpen} onOpenChange={setIsCreateSchemaOpen}>
            <DialogTrigger asChild>
              <Button className="w-full sm:w-auto justify-center sm:justify-start">
                <FolderPlus className="h-4 w-4 mr-2" />
                New Schema
              </Button>
            </DialogTrigger>
            <DialogContent className="w-[95vw] sm:w-auto">
              <DialogHeader>
                <DialogTitle className="text-lg sm:text-xl">Create Schema</DialogTitle>
                <DialogDescription className="text-xs sm:text-sm">
                  Add a new database schema to your project
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label>Project</Label>
                  <Select
                    value={schemaProjectId?.toString() || ""}
                    onValueChange={(v) => setSchemaProjectId(parseInt(v))}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Select project" />
                    </SelectTrigger>
                    <SelectContent>
                      {projects.map((project) => (
                        <SelectItem key={project.id} value={String(project.id)}>
                          {project.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>Schema Name</Label>
                  <Input
                    placeholder="e.g., production, staging"
                    value={newSchemaName}
                    onChange={(e) => setNewSchemaName(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Dialect</Label>
                  <Select value={newSchemaDialect} onValueChange={setNewSchemaDialect}>
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="POSTGRESQL">PostgreSQL</SelectItem>
                      <SelectItem value="ORACLE">Oracle</SelectItem>
                      <SelectItem value="MYSQL">MySQL</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>DDL Script (optional)</Label>
                  <Textarea
                    placeholder="CREATE TABLE users (...);"
                    value={newSchemaDdl}
                    onChange={(e) => setNewSchemaDdl(e.target.value)}
                    className="font-mono text-sm min-h-[120px]"
                  />
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsCreateSchemaOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleCreateSchema} disabled={isCreatingSchema}>
                  {isCreatingSchema && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                  Create Schema
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>

          <Dialog open={isCreateProjectOpen} onOpenChange={setIsCreateProjectOpen}>
            <DialogTrigger asChild>
              <Button className="w-full sm:w-auto justify-center sm:justify-start">
                <FolderPlus className="h-4 w-4 mr-2" />
                New Project
              </Button>
            </DialogTrigger>
            <DialogContent className="w-[95vw] sm:w-auto">
              <DialogHeader>
                <DialogTitle className="text-lg sm:text-xl">Create Project</DialogTitle>
                <DialogDescription className="text-xs sm:text-sm">
                  Create a new project to organize your database schemas.
                </DialogDescription>
              </DialogHeader>
              <div className="space-y-4 py-4">
                <div className="space-y-2">
                  <Label>Project Name</Label>
                  <Input
                    placeholder="e.g., E-Commerce App"
                    value={newProjectName}
                    onChange={(e) => setNewProjectName(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Description (optional)</Label>
                  <Textarea
                    placeholder="Brief description of the project"
                    value={newProjectDescription}
                    onChange={(e) => setNewProjectDescription(e.target.value)}
                  />
                </div>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => setIsCreateProjectOpen(false)}>
                  Cancel
                </Button>
                <Button onClick={handleCreateProject} disabled={isCreatingProject}>
                  {isCreatingProject && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                  Create Project
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>
      </div>

      <div className="grid gap-6 grid-cols-1 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <CardHeader>
            <CardTitle className="text-lg sm:text-xl">Projects</CardTitle>
            <CardDescription className="text-xs sm:text-sm">
              {projects.length} project{projects.length !== 1 ? "s" : ""}
            </CardDescription>
          </CardHeader>
          <CardContent>
            <ScrollArea className="h-[300px] sm:h-[500px] pr-4">
              {projects.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-8 text-center">
                  <Database className="h-12 w-12 text-muted-foreground mb-4" />
                  <p className="text-sm text-muted-foreground">No projects yet</p>
                  <p className="text-xs text-muted-foreground">Create a project to get started</p>
                </div>
              ) : (
                <div className="space-y-2">
                  {projects.map((project) => (
                    <Collapsible
                      key={project.id}
                      open={expandedProjects.has(project.id)}
                      onOpenChange={() => toggleProject(project.id)}
                    >
                      <div className="rounded-lg border">
                        <CollapsibleTrigger asChild>
                          <div className="flex items-center justify-between p-3 cursor-pointer hover:bg-muted/50 min-h-10">
                            <div className="flex items-center gap-2 min-w-0 flex-1">
                              <ChevronRight
                                className={`h-4 w-4 transition-transform flex-shrink-0 ${
                                  expandedProjects.has(project.id) ? "rotate-90" : ""
                                }`}
                              />
                              <span className="font-medium text-sm truncate">{project.name}</span>
                            </div>
                            <div className="flex items-center gap-1 sm:gap-2 flex-shrink-0 ml-2">
                              <Badge variant="secondary" className="text-xs">
                                {project.schemaCount}
                              </Badge>
                              <AlertDialog>
                                <AlertDialogTrigger asChild>
                                  <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-7 w-7"
                                    onClick={(e) => e.stopPropagation()}
                                  >
                                    <Trash2 className="h-3 w-3" />
                                  </Button>
                                </AlertDialogTrigger>
                                <AlertDialogContent>
                                  <AlertDialogHeader>
                                    <AlertDialogTitle>Delete Project</AlertDialogTitle>
                                    <AlertDialogDescription>
                                      This will permanently delete the project and all its schemas. This action cannot be undone.
                                    </AlertDialogDescription>
                                  </AlertDialogHeader>
                                  <AlertDialogFooter>
                                    <AlertDialogCancel>Cancel</AlertDialogCancel>
                                    <AlertDialogAction onClick={() => handleDeleteProject(project.id)}>
                                      Delete
                                    </AlertDialogAction>
                                  </AlertDialogFooter>
                                </AlertDialogContent>
                              </AlertDialog>
                            </div>
                          </div>
                        </CollapsibleTrigger>
                        <CollapsibleContent>
                          <div className="border-t px-3 py-2 space-y-1">
                            {project.schemas?.map((schema) => (
                              <div
                                key={schema.id}
                                className={`flex items-center justify-between rounded p-2 cursor-pointer hover:bg-muted/50 ${
                                  selectedSchema?.id === schema.id ? "bg-muted" : ""
                                }`}
                                onClick={() => viewSchemaDetails(schema.id)}
                              >
                                <div className="flex items-center gap-2">
                                  <Database className="h-4 sm:h-5 w-4 sm:w-5 flex-shrink-0" />
                                  <span className="truncate">{schema.name}</span>
                                </div>
                                <div className="flex items-center gap-1">
                                  <Badge variant="outline" className="text-xs">
                                    {schema.dialect}
                                  </Badge>
                                  <AlertDialog>
                                    <AlertDialogTrigger asChild>
                                      <Button
                                        variant="ghost"
                                        size="icon"
                                        className="h-6 w-6"
                                        onClick={(e) => e.stopPropagation()}
                                      >
                                        <Trash2 className="h-3 w-3" />
                                      </Button>
                                    </AlertDialogTrigger>
                                    <AlertDialogContent>
                                      <AlertDialogHeader>
                                        <AlertDialogTitle>Delete Schema</AlertDialogTitle>
                                        <AlertDialogDescription>
                                          This will permanently delete the schema and all its tables. This action cannot be undone.
                                        </AlertDialogDescription>
                                      </AlertDialogHeader>
                                      <AlertDialogFooter>
                                        <AlertDialogCancel>Cancel</AlertDialogCancel>
                                        <AlertDialogAction
                                          onClick={() => handleDeleteSchema(schema.id, project.id)}
                                        >
                                          Delete
                                        </AlertDialogAction>
                                      </AlertDialogFooter>
                                    </AlertDialogContent>
                                  </AlertDialog>
                                </div>
                              </div>
                            )) || (
                              <div className="flex items-center justify-center py-4">
                                <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                              </div>
                            )}
                            {project.schemas?.length === 0 && (
                              <p className="text-xs text-muted-foreground text-center py-2">
                                No schemas in this project
                              </p>
                            )}
                          </div>
                        </CollapsibleContent>
                      </div>
                    </Collapsible>
                  ))}
                </div>
              )}
            </ScrollArea>
          </CardContent>
        </Card>

        {selectedSchema ? (
          <Card className="lg:col-span-2">
            <CardHeader>
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <CardTitle className="flex items-center gap-2 text-lg sm:text-xl truncate">
                    <Database className="h-4 sm:h-5 w-4 sm:w-5 flex-shrink-0" />
                    <span className="truncate">{selectedSchema.name}</span>
                  </CardTitle>
                  <CardDescription className="text-xs sm:text-sm">{selectedSchema.dialect}</CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <ScrollArea className="h-[300px] sm:h-[500px] pr-4">
                {selectedSchema.tables?.map((table) => (
                  <TableCard key={table.tableName} table={table} />
                ))}
              </ScrollArea>
            </CardContent>
          </Card>
        ) : (
          <div className="flex flex-col items-center justify-center py-16 text-center">
            <Database className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-lg font-medium">Select a schema</p>
            <p className="text-sm text-muted-foreground">
              Click on a schema to view its tables and columns
            </p>
          </div>
        )}
      </div>
    </div>
  )
}

function TableCard({ table }: { table: TableDefinition }) {
  const [isExpanded, setIsExpanded] = useState(false)

  return (
    <Collapsible open={isExpanded} onOpenChange={setIsExpanded}>
      <div className="rounded-lg border">
        <CollapsibleTrigger asChild>
          <div className="flex items-center justify-between p-3 sm:p-4 cursor-pointer hover:bg-muted/50 min-h-12">
            <div className="flex items-center gap-2 sm:gap-3 flex-1 min-w-0">
              <Table2 className="h-4 sm:h-5 w-4 sm:w-5 text-muted-foreground flex-shrink-0" />
              <div className="min-w-0 flex-1">
                <p className="font-mono font-medium text-xs sm:text-sm truncate">{table.tableName}</p>
                <p className="text-xs text-muted-foreground">
                  {table.columns.length}Col
                  {table.indexes?.length ? ` - ${table.indexes.length}Idx` : ""}
                  {table.estimatedRows > 0 && ` - ${table.estimatedRows.toLocaleString()}r`}
                </p>
              </div>
            </div>
            <ChevronRight
              className={`h-4 sm:h-5 w-4 sm:w-5 text-muted-foreground transition-transform flex-shrink-0 ${
                isExpanded ? "rotate-90" : ""
              }`}
            />
          </div>
        </CollapsibleTrigger>
        <CollapsibleContent>
          <div className="border-t px-4 py-3 space-y-4">
            <div>
              <div className="flex items-center justify-between mb-3">
                <h4 className="font-semibold text-xs sm:text-sm">Columns ({table.columns.length})</h4>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full text-xs sm:text-sm">
                  <thead>
                    <tr className="border-b">
                      <th className="py-2 px-1 text-left font-medium">Column</th>
                      <th className="py-2 px-1 text-left font-medium">Type</th>
                      <th className="py-2 px-1 text-left font-medium">Constraints</th>
                    </tr>
                  </thead>
                  <tbody>
                    {table.columns.map((column) => (
                      <tr key={column.columnName} className="border-b last:border-0">
                        <td className="py-2 px-1 font-mono text-xs truncate">{column.columnName}</td>
                        <td className="py-2 px-1 font-mono text-xs text-muted-foreground truncate">
                          {column.dataType}
                        </td>
                        <td className="py-2 px-1">
                          <div className="flex gap-1 flex-wrap">
                            {column.isPrimaryKey && (
                              <Badge variant="default" className="text-xs">
                                <Key className="h-2 w-2 mr-1" />
                                PK
                              </Badge>
                            )}
                            {column.isForeignKey && (
                              <Badge variant="secondary" className="text-xs">
                                FK
                              </Badge>
                            )}
                            {!column.isNullable && (
                              <Badge variant="outline" className="text-xs">
                                NOT NULL
                              </Badge>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {table.indexes && table.indexes.length > 0 && (
              <div>
                <div className="flex items-center justify-between mb-3">
                  <h4 className="font-semibold text-xs sm:text-sm">Indexes ({table.indexes.length})</h4>
                </div>
                <div className="overflow-x-auto">
                  <table className="w-full text-xs sm:text-sm">
                    <thead>
                      <tr className="border-b">
                        <th className="py-2 px-1 text-left font-medium">Index</th>
                        <th className="py-2 px-1 text-left font-medium">Columns</th>
                        <th className="py-2 px-1 text-left font-medium">Type</th>
                        <th className="py-2 px-1 text-left font-medium">Unique</th>
                      </tr>
                    </thead>
                    <tbody>
                      {table.indexes.map((index) => (
                        <tr key={index.indexName} className="border-b last:border-0">
                          <td className="py-2 px-1 font-mono text-xs truncate">{index.indexName}</td>
                          <td className="py-2 px-1 font-mono text-xs text-muted-foreground truncate">
                            {index.columns.join(",")}
                          </td>
                          <td className="py-2 px-1 text-xs">
                            <Badge variant="outline" className="text-xs">{index.indexType}</Badge>
                          </td>
                          <td className="py-2 px-1 text-xs">
                            {index.isUnique ? (
                              <Badge variant="secondary" className="text-xs">YES</Badge>
                            ) : (
                              <span className="text-muted-foreground">No</span>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        </CollapsibleContent>
      </div>
    </Collapsible>
  )
}
