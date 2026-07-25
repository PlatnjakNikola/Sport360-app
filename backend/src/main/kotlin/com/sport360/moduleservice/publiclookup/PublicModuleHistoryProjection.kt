package com.sport360.moduleservice.publiclookup

import java.time.OffsetDateTime

/**
 * Privacy-safe projection for the public module history. Deliberately excludes technician/client
 * names, package numbers, prices and images — those must never leave the service over this path.
 */
interface PublicModuleHistoryProjection {
    val statusCode: String
    val statusName: String
    val problemTypeName: String
    val pixelsRepaired: Short?
    val chipsReplaced: Short?
    val repairNote: String?
    val completedAt: OffsetDateTime?
    val receivedAt: OffsetDateTime?
    val serviceCompletedAt: OffsetDateTime?
    val shippedAt: OffsetDateTime?
    val arrivedAt: OffsetDateTime?
}
