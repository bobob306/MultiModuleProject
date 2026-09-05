package com.bsdevs.babycare.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.renderer.RenderUI
import com.bsdevs.uicomponents.MMPScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenericSduiScreen(
    screenId: String,
    title: String,
    onNavigateBack: (() -> Unit)? = null,
    onAddNew: (() -> Unit)? = null,
    onDynamicClick: (String, String) -> Unit = { _, _ -> },
    lazyFeatureContent: LazyListScope.(NetworkScreenData) -> Boolean = { false },
    viewModel: GenericSduiViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
) {
    val uiState by remember(screenId) { viewModel.getUiState(screenId) }.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    MMPScaffold(
        title = title,
        onBackClick = onNavigateBack,
        floatingActionButton = {
            if (onAddNew != null) {
                FloatingActionButton(onClick = onAddNew) {
                    Icon(Icons.Default.Add, contentDescription = "Add New")
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh(screenId) },
            modifier = Modifier.padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp), // Unified horizontal padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (val result = uiState) {
                    is Result.Success -> {
                        result.data.forEach { component ->
                            val handled = lazyFeatureContent(component)
                            if (!handled) {
                                item(key = "dynamic_${component.index}") {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        RenderUI(
                                            item = component,
                                            context = context,
                                            onClick = onDynamicClick,
                                            onChipClick = {},
                                            onSwitchClick = {}
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is Result.Loading -> {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is Result.Error -> {
                        item {
                            Text("Error loading screen configuration", modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}
