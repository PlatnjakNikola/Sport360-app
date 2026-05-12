export function LoadingSpinner() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-50">
      <div className="h-8 w-8 animate-spin rounded-full border-2 border-slate-300 border-t-slate-600" />
    </div>
  )
}
