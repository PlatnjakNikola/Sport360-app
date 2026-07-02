package com.sport360.moduleservice.modules.repository

import com.sport360.moduleservice.modules.domain.ModuleImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime

/** Projects the first (lowest id) image per module, for list thumbnails without N+1. */
interface FirstImageProjection {
    val moduleId: Long
    val imageId: Long
}

interface ModuleImageRepository : JpaRepository<ModuleImage, Long> {

    fun findAllByModuleIdOrderByIdAsc(moduleId: Long): List<ModuleImage>

    fun countByModuleId(moduleId: Long): Long

    @Query(
        "select mi.moduleId as moduleId, min(mi.id) as imageId from ModuleImage mi " +
            "where mi.moduleId in :moduleIds group by mi.moduleId",
    )
    fun findFirstImageIds(@Param("moduleIds") moduleIds: Collection<Long>): List<FirstImageProjection>

    /** Images whose package arrived before the cutoff — eligible for the 30-day cleanup. */
    @Query(
        value = """
            select mi.* from module_images mi
            join modules m on m.id = mi.module_id
            join packages p on p.id = m.package_id
            where p.arrived_at is not null and p.arrived_at < :cutoff
        """,
        nativeQuery = true,
    )
    fun findExpired(@Param("cutoff") cutoff: OffsetDateTime): List<ModuleImage>
}
