const COLORS: Record<string, string> = {
  created: 'bg-slate-100 text-slate-600',
  sent_to_service: 'bg-blue-50 text-blue-700',
  received_by_service: 'bg-indigo-50 text-indigo-700',
  on_service: 'bg-amber-50 text-amber-700',
  repaired_waiting_shipment: 'bg-green-50 text-green-700',
  shipped_to_client: 'bg-purple-50 text-purple-700',
  arrived: 'bg-emerald-50 text-emerald-700',
}

export function PackageStatusBadge({ code, label }: { code: string; label: string }) {
  const classes = COLORS[code] ?? 'bg-slate-100 text-slate-600'
  return <span className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${classes}`}>{label}</span>
}
