package com.bsdevs.babycare.presentation.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.widget.RemoteViews
import com.bsdevs.babycare.R
import com.google.firebase.auth.FirebaseAuth

class BabyCareWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.baby_care_widget)
        
        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

        // Left Feed
        views.setOnClickPendingIntent(R.id.btn_left_feed, createPendingIntent(context, "left", isLoggedIn))
        
        // Right Feed
        views.setOnClickPendingIntent(R.id.btn_right_feed, createPendingIntent(context, "right", isLoggedIn))
        
        // Nappy
        views.setOnClickPendingIntent(R.id.btn_nappy, createPendingIntent(context, "nappy", isLoggedIn))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun createPendingIntent(context: Context, action: String, isLoggedIn: Boolean): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setClassName(context.packageName, "com.bsdevs.multimoduleproject.MainActivity")
            
            if (isLoggedIn) {
                data = when (action) {
                    "left" -> "babycare://feeding?startSide=left".toUri()
                    "right" -> "babycare://feeding?startSide=right".toUri()
                    "nappy" -> "babycare://nappy".toUri()
                    else -> null
                }
            } else {
                data = "babycare://login".toUri()
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        return PendingIntent.getActivity(
            context, 
            action.hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
