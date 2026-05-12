import { Component, type ErrorInfo, type ReactNode } from 'react'

/** Catches render-time errors anywhere below it and shows a recoverable fallback. */
export class ErrorBoundary extends Component<{ children: ReactNode }, { hasError: boolean }> {
  state = { hasError: false }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Unhandled UI error', error, info)
  }

  render() {
    if (!this.state.hasError) return this.props.children
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-3 bg-slate-50 px-4 text-center text-slate-800">
        <h1 className="text-2xl font-semibold">Something went wrong</h1>
        <p className="text-sm text-slate-500">An unexpected error occurred. Please reload the page.</p>
        <button
          type="button"
          onClick={() => window.location.reload()}
          className="rounded-md bg-slate-800 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
        >
          Reload
        </button>
      </div>
    )
  }
}
