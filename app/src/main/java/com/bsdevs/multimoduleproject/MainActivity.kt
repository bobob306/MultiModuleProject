package com.bsdevs.multimoduleproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.mandatorySystemGesturesPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.systemGesturesPadding
import androidx.compose.foundation.layout.waterfall
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Short
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bsdevs.navigation.MMPNavHost
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val paddingValues = WindowInsets.navigationBars.asPaddingValues()
            val otherPadding = WindowInsets.systemBars.asPaddingValues()
            val thisPadding = WindowInsets.safeContent.asPaddingValues()
            Scaffold(
                modifier = Modifier.fillMaxSize().padding(otherPadding).padding(paddingValues),
                snackbarHost = { SnackbarHost(snackbarHostState) },
            ) { it
                MMPNavHost(
                    onShowSnackBar = { message, action ->
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = message,
                                actionLabel = action,
                                duration = Short
                            ) == SnackbarResult.ActionPerformed
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}