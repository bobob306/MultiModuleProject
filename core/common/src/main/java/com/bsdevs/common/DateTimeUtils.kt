package com.bsdevs.common

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun parseToInstant(dateTimeStr: String): Instant {
        if (dateTimeStr.isBlank()) return Instant.EPOCH
        return try {
            Instant.parse(dateTimeStr)
        } catch (_: Exception) {
            try {
                val normalized = if (dateTimeStr.contains(" ") && !dateTimeStr.contains("T")) {
                    dateTimeStr.replace(" ", "T")
                } else {
                    dateTimeStr
                }
                LocalDateTime.parse(normalized).atZone(ZoneId.systemDefault()).toInstant()
            } catch (_: Exception) {
                Instant.EPOCH
            }
        }
    }

    fun formatIsoToTime(iso: String?, zoneId: ZoneId = ZoneId.systemDefault()): String? {
        if (iso == null) return null
        return try {
            OffsetDateTime.parse(iso).atZoneSameInstant(zoneId).format(timeFormatter)
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(iso).format(timeFormatter)
            } catch (_: Exception) {
                iso
            }
        }
    }

    fun extractTime(time: String, dateTimeString: String): String {
        return time.ifEmpty {
            dateTimeString.substringAfter("T", dateTimeString.substringAfter(" ", "")).take(5)
        }
    }
}
