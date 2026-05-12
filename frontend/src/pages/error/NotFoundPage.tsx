import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-50 text-slate-800">
      <h1 className="text-3xl font-semibold">404</h1>
      <p className="text-slate-500">This page does not exist.</p>
      <Link to="/" className="text-sm font-medium text-slate-700 underline">
        Go to home
      </Link>
    </div>
  )
}
