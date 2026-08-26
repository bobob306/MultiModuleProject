package com.bsdevs.babycare.presentation.home

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.babycare.presentation.common.BabyActivity
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.renderer.RenderUI
import com.bsdevs.uicomponents.MMPScaffold
import com.bsdevs.uicomponents.shimmer

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BabyCareHomeScreenRoute(
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

    val onDynamicClick: (String, String) -> Unit = { destination, _ ->
        when (destination) {
            "babycare://nappy" -> onNavigateToNappyChange()
            "babycare://feeding" -> onNavigateToFeeding()
            "babycare://temperature" -> onNavigateToTemperature()
            "babycare://graph" -> onNavigateToGraph()
            else -> {
                // Handle unknown or generic navigation if necessary
            }
        }
    }

    when (val state = result) {
        is Result.Success -> {
            BabyCareHomeScreen(
                viewData = state.data,
                onRefresh = { viewModel.refreshData() },
                onNavigateToEditNappyChange = onNavigateToEditNappyChange,
                onNavigateToEditFeeding = onNavigateToEditFeeding,
                onNavigateToEditTemperature = onNavigateToEditTemperature,
                onToggleFilter = viewModel::toggleActivityFilter,
                onToggleHeaderCollapse = viewModel::toggleHeaderCollapse,
                onLoadMore = viewModel::loadMore,
                onDeleteActivity = viewModel::deleteActivity,
                onDynamicClick = onDynamicClick,
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
    onNavigateToEditNappyChange: (String) -> Unit,
    onNavigateToEditFeeding: (String) -> Unit,
    onNavigateToEditTemperature: (String) -> Unit,
    onToggleFilter: (ActivityFilter) -> Unit,
    onToggleHeaderCollapse: (String) -> Unit,
    onLoadMore: () -> Unit,
    onDeleteActivity: (BabyActivity) -> Unit,
    onDynamicClick: (String, String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pullToRefreshState = rememberPullToRefreshState()
    val context = LocalContext.current
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
                viewData.dynamicUi.sortedBy { it.index }.forEach { component ->
                    when (component) {
                        is NetworkScreenData.ActivityFeedDataNetwork -> {
                            renderActivityFeed(
                                viewData = viewData,
                                onToggleHeaderCollapse = onToggleHeaderCollapse,
                                onNavigateToEditNappyChange = onNavigateToEditNappyChange,
                                onNavigateToEditFeeding = onNavigateToEditFeeding,
                                onNavigateToEditTemperature = onNavigateToEditTemperature,
                                onToggleFilter = onToggleFilter,
                                onDeleteActivity = { activityToDelete = it },
                                onLoadMore = onLoadMore,
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }

                        else -> {
                            item(key = "dynamic_${component.index}") {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    RenderUI(
                                        item = component,
                                        context = context,
                                        onClick = onDynamicClick,
                                        onChipClick = {},
                                        onSwitchClick = {},
                                        featureContent = { featureComponent ->
                                            when (featureComponent) {
                                                is NetworkScreenData.TileRowDataNetwork -> {
                                                    BabyCareTileRow(
                                                        viewData = viewData,
                                                        tiles = featureComponent.tiles,
                                                        onDynamicClick = onDynamicClick,
                                                        sharedTransitionScope = sharedTransitionScope,
                                                        animatedVisibilityScope = animatedVisibilityScope,
                                                    )
                                                }
                                                else -> {}
                                            }
                                        }
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
private fun LazyListScope.renderActivityFeed(
    viewData: BabyCareHomeViewData,
    onToggleHeaderCollapse: (String) -> Unit,
    onNavigateToEditNappyChange: (String) -> Unit,
    onNavigateToEditFeeding: (String) -> Unit,
    onNavigateToEditTemperature: (String) -> Unit,
    onToggleFilter: (ActivityFilter) -> Unit,
    onDeleteActivity: (BabyActivity) -> Unit,
    onLoadMore: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
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
                        val index = activityItems.indexOf(feedItem)
                        val animatedOffset = remember { Animatable(100f) }
                        val animatedAlpha = remember { Animatable(0f) }


                        LaunchedEffect(key1 = true) {
                            kotlinx.coroutines.delay((index % 10) * 50L)
                            animatedOffset.animateTo(
                                targetValue = 0f,
                                animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing)
                            )
                        }
                        LaunchedEffect(key1 = true) {
                            kotlinx.coroutines.delay((index % 10) * 50L)
                            animatedAlpha.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 400)
                            )
                        }

                        Box(modifier = Modifier
                            .graphicsLayer {
                                translationY = animatedOffset.value
                                alpha = animatedAlpha.value
                            }
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
                                    onDeleteActivity(currentActivity)
                                },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun BabyCareTileRow(
    viewData: BabyCareHomeViewData,
    tiles: List<NetworkScreenData.TileDataNetwork>,
    onDynamicClick: (String, String) -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
) {
    Row(
        modifier = Modifier
            .horizontalScroll(state = rememberScrollState(), enabled = true)
            .padding(top = 8.dp)
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Spacer(modifier = Modifier.width(0.dp))
        tiles.forEach { tile ->
            val subtitle = when (tile.subtitleType) {
                "NAPPY" -> viewData.lastNappyChange
                "FEEDING" -> viewData.lastFeeding
                "TEMPERATURE" -> viewData.lastTemperature
                else -> null
            }
            BabyCareTile(
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(end = 12.dp)
                    .heightIn(max = 120.dp),
                title = tile.title,
                subtitle = subtitle,
                icon = mapIconNameToVector(tile.iconName),
                onClick = { onDynamicClick(tile.destination, tile.title) },
                sharedTransitionScope = sharedTransitionScope,
                animatedVisibilityScope = animatedVisibilityScope,
                id = tile.sharedElementKey ?: "tile_${tile.index}"
            )
        }
        Spacer(modifier = Modifier.width(0.dp))
    }
}

private fun mapIconNameToVector(iconName: String): ImageVector {
    return when (iconName) {
        "ChildCare" -> Icons.Default.ChildCare
        "Restaurant" -> Icons.Default.Restaurant
        "Thermostat" -> Icons.Default.Thermostat
        "AutoGraph" -> Icons.Default.AutoGraph
        else -> Icons.Default.ChildCare
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
    val (icon, color, onColor) = when (item) {
        is BabyActivity.Nappy -> Triple(
            Icons.Default.ChildCare,
            Color(0xFFE8F5E9), // Light Green
            Color(0xFF2E7D32)  // Dark Green
        )
        is BabyActivity.Feeding -> Triple(
            Icons.Default.Restaurant,
            Color(0xFFE3F2FD), // Light Blue
            Color(0xFF1565C0)  // Dark Blue
        )
        is BabyActivity.Temperature -> Triple(
            Icons.Default.Thermostat,
            Color(0xFFFFF3E0), // Light Orange
            Color(0xFFE65100)  // Dark Orange
        )
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
            val bgColor = when (direction) {
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
                    .background(bgColor)
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
                            .background(color, CircleShape)
                            .clip(CircleShape)
                            .clickable { onIconClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = "Filter by type",
                            modifier = Modifier.size(24.dp),
                            tint = onColor
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
