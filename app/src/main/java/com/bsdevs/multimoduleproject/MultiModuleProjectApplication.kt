package com.bsdevs.multimoduleproject

import android.app.Application
import androidx.appfunctions.AppFunctionConfiguration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MultiModuleProjectApplication : Application(), AppFunctionConfiguration.Provider {
    override val appFunctionConfiguration: AppFunctionConfiguration
        get() = AppFunctionConfiguration.Builder().build()
}