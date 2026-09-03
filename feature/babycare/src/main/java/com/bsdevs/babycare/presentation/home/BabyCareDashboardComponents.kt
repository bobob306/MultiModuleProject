package com.bsdevs.babycare.presentation.home

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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.babycare.presentation.common.BabyActivity
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BabyCareTileRowComponent(
    viewData: BabyCareHomeViewData?,
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
                "NAPPY" -> viewData?.lastNappyChange
                "FEEDING" -> viewData?.lastFeeding
                "TEMPERATURE" -> viewData?.lastTemperature
                "MEASUREMENT" -> viewData?.lastMeasurement
                "VACCINATION" -> viewData?.lastVaccination
                "ANALYSIS" -> "View Routine Insights"
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
fun LazyListScope.ActivityFeedItems(
    viewData: BabyCareHomeViewData,
    onToggleHeaderCollapse: (String) -> Unit,
    onToggleActivityFilter: (ActivityFilter) -> Unit,
    onDeleteActivity: (BabyActivity) -> Unit,
    onLoadMore: () -> Unit,
    onNavigateToEditNappyChange: (String) -> Unit,
    onNavigateToEditFeeding: (String) -> Unit,
    onNavigateToEditTemperature: (String) -> Unit,
    onNavigateToEditMeasurement: (String) -> Unit,
    onNavigateToEditVaccination: (String) -> Unit,
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
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isCollapsed = viewData.collapsedHeaders.contains(feedItem.title)
                            Text(
                                text = if (isCollapsed) "▶ ${feedItem.title}" else "▼ ${feedItem.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            modifier = Modifier.padding(start = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (feedItem.feedingCount > 0) Text(text = "🍼 ${feedItem.feedingCount}", style = MaterialTheme.typography.labelMedium)
                            if (feedItem.nappyCount > 0) Text(text = "🍃 ${feedItem.nappyCount}", style = MaterialTheme.typography.labelMedium)
                            if (feedItem.temperatureCount > 0) Text(text = "🌡️ ${feedItem.temperatureCount}", style = MaterialTheme.typography.labelMedium)
                            if (feedItem.measurementCount > 0) Text(text = "⚖️ ${feedItem.measurementCount}", style = MaterialTheme.typography.labelMedium)
                            if (feedItem.vaccinationCount > 0) Text(text = "💉 ${feedItem.vaccinationCount}", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }

            is HomeFeedItem.ActivityRow -> {
                val currentActivity = feedItem.activity

                item(key = "row_${currentActivity.id ?: "unknown_${feedItem.hashCode()}"}") {
                    // Pagination trigger
                    val allRows = activityItems.filterIsInstance<HomeFeedItem.ActivityRow>()
                    val globalIndex = allRows.indexOf(feedItem)
                    if (globalIndex >= allRows.size - 1 && viewData.canLoadMore && !viewData.isLoadingMore) {
                        LaunchedEffect(Unit) { onLoadMore() }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        val index = activityItems.indexOf(feedItem)
                        val animatedOffset = remember { Animatable(100f) }
                        val animatedAlpha = remember { Animatable(0f) }

                        LaunchedEffect(key1 = true) {
                            delay(((index % 10) * 50L).milliseconds)
                            animatedOffset.animateTo(0f, tween(durationMillis = 400, easing = LinearOutSlowInEasing))
                        }
                        LaunchedEffect(key1 = true) {
                            delay(((index % 10) * 50L).milliseconds)
                            animatedAlpha.animateTo(1f, tween(durationMillis = 400))
                        }

                        Box(modifier = Modifier.graphicsLayer {
                            translationY = animatedOffset.value
                            alpha = animatedAlpha.value
                        }) {
                            ActivityFeedItem(
                                item = currentActivity,
                                onEdit = {
                                    val activityId = currentActivity.id
                                    activityId?.let {
                                        when (currentActivity) {
                                            is BabyActivity.Nappy -> onNavigateToEditNappyChange(activityId)
                                            is BabyActivity.Feeding -> onNavigateToEditFeeding(activityId)
                                            is BabyActivity.Temperature -> onNavigateToEditTemperature(activityId)
                                            is BabyActivity.Measurement -> onNavigateToEditMeasurement(activityId)
                                            is BabyActivity.Vaccination -> onNavigateToEditVaccination(activityId)
                                        }
                                    }
                                },
                                onIconClick = {
                                    val targetFilter = when (currentActivity) {
                                        is BabyActivity.Nappy -> ActivityFilter.NAPPY
                                        is BabyActivity.Feeding -> ActivityFilter.FEEDING
                                        is BabyActivity.Temperature -> ActivityFilter.TEMPERATURE
                                        is BabyActivity.Measurement -> ActivityFilter.MEASUREMENT
                                        is BabyActivity.Vaccination -> ActivityFilter.VACCINATION
                                    }
                                    onToggleActivityFilter(targetFilter)
                                },
                                onDelete = { onDeleteActivity(currentActivity) },
                                sharedTransitionScope = sharedTransitionScope,
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (viewData.isLoadingMore) {
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }

    if (activityItems.isEmpty() && !viewData.isRefreshing) {
        item {
            Text(
                text = "No recent activity recorded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
    }
}

// --- Dashboard Components ---

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
        is BabyActivity.Nappy -> Triple(Icons.Default.ChildCare, Color(0xFFE8F5E9), Color(0xFF2E7D32))
        is BabyActivity.Feeding -> Triple(Icons.Default.Restaurant, Color(0xFFE3F2FD), Color(0xFF1565C0))
        is BabyActivity.Temperature -> Triple(Icons.Default.Thermostat, Color(0xFFFFF3E0), Color(0xFFE65100))
        is BabyActivity.Measurement -> Triple(Icons.Default.AutoGraph, Color(0xFFF3E5F5), Color(0xFF7B1FA2))
        is BabyActivity.Vaccination -> Triple(Icons.Default.MedicalServices, Color(0xFFFCE4EC), Color(0xFFC2185B))
    }

    val title = when (item) {
        is BabyActivity.Nappy -> "Nappy Change: ${item.dto.type}"
        is BabyActivity.Feeding -> {
            if (item.dto.mainFeedingSide == "Bottle") "Feeding: Bottle (${item.dto.bottleAmountMl}ml)"
            else "Feeding: ${item.dto.mainFeedingSide ?: "Both"} (${formatDuration(item.dto.totalDuration)})"
        }
        is BabyActivity.Temperature -> "Temperature: ${item.dto.temperature}°C"
        is BabyActivity.Measurement -> {
            val weightStr = item.dto.weight?.let { "Weight: " + String.format(Locale.getDefault(), "%.2fkg", it) } ?: ""
            val heightStr = item.dto.height?.let { "Height: " + String.format(Locale.getDefault(), "%.1fcm", it) } ?: ""
            val headStr = item.dto.headCircumference?.let { "Head Circ.: " + String.format(Locale.getDefault(), "%.1fcm", it) } ?: ""
            val typeStr = if (item.dto.isMedical) " (Medical)" else " (Self)"
            "Measurement: ${listOf(weightStr, heightStr, headStr).filter { it.isNotEmpty() }.joinToString(", ")}$typeStr"
        }
        is BabyActivity.Vaccination -> "Vaccination: ${item.dto.vaccinationNames.joinToString(", ")}"
    }

    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd) { onDelete(); dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
        else if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) { onEdit(); dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            if (direction == SwipeToDismissBoxValue.Settled) return@SwipeToDismissBox
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CardDefaults.shape)
                    .background(bgColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                val swipeIcon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Delete else Icons.Default.Edit
                Icon(
                    swipeIcon,
                    contentDescription = null,
                    tint = if (direction == SwipeToDismissBoxValue.StartToEnd) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        with(sharedTransitionScope) {
            Card(
                modifier = Modifier.fillMaxWidth().sharedElement(rememberSharedContentState(key = "activity_card_${item.id}"), animatedVisibilityScope).combinedClickable(onClick = onEdit, onLongClick = onEdit),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(color, CircleShape).clip(CircleShape).clickable { onIconClick() }, contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = "Filter", modifier = Modifier.size(24.dp), tint = onColor)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        item.comment?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Text(text = item.time ?: "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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
            modifier = modifier.sharedElement(rememberSharedContentState(key = "tile_$id"), animatedVisibilityScope),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(8.dp)) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(text = title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
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

private fun mapIconNameToVector(iconName: String): ImageVector {
    return when (iconName) {
        "ChildCare" -> Icons.Default.ChildCare
        "Restaurant" -> Icons.Default.Restaurant
        "Thermostat" -> Icons.Default.Thermostat
        "AutoGraph" -> Icons.Default.AutoGraph
        "Scale" -> Icons.Default.AutoGraph
        "Vaccines" -> Icons.Default.MedicalServices
        else -> Icons.Default.ChildCare
    }
}
