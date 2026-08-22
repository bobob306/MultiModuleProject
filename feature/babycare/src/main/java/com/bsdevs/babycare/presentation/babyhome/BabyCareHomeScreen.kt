package com.bsdevs.babycare.presentation.babyhome

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.babycare.presentation.babyactivities.BabyActivity
import com.bsdevs.common.result.Result
import com.bsdevs.data.LocationTypeData
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.NetworkScreenData.BabyFeedHeaderNetwork
import com.bsdevs.renderer.RenderUI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BabyCareHomeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToDeepLink: (String) -> Unit,
    viewModel: BabyCareHomeViewModel = hiltViewModel(),
) {

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is BabyCareHomeNavigationEvent.NavigateToDeepLink -> {
                    // 🚀 Fire the callback up to the NavHost to handle the actual navigation
                    onNavigateToDeepLink(event.uriString)
                }
            }
        }
    }

    val result by viewModel.viewData.collectAsStateWithLifecycle()

    when (val state = result) {
        is Result.Success -> {
            BabyCareHomeScreen(
                viewData = state.data,
                isRefreshing = state.data.isRefreshing,
                onRefresh = { viewModel.refreshData() },
                onNavigate = viewModel::onNavigate,
                onUiIntent = {viewModel.onUiIntent(intent = it as UiIntent.HomeUiIntent)}
            )
        }

        is Result.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is Result.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error loading baby care data")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun BabyCareHomeScreen(
    viewData: BabyCareHomeViewData,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigate: (String, LocationTypeData, String) -> Unit,
    onUiIntent: (UiIntent) -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current
    val activityItems = viewData.activityFeed
    val hasNoRows = remember(activityItems) {
        activityItems.none { it is NetworkScreenData.BabyFeedRowNetwork }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        ActivityFeed(activityItems, viewData.isLoadingMore, hasNoRows, viewData.canLoadMore, context, onUiIntent, onNavigate)
//        LazyColumn(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(horizontal = 16.dp),
//            verticalArrangement = Arrangement.spacedBy(4.dp)
//        ) {
//
//            // 🚀 1. Dynamic Server-Driven UI Renderer Processing Loop
//            activityItems.forEach { screenDataNode ->
//                when (screenDataNode) {
//
//                    // 📌 Pinned Header components intercepting sticky top placements
//                    is BabyFeedHeaderNetwork -> {
//                        stickyHeader(key = "header_${screenDataNode.title}") {
//                            RenderUI(
//                                item = screenDataNode,
//                                context = context,
//                                onNavigationClick = null,
//                                onChipClick = {},
//                                onSwitchClick = {},
//                                onClick = { id, type ->
//                                    when (type) {
//                                        "COLLAPSE" -> onUiIntent(HomeUiIntent.CollapseHeader(id))
//                                        "FILTER" -> onUiIntent(HomeUiIntent.ToggleFilter(id))
//                                        "EDIT" -> {
//                                            if (screenDataNode is NetworkScreenData.BabyFeedRowNetwork) {
//                                                onUiIntent(
//                                                    HomeUiIntent.EditActivityRow(
//                                                        id,
//                                                        screenDataNode.activityType
//                                                    )
//                                                )
//                                            }
//                                        }
//                                    }
//                                },
//                            )
//                        }
//                    }
//
//                    // 📋 Flat components mapping into sequential generic rows
//                    else -> {
//                        val elementKey = when (screenDataNode) {
//                            is NetworkScreenData.BabyDashboardTilesNetwork -> "nappy_tile_${screenDataNode.index}"
//                            is NetworkScreenData.BabyFeedRowNetwork -> "row_${screenDataNode.id}"
//                            else -> "${screenDataNode::class.java.simpleName}_${screenDataNode.index}"
//                        }
//
//                        item(key = elementKey) {
//                            val allRows = remember(activityItems) {
//                                activityItems.filterIsInstance<NetworkScreenData.BabyFeedRowNetwork>()
//                            }
//
//                            // Check pagination boundaries strictly relative to list rows
//                            if (screenDataNode is NetworkScreenData.BabyFeedRowNetwork) {
//                                val globalIndex = allRows.indexOf(screenDataNode)
//                                if (globalIndex >= allRows.size - 1 && viewData.canLoadMore && !viewData.isLoadingMore) {
//                                    LaunchedEffect(Unit) {
//                                        onLoadMore()
//                                    }
//                                }
//                            }
//
//                            Box(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .animateItem()
//                            ) {
//                                RenderUI(
//                                    item = screenDataNode,
//                                    context = context,
//                                    onNavigationClick = { destination, locationType, label ->
//                                        onNavigate(destination, locationType, label)
//                                    },
//                                    onChipClick = {},
//                                    onSwitchClick = {},
//                                    onClick = { id, type ->
//                                        when (type) {
//                                            "COLLAPSE" -> onUiIntent(HomeUiIntent.CollapseHeader(id))
//                                            "FILTER" -> onUiIntent(HomeUiIntent.ToggleFilter(id))
//                                            "EDIT" -> {
//                                                if (screenDataNode is NetworkScreenData.BabyFeedRowNetwork) {
//                                                    onUiIntent(
//                                                        HomeUiIntent.EditActivityRow(
//                                                            id,
//                                                            screenDataNode.activityType
//                                                        )
//                                                    )
//                                                }
//                                            }
//                                        }
//                                    },
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//
//            // ⏳ 2. Structural Spinner Slot for Pagination Loads
//            if (viewData.isLoadingMore) {
//                item(key = "sdui_pagination_spinner") {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(16.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
//                    }
//                }
//            }
//
//            // 📭 3. Structural Row Placeholder when Activity history size matches 0
//
//            if (hasNoRows) {
//                item(key = "sdui_empty_placeholder") {
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(vertical = 32.dp),
//                        contentAlignment = Alignment.Center
//                    ) {
//                        Text(
//                            text = "No recent activity recorded.",
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
//                    }
//                }
//            }
//            // 🛏️ 4. Decorative Spacing Buffer
//            item(key = "sdui_bottom_spacer") { Spacer(modifier = Modifier.padding(bottom = 16.dp)) }
//        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityFeed(
    activityItems: List<NetworkScreenData>,
    isLoadingMore: Boolean,
    hasNoRows: Boolean,
    canLoadMore: Boolean,
    context: Context,
    onUiIntent: (UiIntent) -> Unit,
    onNavigate: (String, LocationTypeData, String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {

        // 🚀 1. Dynamic Server-Driven UI Renderer Processing Loop
        activityItems.forEach { screenDataNode ->
            when (screenDataNode) {

                // 📌 Pinned Header components intercepting sticky top placements
                is BabyFeedHeaderNetwork -> {
                    stickyHeader(key = "header_${screenDataNode.title}") {
                        RenderUI(
                            item = screenDataNode,
                            context = context,
                            onNavigationClick = null,
                            onChipClick = {},
                            onSwitchClick = {},
                            onClick = { id, type ->
                                when (type) {
                                    "COLLAPSE" -> onUiIntent(HomeUiIntent.CollapseHeader(id))
                                    "FILTER" -> onUiIntent(HomeUiIntent.ToggleFilter(id))
                                }
                            },
                        )
                    }
                }

                // 📋 Flat components mapping into sequential generic rows
                else -> {
                    val elementKey = when (screenDataNode) {
                        is NetworkScreenData.BabyDashboardTilesNetwork -> "nappy_tile_${screenDataNode.index}"
                        is NetworkScreenData.BabyFeedRowNetwork -> "row_${screenDataNode.id}"
                        else -> "${screenDataNode::class.java.simpleName}_${screenDataNode.index}"
                    }

                    item(key = elementKey) {
                        val allRows = remember(activityItems) {
                            activityItems.filterIsInstance<NetworkScreenData.BabyFeedRowNetwork>()
                        }

                        // Check pagination boundaries strictly relative to list rows
                        if (screenDataNode is NetworkScreenData.BabyFeedRowNetwork) {
                            val globalIndex = allRows.indexOf(screenDataNode)
                            if (globalIndex >= allRows.size - 1 && canLoadMore && isLoadingMore) {
                                LaunchedEffect(Unit) {
                                    onUiIntent.invoke(HomeUiIntent.LoadMore)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
                        ) {
                            RenderUI(
                                item = screenDataNode,
                                context = context,
                                onNavigationClick = { destination, locationType, label ->
                                    onNavigate(destination, locationType, label)
                                },
                                onChipClick = {},
                                onSwitchClick = {},
                                onClick = { id, type ->
                                    when (type) {
                                        "COLLAPSE" -> onUiIntent(HomeUiIntent.CollapseHeader(id))
                                        "FILTER" -> onUiIntent(HomeUiIntent.ToggleFilter(id))
                                        "EDIT" -> {
                                            if (screenDataNode is NetworkScreenData.BabyFeedRowNetwork) {
                                                onUiIntent(
                                                    HomeUiIntent.EditActivityRow(
                                                        id,
                                                        screenDataNode.activityType
                                                    )
                                                )
                                            }
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }

        // ⏳ 2. Structural Spinner Slot for Pagination Loads
        if (isLoadingMore) {
            item(key = "sdui_pagination_spinner") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }

        // 📭 3. Structural Row Placeholder when Activity history size matches 0

        if (hasNoRows) {
            item(key = "sdui_empty_placeholder") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent activity recorded.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // 🛏️ 4. Decorative Spacing Buffer
        item(key = "sdui_bottom_spacer") { Spacer(modifier = Modifier.padding(bottom = 16.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityFeedItem(
    item: BabyActivity,
    onEdit: () -> Unit,
    onIconClick: () -> Unit // ➕ 1. Add this callback parameter
) {
    val icon = when (item) {
        is BabyActivity.Nappy -> Icons.Default.ChildCare
        is BabyActivity.Feeding -> Icons.Default.Restaurant
    }

    val title = when (item) {
        is BabyActivity.Nappy -> "Nappy Change: ${item.dto.type}"
        is BabyActivity.Feeding -> {
            if (item.dto.mainFeedingSide == "Bottle") {
                "Feeding: Bottle (${item.dto.bottleAmountMl}ml)"
            } else {
                val side = item.dto.mainFeedingSide ?: "Both"
                "Feeding: $side (${formatDuration(item.dto.totalDuration)})"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onEdit
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 🔄 2. Make only this circle clickable to capture the filter event
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    .clip(CircleShape) // Ensures the click ripple is a perfect circle
                    .clickable { onIconClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Filter by type",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatActivityDate(item.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = item.time ?: "",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

private fun formatActivityDate(dateStr: String?): String {
    val date = try {
        LocalDate.parse(dateStr)
    } catch (_: Exception) {
        return dateStr ?: ""
    }
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)

    return when {
        date == today -> "Today"
        date == yesterday -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }
}