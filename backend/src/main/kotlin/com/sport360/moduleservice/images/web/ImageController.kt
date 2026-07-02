package com.sport360.moduleservice.images.web

import com.sport360.moduleservice.images.service.ImageService
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

/** Serves module image bytes to authenticated users; access checks live in [ImageService]. */
@RestController
@RequestMapping("/api/v1/images")
class ImageController(private val imageService: ImageService) {

    @GetMapping("/{id}")
    fun serve(@PathVariable id: Long): ResponseEntity<Resource> {
        val image = imageService.serve(id)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(image.contentType))
            .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePrivate())
            .body(image.resource)
    }
}
