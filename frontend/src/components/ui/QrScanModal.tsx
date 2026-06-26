import { useEffect, useId, useRef, useState } from 'react'
import { Html5Qrcode } from 'html5-qrcode'
import { Modal } from './Modal'

/** Camera QR scanner in a modal. Fires onResult once with the decoded text; the caller then closes it. */
export function QrScanModal({ onResult, onClose }: { onResult: (text: string) => void; onClose: () => void }) {
  const [error, setError] = useState<string | null>(null)
  const handled = useRef(false)
  // A stable, unique element id so two scanners can never collide on the page.
  const readerId = `qr-reader-${useId().replace(/:/g, '')}`

  useEffect(() => {
    const scanner = new Html5Qrcode(readerId)
    let started = false
    scanner
      .start(
        { facingMode: 'environment' },
        { fps: 10, qrbox: 240 },
        (text) => {
          if (handled.current) return
          handled.current = true
          onResult(text)
        },
        () => { /* ignore per-frame decode errors */ },
      )
      .then(() => { started = true })
      .catch(() => setError('Unable to access the camera. Check permissions or type the number instead.'))

    return () => {
      if (started) {
        scanner.stop().then(() => scanner.clear()).catch(() => { /* already stopped */ })
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <Modal title="Scan module QR" onClose={onClose}>
      <div className="space-y-3">
        <div id={readerId} className="overflow-hidden rounded-md" />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <p className="text-xs text-slate-400">Point the camera at the QR code on the module label.</p>
      </div>
    </Modal>
  )
}
