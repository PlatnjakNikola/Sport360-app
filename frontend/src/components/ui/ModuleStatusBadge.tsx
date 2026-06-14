const COLORS: Record<string, string> = {
  waiting_for_repair: 'bg-amber-50 text-amber-700',
  repaired: 'bg-green-50 text-green-700',
  not_repairable: 'bg-red-50 text-red-700',
}

export function ModuleStatusBadge({ code, label }: { code: string; label: string }) {
  const classes = COLORS[code] ?? 'bg-slate-100 text-slate-600'
  return <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${classes}`}>{label}</span>
}
