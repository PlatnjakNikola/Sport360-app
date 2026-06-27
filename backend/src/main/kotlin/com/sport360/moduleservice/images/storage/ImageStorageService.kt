package com.sport360.moduleservice.images.storage

import org.springframework.core.io.Resource

/**
 * Storage abstraction for image bytes. The local implementation writes to disk; a future
 * S3/R2 implementation can replace it without touching callers. The DB stores only the key.
 */
interface ImageStorageService {

    /** Persists [content] for a module and returns the storage key to save in the DB. */
    fun save(moduleId: Long, content: ByteArray, extension: String): String

    /** Loads a previously stored object by its key. */
    fun load(key: String): Resource

    /** Removes a stored object. Missing objects are ignored. */
    fun delete(key: String)
}
