package com.bsdevs.navigation

import android.net.Uri
import android.util.Log
import androidx.navigation.NavController
import com.bsdevs.babycare.presentation.navigation.FeedingRoute
import com.bsdevs.babycare.presentation.navigation.NappyChangeRoute

object DeepLinkRouter {
    private const val TAG = "DEEPLINK_ROUTER"

    /**
     * 🎯 Centralized traffic controller to decode URI strings and navigate type-safely
     */
    fun navigate(navController: NavController, uriString: String) {
        try {
            val safeUri = Uri.parse(uriString)
            val schemeAndHost = "${safeUri.scheme}://${safeUri.host}"

            // 🌟 FIXED FALLBACK: Try to extract 'activityId' first, then fall back to 'id' if needed!
            val activityId = safeUri.getQueryParameter("activityId") ?: safeUri.getQueryParameter("id")
            val startSide = safeUri.getQueryParameter("startSide")

            // 🌟 FIXED FALLBACK: Checks for both standard path hooks and your edit action prefixes
            when (schemeAndHost) {
                "babycare://nappy", "babycare://edit_nappy" -> {
                    // 🚀 Natively forwards the clean extracted ID straight down
                    navController.navigate(NappyChangeRoute(activityId = activityId))
                }

                "babycare://feeding", "babycare://edit_feeding" -> {
                    navController.navigate(
                        FeedingRoute(
                            activityId = activityId,
                            startSide = startSide
                        )
                    )
                }

                // 🌐 Generic Fallback for standard deep links
                else -> {
                    navController.navigate(safeUri)
                }
            }
            Log.d(TAG, "🚀 Successfully routed deep link: $uriString")
        } catch (e: Exception) {
            Log.e(TAG, "💥 Failed to decode and resolve central route graph: $uriString", e)
        }
    }
}
