export function StatusBadge({ active }: { active: boolean }) {
  const classes = active ? 'bg-emerald-50 text-emerald-700' : 'bg-slate-100 text-slate-500'
  return <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${classes}`}>{active ? 'Active' : 'Inactive'}</span>
}
