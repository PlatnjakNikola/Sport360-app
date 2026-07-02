import { useState } from 'react'
import { X } from 'lucide-react'
import type { ModuleImage } from '../../types/module'

/** Thumbnail grid with a click-to-enlarge lightbox. Read-only; renders nothing when empty. */
export function ImageGallery({ images }: { images: ModuleImage[] }) {
  const [active, setActive] = useState<ModuleImage | null>(null)
  if (images.length === 0) return <p className="text-sm text-slate-400">No images.</p>

  return (
    <>
      <div className="flex flex-wrap gap-2">
        {images.map((image) => (
          <button
            key={image.id}
            type="button"
            onClick={() => setActive(image)}
            className="overflow-hidden rounded-md border border-slate-200 hover:border-slate-400"
          >
            <img src={image.url} alt="Module" loading="lazy" className="h-20 w-20 object-cover" />
          </button>
        ))}
      </div>
      {active && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4"
          onClick={() => setActive(null)}
          role="presentation"
        >
          <button
            type="button"
            onClick={() => setActive(null)}
            className="absolute right-4 top-4 text-white/80 hover:text-white"
            aria-label="Close"
          >
            <X className="h-6 w-6" />
          </button>
          <img
            src={active.url}
            alt="Module"
            className="max-h-[85vh] max-w-full rounded-lg"
            onClick={(event) => event.stopPropagation()}
          />
        </div>
      )}
    </>
  )
}
