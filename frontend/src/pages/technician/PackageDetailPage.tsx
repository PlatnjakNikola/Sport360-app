import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import toast from 'react-hot-toast'
import { QrCode } from 'lucide-react'
import { technicianApi } from '../../api/technician'
import { getErrorMessage } from '../../api/client'
import { PackageStatusBadge } from '../../components/ui/PackageStatusBadge'
import { ModuleStatusBadge } from '../../components/ui/ModuleStatusBadge'
import { Timeline } from '../../components/ui/Timeline'
import { ConfirmModal } from '../../components/ui/ConfirmModal'
import { Modal } from '../../components/ui/Modal'
import { QrScanModal } from '../../components/ui/QrScanModal'
import { technicianAction } from '../../utils/technicianStatus'
import { formatDateTime } from '../../utils/formatDate'
import { IMAGE_ACCEPT_ATTR, validateImageFiles } from '../../utils/imageFiles'
import { buttonClass, inputClass, labelClass, primaryButtonClass, secondaryButtonClass } from '../../components/ui/formStyles'

export function TechnicianPackageDetailPage() {
  const { id } = useParams<{ id: string }>()
  const packageId = Number(id)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [shipOpen, setShipOpen] = useState(false)
  const [finishOpen, setFinishOpen] = useState(false)
  const [arrivalOpen, setArrivalOpen] = useState(false)
  const [scanOpen, setScanOpen] = useState(false)
  const [findInput, setFindInput] = useState('')
  const [createPrompt, setCreatePrompt] = useState<string | null>(null)
  const [createFor, setCreateFor] = useState<string | null>(null)

  const { data: pkg, isLoading, isError } = useQuery({
    queryKey: ['tech-package', packageId],
    queryFn: () => technicianApi.packageDetail(packageId),
  })
  const { data: modules } = useQuery({
    queryKey: ['tech-modules', packageId],
    queryFn: () => technicianApi.listModules(packageId),
    enabled: !!pkg,
  })

  const refresh = () => {
    queryClient.invalidateQueries({ queryKey: ['tech-package', packageId] })
    queryClient.invalidateQueries({ queryKey: ['tech-modules', packageId] })
    queryClient.invalidateQueries({ queryKey: ['tech-packages'] })
  }

  const nextStatus = useMutation({
    mutationFn: (returnTrackingLink?: string) => technicianApi.nextStatus(packageId, returnTrackingLink),
    onSuccess: () => {
      toast.success('Status updated')
      refresh()
      setShipOpen(false)
      setFinishOpen(false)
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const confirmArrival = useMutation({
    mutationFn: () => technicianApi.confirmArrivalInternal(packageId),
    onSuccess: () => {
      toast.success('Arrival confirmed')
      refresh()
      setArrivalOpen(false)
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  // Jump straight to a module by typed/scanned number; offer to create a request if none exists.
  const find = useMutation({
    mutationFn: (number: string) => technicianApi.findModule(packageId, number),
    onSuccess: (module, number) => {
      if (module) {
        navigate(
          module.statusCode === 'waiting_for_repair'
            ? `/technician/modules/${module.id}/repair`
            : `/technician/modules/${module.id}`,
        )
      } else {
        setCreatePrompt(number)
      }
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const submitFind = (value: string) => {
    const number = value.trim()
    if (number) find.mutate(number)
  }

  if (isLoading) return <p className="text-slate-400">Loading…</p>
  if (isError || !pkg) return <p className="text-slate-500">Package not found.</p>

  const action = technicianAction(pkg.statusCode, pkg.isInternal)
  const onAction = () => {
    if (!action) return
    if (action.kind === 'confirm-arrival') setArrivalOpen(true)
    else if (pkg.statusCode === 'on_service') setFinishOpen(true)
    else if (pkg.statusCode === 'repaired_waiting_shipment') setShipOpen(true)
    else nextStatus.mutate(undefined)
  }
  const backTo = pkg.isInternal ? '/technician/internal' : '/technician/packages'

  return (
    <div className="space-y-5">
      <Link to={backTo} className="text-sm text-slate-500 hover:underline">
        ← Back to {pkg.isInternal ? 'internal packages' : 'packages'}
      </Link>

      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h1 className="text-2xl font-semibold">{pkg.packageNumber}</h1>
          <PackageStatusBadge code={pkg.statusCode} label={pkg.statusName} />
        </div>
        {action && (
          <button type="button" className={buttonClass} disabled={nextStatus.isPending} onClick={onAction}>
            {action.label}
          </button>
        )}
      </div>

      <div className="grid grid-cols-1 gap-5 lg:grid-cols-3">
        <section className="rounded-lg border border-slate-200 bg-white p-5 lg:col-span-2">
          <h2 className="mb-3 text-sm font-medium text-slate-500">Details</h2>
          <dl className="grid grid-cols-1 gap-3 text-sm sm:grid-cols-2">
            <Field label="Description" value={pkg.description ?? '—'} />
            <Field label="Note" value={pkg.note ?? '—'} />
            <Field label="Approx quantity" value={pkg.approxQuantity?.toString() ?? '—'} />
            <Field label="Outbound tracking" value={pkg.outboundTrackingLink ?? '—'} />
            <Field label="Modules" value={`${pkg.finishedModules}/${pkg.totalModules} finished`} />
            <Field label="Received" value={pkg.receivedAt ? formatDateTime(pkg.receivedAt) : '—'} />
          </dl>
        </section>
        <section className="rounded-lg border border-slate-200 bg-white p-5">
          <h2 className="mb-3 text-sm font-medium text-slate-500">Timeline</h2>
          <Timeline entries={pkg.timeline} />
        </section>
      </div>

      {pkg.statusCode === 'on_service' && (
        <section className="rounded-lg border border-slate-200 bg-white p-5">
          <h2 className="mb-3 text-sm font-medium text-slate-500">Find module</h2>
          <form
            onSubmit={(event) => {
              event.preventDefault()
              submitFind(findInput)
            }}
            className="flex flex-wrap items-end gap-3"
          >
            <div className="flex-1">
              <label className={labelClass} htmlFor="findModule">
                Module number
              </label>
              <input
                id="findModule"
                className={inputClass}
                value={findInput}
                onChange={(event) => setFindInput(event.target.value)}
                placeholder="Scan or type a module number…"
              />
            </div>
            <button type="submit" disabled={find.isPending} className={`${primaryButtonClass} w-auto`}>
              Find
            </button>
            <button type="button" className={secondaryButtonClass} onClick={() => setScanOpen(true)}>
              <QrCode className="h-4 w-4" /> Scan QR
            </button>
          </form>
          <p className="mt-2 text-xs text-slate-400">
            Opens the module to fill in its data. If no service request exists for it yet, you can create one.
          </p>
        </section>
      )}

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white">
        <div className="border-b border-slate-100 px-4 py-2 text-sm font-medium text-slate-500">Modules</div>
        <div className="overflow-x-auto"><table className="w-full min-w-[640px] text-left text-sm">
          <thead className="bg-slate-50 text-slate-500">
            <tr>
              <th className="px-4 py-2 font-medium">Image</th>
              <th className="px-4 py-2 font-medium">Number</th>
              <th className="px-4 py-2 font-medium">Problem type</th>
              <th className="px-4 py-2 font-medium">Status</th>
              <th className="px-4 py-2 font-medium">Technician</th>
              <th className="px-4 py-2 font-medium">Price</th>
              <th className="px-4 py-2 text-right font-medium">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {modules?.items.length === 0 && (
              <tr>
                <td colSpan={7} className="px-4 py-6 text-center text-slate-400">
                  No modules scanned yet.
                </td>
              </tr>
            )}
            {modules?.items.map((module) => (
              <tr key={module.id}>
                <td className="px-4 py-2">
                  {module.thumbnailUrl ? (
                    <img src={module.thumbnailUrl} alt="" className="h-10 w-10 rounded border border-slate-200 object-cover" />
                  ) : (
                    <span className="text-slate-300">—</span>
                  )}
                </td>
                <td className="px-4 py-2 font-medium">{module.moduleNumber}</td>
                <td className="px-4 py-2">{module.problemTypeName}</td>
                <td className="px-4 py-2">
                  <ModuleStatusBadge code={module.statusCode} label={module.statusName} />
                </td>
                <td className="px-4 py-2">{module.technicianName}</td>
                <td className="px-4 py-2">{module.price != null ? `€${module.price}` : '—'}</td>
                <td className="px-4 py-2 text-right">
                  {module.statusCode === 'waiting_for_repair' ? (
                    <Link to={`/technician/modules/${module.id}/repair`} className="text-slate-700 underline">
                      Repair
                    </Link>
                  ) : (
                    <Link to={`/technician/modules/${module.id}`} className="text-slate-600 underline">
                      View
                    </Link>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table></div>
      </section>

      {finishOpen && (
        <ConfirmModal
          title="Finish service"
          message={`Total modules: ${pkg.totalModules}, finished: ${pkg.finishedModules}. Finish service for this package?`}
          confirmLabel="Finish service"
          loading={nextStatus.isPending}
          onConfirm={() => nextStatus.mutate(undefined)}
          onClose={() => setFinishOpen(false)}
        />
      )}
      {shipOpen && (
        <ShipModal loading={nextStatus.isPending} onConfirm={(link) => nextStatus.mutate(link)} onClose={() => setShipOpen(false)} />
      )}
      {arrivalOpen && (
        <ConfirmModal
          title="Confirm arrival"
          message="Confirm that this internal package has arrived?"
          confirmLabel="Confirm arrival"
          loading={confirmArrival.isPending}
          onConfirm={() => confirmArrival.mutate()}
          onClose={() => setArrivalOpen(false)}
        />
      )}
      {scanOpen && (
        <QrScanModal
          onResult={(text) => {
            const value = text.trim()
            setFindInput(value)
            setScanOpen(false)
            submitFind(value)
          }}
          onClose={() => setScanOpen(false)}
        />
      )}
      {createPrompt && (
        <ConfirmModal
          title="No service request found"
          message={`No service request exists for module "${createPrompt}". Create a new service request for it?`}
          confirmLabel="Create service request"
          onConfirm={() => {
            setCreateFor(createPrompt)
            setFindInput('')
            setCreatePrompt(null)
          }}
          onClose={() => setCreatePrompt(null)}
        />
      )}
      {createFor && (
        <CreateRequestModal
          packageId={packageId}
          moduleNumber={createFor}
          onCreated={() => {
            refresh()
            setCreateFor(null)
          }}
          onClose={() => setCreateFor(null)}
        />
      )}
    </div>
  )
}

/** Opens when a scanned/typed module has no request yet: fill in the rest (problem type + images). */
function CreateRequestModal({
  packageId,
  moduleNumber,
  onCreated,
  onClose,
}: {
  packageId: number
  moduleNumber: string
  onCreated: () => void
  onClose: () => void
}) {
  const { data: problemTypes } = useQuery({ queryKey: ['problem-types'], queryFn: technicianApi.problemTypes })
  const [problemTypeId, setProblemTypeId] = useState('')
  const [files, setFiles] = useState<File[]>([])

  const create = useMutation({
    mutationFn: () => technicianApi.createModule(packageId, { moduleNumber, problemTypeId: Number(problemTypeId) }),
    onSuccess: async (module) => {
      if (files.length > 0) {
        try {
          await technicianApi.uploadModuleImages(module.id, files)
        } catch (error) {
          toast.error(getErrorMessage(error))
        }
      }
      toast.success(`Service request created for ${module.moduleNumber}`)
      onCreated()
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    if (!problemTypeId) {
      toast.error('Select a problem type')
      return
    }
    create.mutate()
  }

  const onPick = (selected: File[]) => {
    const error = validateImageFiles(selected)
    if (error) {
      toast.error(error)
      return
    }
    setFiles(selected)
  }

  return (
    <Modal title="New service request" onClose={onClose}>
      <form onSubmit={submit} className="space-y-4">
        <div>
          <label className={labelClass} htmlFor="reqModuleNumber">
            Module number
          </label>
          <input id="reqModuleNumber" className={`${inputClass} bg-slate-50`} value={moduleNumber} readOnly />
        </div>
        <div>
          <label className={labelClass} htmlFor="reqProblemType">
            Problem type
          </label>
          <select
            id="reqProblemType"
            autoFocus
            className={inputClass}
            value={problemTypeId}
            onChange={(event) => setProblemTypeId(event.target.value)}
          >
            <option value="">Select…</option>
            {problemTypes?.map((type) => (
              <option key={type.id} value={type.id}>
                {type.name}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label className={labelClass} htmlFor="reqImages">
            Images (optional)
          </label>
          <input
            id="reqImages"
            type="file"
            accept={IMAGE_ACCEPT_ATTR}
            multiple
            className="mt-1 block text-sm text-slate-600 file:mr-3 file:rounded-md file:border file:border-slate-300 file:bg-white file:px-3 file:py-1.5 file:text-sm file:font-medium file:text-slate-700 hover:file:bg-slate-50"
            onChange={(event) => onPick(Array.from(event.target.files ?? []))}
          />
          {files.length > 0 && (
            <p className="mt-1 text-xs text-slate-400">
              {files.length} {files.length === 1 ? 'image' : 'images'} will be uploaded with this request
            </p>
          )}
        </div>
        <div className="flex justify-end gap-2 pt-2">
          <button type="button" onClick={onClose} className={secondaryButtonClass}>
            Cancel
          </button>
          <button type="submit" disabled={create.isPending} className={`${primaryButtonClass} w-auto`}>
            Create request
          </button>
        </div>
      </form>
    </Modal>
  )
}

function ShipModal({
  loading,
  onConfirm,
  onClose,
}: {
  loading: boolean
  onConfirm: (returnTrackingLink?: string) => void
  onClose: () => void
}) {
  const [link, setLink] = useState('')
  return (
    <Modal title="Mark as shipped" onClose={onClose}>
      <div className="space-y-4">
        <div>
          <label className={labelClass} htmlFor="returnTracking">
            Return tracking link (optional)
          </label>
          <input id="returnTracking" className={inputClass} value={link} onChange={(event) => setLink(event.target.value)} />
        </div>
        <div className="flex justify-end gap-2">
          <button type="button" onClick={onClose} className={secondaryButtonClass}>
            Cancel
          </button>
          <button
            type="button"
            disabled={loading}
            className={`${primaryButtonClass} w-auto`}
            onClick={() => onConfirm(link.trim() || undefined)}
          >
            Mark as shipped
          </button>
        </div>
      </div>
    </Modal>
  )
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-slate-500">{label}</dt>
      <dd className="font-medium break-words">{value}</dd>
    </div>
  )
}
