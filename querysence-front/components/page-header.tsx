import type { ReactNode } from "react"

interface PageHeaderProps {
  title: string
  description?: string
  /** Actions rendered on the right side of the header, e.g. a primary button */
  actions?: ReactNode
}

/**
 * Consistent title/description block used at the top of every dashboard page.
 * Keeps typography, spacing, and the title/actions layout identical across pages.
 */
export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div className="space-y-1">
        <h1 className="text-2xl sm:text-3xl font-black tracking-tight">{title}</h1>
        {description && (
          <p className="text-sm sm:text-base text-muted-foreground">{description}</p>
        )}
      </div>
      {actions && <div className="flex flex-wrap items-center gap-2">{actions}</div>}
    </div>
  )
}
