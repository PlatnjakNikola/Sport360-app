import { Modal } from './Modal'
import { primaryButtonClass, secondaryButtonClass } from './formStyles'

export function ConfirmModal({
  title,
  message,
  confirmLabel = 'Confirm',
  loading = false,
  onConfirm,
  onClose,
}: {
  title: string
  message: string
  confirmLabel?: string
  loading?: boolean
  onConfirm: () => void
  onClose: () => void
}) {
  return (
    <Modal title={title} onClose={onClose}>
      <p className="text-sm text-slate-600">{message}</p>
      <div className="mt-5 flex justify-end gap-2">
        <button type="button" onClick={onClose} className={secondaryButtonClass}>
          Cancel
        </button>
        <button type="button" onClick={onConfirm} disabled={loading} className={`${primaryButtonClass} w-auto`}>
          {confirmLabel}
        </button>
      </div>
    </Modal>
  )
}
