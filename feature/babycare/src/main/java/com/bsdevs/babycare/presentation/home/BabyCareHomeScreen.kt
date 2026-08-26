package com.bsdevs.babycare.presentation.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.babycare.presentation.common.BabyActivity
import com.bsdevs.common.result.Result
import com.bsdevs.uicomponents.MMPScaffold
import com.bsdevs.uicomponents.shimmer

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BabyCareHomeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToNappyChange: () -> Unit,
    onNavigateToFeeding: () -> Unit,
    onNavigateToTemperature: () -> Unit,
    onNavigateToGraph: () -> Unit,
    onNavigateToEditNappyChange: (String) -> Unit,
    onNavigateToEditFeeding: (String) -> Unit,
    onNavigateToEditTemperature: (String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: BabyCareHomeViewModel = hiltViewModel(),
) {
    val result by viewModel.viewData.collectAsStateWithLifecycle()

    when (val state = result) {
        is Result.Success -> {
            BabyCareHomeScreen(
                viewData = state.data,
                onRefresh = { viewModel.refreshData() },
                onNavigateToNappyChange = onNavigateToNappyChange,
                onNavigateToFeeding = onNavigateToFeeding,
                onNavigateToTemperature = onNavigateToTemperature,
                onNavigateToEditNappyChange = onNavigateToEditNappyChange,
                onNavigateToEditFeeding = onNavigateToEditFeeding,
                onNavigateToEditTemperature = onNavigateToEditTemperature,
                onToggleFilter = viewModel::toggleActivityFilter,
                onToggleHeaderCollapse = viewModel::toggleHeaderCollapse,
                onLoadMore = viewModel::loadMore,
                onNavigateToGraph = onNavigateToGraph,
                onDeleteActivity = viewModel::deleteActivity,
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }

        is Result.Loading -> {
            BabyCareHomeLoading()
        }

        is Result.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error loading baby care data")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
internal fun BabyCareHomeScreen(
    viewData: BabyCareHomeViewData,
    onRefresh: () -> Unit,
    onNavigateToNappyChange: () -> Unit,
    onNavigateToFeeding: () -> Unit,
    onNavigateToTemperature: () -> Unit,
    onNavigateToEditNappyChange: (String) -> Unit,
    onNavigateToEditFeeding: (String) -> Unit,
    onNavigateToEditTemperature: (String) -> Unit,
    onToggleFilter: (ActivityFilter) -> Unit,
    onToggleHeaderCollapse: (String) -> Unit,
    onNavigateToGraph: () -> Unit,
    onLoadMore: () -> Unit,
    onDeleteActivity: (BabyActivity) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val horizontalPadding = if (isLandscape) 8.dp else 16.dp
    
    var activityToDelete by remember { mutableStateOf<BabyActivity?>(null) }

    if (activityToDelete != null) {
        AlertDialog(
            onDismissRequest = { activityToDelete = null },
            title = { Text("Delete Activity") },
            text = { Text("Are you sure you want to delete this activity? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        activityToDelete?.let { onDeleteActivity(it) }
                        activityToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { activityToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    MMPScaffold(
        title = "Baby Care",
        scrollBehavior = scrollBehavior
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = viewData.isRefreshing,
            onRefresh = {
                onRefresh()
            },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = horizontalPadding, end = horizontalPadding, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(state = rememberScrollState(), enabled = true)
                            .padding(top = 8.dp)
                            .wrapContentHeight(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Spacer(modifier = Modifier.width(0.dp))
                        BabyCareTile(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(end = 12.dp)
                                .heightIn(max = 120.dp),
                            title = "Nappy Change",
                            subtitle = viewData.lastNappyChange,
                            icon = Icons.Default.ChildCare,
                            onClick = onNavigateToNappyChange,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            id = "nappy_tile"
                        )
                        BabyCareTile(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(end = 12.dp)
                                .heightIn(max = 120.dp),
                            title = "Feeding",
                            subtitle = viewData.lastFeeding,
                            icon = Icons.Default.Restaurant,
                            onClick = onNavigateToFeeding,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            id = "feeding_tile"
                        )
                        BabyCareTile(
                            modifier = Modifier
                                .wrapContentWidth()
                                .padding(end = 12.dp)
                                .heightIn(max = 120.dp),
                            title = "Temperature",
                            subtitle = viewData.lastTemperature,
                            icon = Icons.Default.Thermostat,
                            onClick = onNavigateToTemperature,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            id = "temperature_tile"
                        )
                        BabyCareTile(
                            modifier = Modifier
                                .wrapContentWidth()
                                .heightIn(max = 120.dp),
                            title = "Analysis",
                            subtitle = null,
                            icon = Icons.Default.AutoGraph,
                            onClick = onNavigateToGraph,
                            sharedTransitionScope = sharedTransitionScope,
                            animatedVisibilityScope = animatedVisibilityScope,
                            id = "graph_tile"
                        )
                        Spacer(modifier = Modifier.width(0.dp))
                    }
                }

                item {
                    Text(
                        text = "Recent Activity",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }

                val activityItems = viewData.activityFeed

                activityItems.forEach { feedItem ->
                    when (feedItem) {
                        is HomeFeedItem.Header -> {
                            stickyHeader(key = "header_${feedItem.title}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surface)
                                        .clickable { onToggleHeaderCollapse(feedItem.title) }
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isCollapsed =
                                            viewData.collapsedHeaders.contains(feedItem.title)
                                        Text(
                                            text = if (isCollapsed) "▶ ${feedItem.title}" else "▼ ${feedItem.title}",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (feedItem.feedingCount > 0) {
                                            Text(
                                                text = "🍼 ${feedItem.feedingCount}",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        if (feedItem.nappyCount > 0) {
                                            Text(
                                                text = "🍃 ${feedItem.nappyCount}",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                        if (feedItem.temperatureCount > 0) {
                                            Text(
                                                text = "🌡️ ${feedItem.temperatureCount}",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is HomeFeedItem.ActivityRow -> {
                            val currentActivity = feedItem.activity
                            val uniqueId = when (currentActivity) {
                                is BabyActivity.Nappy -> currentActivity.dto.id
                                is BabyActivity.Feeding -> currentActivity.dto.id
                                is BabyActivity.Temperature -> currentActivity.dto.id
                            }

                            item(key = "row_${uniqueId}") {
                                val allRows = remember(activityItems) {
                                    activityItems.filterIsInstance<HomeFeedItem.ActivityRow>()
                                }
                                val globalIndex = allRows.indexOf(feedItem)

                                if (globalIndex >= allRows.size - 1 && viewData.canLoadMore && !viewData.isLoadingMore) {
                                    LaunchedEffect(Unit) {
                                        onLoadMore()
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem()
                                ) {
                                    ActivityFeedItem(
                                        item = currentActivity,
                                        onEdit = {
                                            val activityId = when (currentActivity) {
                                                is BabyActivity.Nappy -> currentActivity.dto.id
                                                is BabyActivity.Feeding -> currentActivity.dto.id
                                                is BabyActivity.Temperature -> currentActivity.dto.id
                                            }
                                            activityId?.let {
                                                when (currentActivity) {
                                                    is BabyActivity.Nappy -> onNavigateToEditNappyChange(activityId)
                                                    is BabyActivity.Feeding -> onNavigateToEditFeeding(activityId)
                                                    is BabyActivity.Temperature -> onNavigateToEditTemperature(activityId)
                                                }
                                            }
                                        },
                                        onIconClick = {
                                            val targetFilter = when (currentActivity) {
                                                is BabyActivity.Nappy -> ActivityFilter.NAPPY
                                                is BabyActivity.Feeding -> ActivityFilter.FEEDING
                                                is BabyActivity.Temperature -> ActivityFilter.TEMPERATURE
                                            }
                                            onToggleFilter(targetFilter)
                                        },
                                        onDelete = {
                                            activityToDelete = currentActivity
                                        },
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            }
                        }
                    }
                }

                if (viewData.isLoadingMore) {
                    item {
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

                if (viewData.activityFeed.isEmpty()) {
                    item {
                        Text(
                            text = "No recent activity recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.padding(bottom = 16.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ActivityFeedItem(
    item: BabyActivity,
    onEdit: () -> Unit,
    onIconClick: () -> Unit,
    onDelete: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val icon = when (item) {
        is BabyActivity.Nappy -> Icons.Default.ChildCare
        is BabyActivity.Feeding -> Icons.Default.Restaurant
        is BabyActivity.Temperature -> Icons.Default.Thermostat
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
        is BabyActivity.Temperature -> "Temperature: ${item.dto.temperature}°C"
    }

    val activityId = when (item) {
        is BabyActivity.Nappy -> item.dto.id
        is BabyActivity.Feeding -> item.dto.id
        is BabyActivity.Temperature -> item.dto.id
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onEdit()
                    false
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }
            val swipeIcon = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Edit
                else -> null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardDefaults.shape)
                    .background(color)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                swipeIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (direction == SwipeToDismissBoxValue.StartToEnd) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) {
        with(sharedTransitionScope) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .sharedElement(
                        sharedContentState = rememberSharedContentState(key = "activity_card_${activityId}"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                    .combinedClickable(
                        onClick = {}, // Disable single tap edit to prevent accidental navigation
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .clip(CircleShape)
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
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        item.comment?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = item.time ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BabyCareTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    id: String
) {
    with(sharedTransitionScope) {
        Card(
            onClick = onClick,
            modifier = modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = "tile_$id"),
                animatedVisibilityScope = animatedVisibilityScope
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BabyCareHomeLoading() {
    MMPScaffold(
        title = "Baby Care"
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .shimmer()
                    )
                }
            }

            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp)
            )

            repeat(5) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .shimmer()
                )
            }
        }
    }
}
