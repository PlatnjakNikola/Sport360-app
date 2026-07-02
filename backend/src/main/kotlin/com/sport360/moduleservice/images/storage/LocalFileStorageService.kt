package com.sport360.moduleservice.images.storage

import com.sport360.moduleservice.common.NotFoundException
import com.sport360.moduleservice.config.AppProperties
import org.springframework.core.io.Resource
import org.springframework.core.io.UrlResource
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Stores image files under [AppProperties.Image.storagePath] using the
 * `{year}/{month}/{moduleId}/{uuid}.{ext}` layout. Keys are relative to the base directory;
 * resolution is guarded against path traversal.
 */
@Service
class LocalFileStorageService(props: AppProperties) : ImageStorageService {

    private val base: Path = Paths.get(props.image.storagePath).toAbsolutePath().normalize()

    override fun save(moduleId: Long, content: ByteArray, extension: String): String {
        val now = OffsetDateTime.now()
        val key = "%04d/%02d/%d/%s.%s".format(now.year, now.monthValue, moduleId, UUID.randomUUID(), extension)
        val target = resolve(key)
        Files.createDirectories(target.parent)
        Files.write(target, content)
        return key
    }

    override fun load(key: String): Resource {
        val target = resolve(key)
        val resource = UrlResource(target.toUri())
        if (!resource.exists() || !resource.isReadable) throw NotFoundException("Image file not found")
        return resource
    }

    override fun delete(key: String) {
        Files.deleteIfExists(resolve(key))
    }

    /** Resolves a key under the base directory, rejecting anything that escapes it. */
    private fun resolve(key: String): Path {
        val resolved = base.resolve(key).normalize()
        require(resolved.startsWith(base)) { "Invalid storage key" }
        return resolved
    }
}
