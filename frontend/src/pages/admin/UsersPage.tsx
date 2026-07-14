import { useState } from 'react'
import { TechniciansTab } from './TechniciansTab'
import { ClientsTab } from './ClientsTab'

type Tab = 'technicians' | 'clients'

export function UsersPage() {
  const [tab, setTab] = useState<Tab>('technicians')

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Users</h1>

      <div className="flex gap-1 border-b border-slate-200">
        <TabButton label="Technicians" active={tab === 'technicians'} onClick={() => setTab('technicians')} />
        <TabButton label="Clients" active={tab === 'clients'} onClick={() => setTab('clients')} />
      </div>

      {tab === 'technicians' ? <TechniciansTab /> : <ClientsTab />}
    </div>
  )
}

function TabButton({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  const classes = active
    ? 'border-slate-800 text-slate-900'
    : 'border-transparent text-slate-500 hover:text-slate-700'
  return (
    <button type="button" onClick={onClick} className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium ${classes}`}>
      {label}
    </button>
  )
}
