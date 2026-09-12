package com.bsdevs.data.repository

object CacheConstants {
    // Data is considered stale after 4 hours
    const val STALE_THRESHOLD_MS = 4 * 60 * 60 * 1000L

    fun isStale(lastUpdated: Long): Boolean {
        return (System.currentTimeMillis() - lastUpdated) > STALE_THRESHOLD_MS
    }
}
