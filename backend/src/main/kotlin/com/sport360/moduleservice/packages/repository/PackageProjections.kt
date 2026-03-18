package com.sport360.moduleservice.packages.repository

import java.time.OffsetDateTime

interface StatusCountProjection {
    val code: String
    val name: String
    val count: Long
}

interface TimelineProjection {
    val statusCode: String
    val statusName: String
    val changedAt: OffsetDateTime
}

interface RecentActivityProjection {
    val packageId: Long
    val packageNumber: String
    val statusCode: String
    val statusName: String
    val changedAt: OffsetDateTime
}
