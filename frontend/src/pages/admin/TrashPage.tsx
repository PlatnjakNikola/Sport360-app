import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import toast from 'react-hot-toast'
import { adminApi } from '../../api/admin'
import { getErrorMessage } from '../../api/client'
import type { TrashItem } from '../../types/admin'
import { formatDateTime } from '../../utils/formatDate'
import { secondaryButtonClass } from '../../components/ui/formStyles'

export function TrashPage() {
  const queryClient = useQueryClient()
  const [filter, setFilter] = useState<'all' | 'package' | 'module'>('all')

  const { data: items, isLoading } = useQuery({ queryKey: ['admin-trash'], queryFn: adminApi.trash })

  const restore = useMutation({
    mutationFn: async (item: TrashItem) => {
      if (item.entityType === 'package') await adminApi.restorePackage(item.id)
      else await adminApi.restoreModule(item.id)
    },
    onSuccess: () => {
      toast.success('Restored')
      queryClient.invalidateQueries({ queryKey: ['admin-trash'] })
      queryClient.invalidateQueries({ queryKey: ['admin-packages'] })
    },
    onError: (e) => toast.error(getErrorMessage(e)),
  })

  const visible = (items ?? []).filter((i) => filter === 'all' || i.entityType === filter)

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-semibold">Trash</h1>
      <div className="flex gap-2 text-sm">
        {(['all', 'package', 'module'] as const).map((f) => (
          <button
            key={f}
            type="button"
            onClick={() => setFilter(f)}
            className={`rounded-md px-3 py-1.5 ${filter === f ? 'bg-slate-800 text-white' : 'border border-slate-300 text-slate-600'}`}
          >
            {f === 'all' ? 'All' : f === 'package' ? 'Packages' : 'Modules'}
          </button>
        ))}
      </div>

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <div className="overflow-x-auto"><table className="w-full min-w-[640px] text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Type</th>
              <th className="px-4 py-2 font-medium">Number</th>
              <th className="px-4 py-2 font-medium">Deleted at</th>
              <th className="px-4 py-2 text-right font-medium">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {isLoading && <tr><td colSpan={4} className="px-4 py-6 text-center text-slate-400">Loading…</td></tr>}
            {!isLoading && visible.length === 0 && <tr><td colSpan={4} className="px-4 py-6 text-center text-slate-400">Trash is empty.</td></tr>}
            {visible.map((item) => (
              <tr key={`${item.entityType}-${item.id}`}>
                <td className="px-4 py-2 capitalize">{item.entityType}</td>
                <td className="px-4 py-2 font-medium">
                  {item.entityType === 'package' ? (
                    <Link to={`/admin/packages/${item.id}`} className="hover:underline">{item.label}</Link>
                  ) : (
                    <Link to={`/admin/modules/${item.id}`} className="hover:underline">{item.label}</Link>
                  )}
                </td>
                <td className="px-4 py-2 text-slate-500">{item.deletedAt ? formatDateTime(item.deletedAt) : '—'}</td>
                <td className="px-4 py-2 text-right">
                  <button type="button" className={secondaryButtonClass} disabled={restore.isPending} onClick={() => restore.mutate(item)}>
                    Restore
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </section>
    </div>
  )
}
