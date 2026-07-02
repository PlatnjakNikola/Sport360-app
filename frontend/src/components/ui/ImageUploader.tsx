import { useEffect, useMemo, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { ImagePlus, X } from 'lucide-react'
import toast from 'react-hot-toast'
import { technicianApi } from '../../api/technician'
import { getErrorMessage } from '../../api/client'
import { IMAGE_ACCEPT_ATTR, MAX_IMAGES_PER_MODULE, validateImageFiles } from '../../utils/imageFiles'
import { primaryButtonClass, secondaryButtonClass } from './formStyles'

/** Technician image picker for an existing module: validate, preview, upload. */
export function ImageUploader({
  moduleId,
  currentCount,
  onUploaded,
}: {
  moduleId: number
  currentCount: number
  onUploaded: () => void
}) {
  const [files, setFiles] = useState<File[]>([])
  const previews = useMemo(() => files.map((file) => URL.createObjectURL(file)), [files])
  useEffect(() => () => previews.forEach((url) => URL.revokeObjectURL(url)), [previews])

  const upload = useMutation({
    mutationFn: () => technicianApi.uploadModuleImages(moduleId, files),
    onSuccess: () => {
      toast.success(files.length > 1 ? 'Images uploaded' : 'Image uploaded')
      setFiles([])
      onUploaded()
    },
    onError: (error) => toast.error(getErrorMessage(error)),
  })

  const remaining = MAX_IMAGES_PER_MODULE - currentCount
  if (remaining <= 0) {
    return <p className="text-xs text-slate-400">Maximum of {MAX_IMAGES_PER_MODULE} images reached.</p>
  }

  const onPick = (selected: File[]) => {
    const error = validateImageFiles(selected, currentCount)
    if (error) {
      toast.error(error)
      return
    }
    setFiles(selected)
  }

  return (
    <div className="space-y-3">
      <label className={`${secondaryButtonClass} w-fit cursor-pointer`}>
        <ImagePlus className="h-4 w-4" /> Choose images
        <input
          type="file"
          accept={IMAGE_ACCEPT_ATTR}
          multiple
          className="hidden"
          onChange={(event) => onPick(Array.from(event.target.files ?? []))}
        />
      </label>
      <p className="text-xs text-slate-400">Up to {remaining} more · JPEG, PNG or WebP · max 20MB each</p>

      {files.length > 0 && (
        <>
          <div className="flex flex-wrap gap-2">
            {previews.map((url, index) => (
              <div key={url} className="relative">
                <img src={url} alt="Selected" className="h-20 w-20 rounded-md border border-slate-200 object-cover" />
                <button
                  type="button"
                  onClick={() => setFiles((current) => current.filter((_, i) => i !== index))}
                  className="absolute -right-2 -top-2 rounded-full bg-slate-800 p-0.5 text-white"
                  aria-label="Remove"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              </div>
            ))}
          </div>
          <button
            type="button"
            disabled={upload.isPending}
            onClick={() => upload.mutate()}
            className={`${primaryButtonClass} w-auto`}
          >
            Upload {files.length} {files.length === 1 ? 'image' : 'images'}
          </button>
        </>
      )}
    </div>
  )
}
