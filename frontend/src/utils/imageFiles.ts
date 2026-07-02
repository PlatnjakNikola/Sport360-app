/** Client-side image upload rules, mirrored from the backend (max 5 per module, 20MB, JPEG/PNG/WebP). */
export const MAX_IMAGES_PER_MODULE = 5
export const MAX_IMAGE_SIZE = 20 * 1024 * 1024
export const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']
export const IMAGE_ACCEPT_ATTR = ACCEPTED_IMAGE_TYPES.join(',')

/** Returns an error message if the selection is invalid, otherwise null. */
export function validateImageFiles(files: File[], existingCount = 0): string | null {
  if (existingCount + files.length > MAX_IMAGES_PER_MODULE) {
    return `A module can have at most ${MAX_IMAGES_PER_MODULE} images`
  }
  for (const file of files) {
    if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
      return 'Only JPEG, PNG and WebP images are allowed'
    }
    if (file.size > MAX_IMAGE_SIZE) {
      return 'Each image must be 20MB or smaller'
    }
  }
  return null
}
