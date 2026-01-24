"use client"

import { useTheme } from "next-themes"
import { Textarea } from "@/components/ui/textarea"
import { cn } from "@/lib/utils"

interface SQLEditorProps {
  value: string
  onChange: (value: string) => void
  placeholder?: string
  className?: string
  minHeight?: string
}

export function SQLEditor({
  value,
  onChange,
  placeholder = "Enter your SQL query here...",
  className,
  minHeight = "200px",
}: SQLEditorProps) {
  const { theme } = useTheme()

  return (
    <Textarea
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      className={cn(
        "font-mono text-sm resize-none",
        theme === "dark" ? "bg-muted" : "bg-muted/50",
        className
      )}
      style={{ minHeight }}
    />
  )
}
