package com.bsdevs.babycare

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bsdevs.babycare.network.NappyChangeDto
import com.bsdevs.common.result.Result
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun BabyCareHomeScreenRoute(
    onShowSnackBar: suspend (String, String?) -> Unit,
    onNavigateToNappyChange: () -> Unit,
    onNavigateToFeeding: () -> Unit,
    onNavigateToEditNappyChange: (String) -> Unit,
    onNavigateToEditFeeding: (String) -> Unit,
    viewModel: BabyCareHomeViewModel = hiltViewModel(),
) {
    val result by viewModel.viewData.collectAsStateWithLifecycle()

    when (val state = result) {
        is Result.Success -> {
            BabyCareHomeScreen(
                viewData = state.data,
                // If your success viewData already tracks the root refresh loading block:
                isRefreshing = state.data.isRefreshing, // Or map to a dedicated 'isRefreshing' state field if available
                onRefresh = { viewModel.refreshData() }, // Make sure your ViewModel exposes a refresh method
                onNavigateToNappyChange = onNavigateToNappyChange,
                onNavigateToFeeding = onNavigateToFeeding,
                onNavigateToEditNappyChange = onNavigateToEditNappyChange,
                onNavigateToEditFeeding = onNavigateToEditFeeding,
                onToggleFilter = viewModel::toggleActivityFilter,
                onLoadMore = viewModel::loadMore
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
    onNavigateToNappyChange: () -> Unit,
    onNavigateToFeeding: () -> Unit,
    onNavigateToEditNappyChange: (String) -> Unit,
    onNavigateToEditFeeding: (String) -> Unit,
    onToggleFilter: (ActivityFilter) -> Unit,
    onLoadMore: () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()
    // 1. Direct PullToRefreshBox wrapper at the root level

    // 🔄 1. Dynamically chunk your flat list into date groups whenever the feed changes
    val groupedActivities = remember(viewData.activityFeed) {
        viewData.activityFeed.groupBy { item ->
            // Use the combined dateTime string if available, otherwise fall back to the raw date field
            val referenceDate = item.dateTime
            formatHeaderDate(referenceDate)
        }
    }
    PullToRefreshBox(
        isRefreshing = isRefreshing, // Use the localized ui flag
        onRefresh = {
            onRefresh() // Trigger your Firebase fetch logic
        },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize()
    ) {
        // 2. The scrollable view MUST be the immediate child so gestures sync perfectly
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Item 1: App Header
            item {
                Text(
                    text = "Baby Care",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            // Item 2: Quick Action Tiles Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BabyCareTile(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 120.dp),
                        title = "Nappy Change",
                        subtitle = viewData.lastNappyChange,
                        icon = Icons.Default.ChildCare,
                        onClick = onNavigateToNappyChange
                    )
                    BabyCareTile(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(max = 120.dp),
                        title = "Feeding",
                        subtitle = viewData.lastFeeding,
                        icon = Icons.Default.Restaurant,
                        onClick = onNavigateToFeeding
                    )
                }
            }

            // Item 3: Activity Feed Label
            item {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }

            // 📱 Item 4: Sticky Grouped Activity Feed Layout
            groupedActivities.forEach { (dateHeader, activitiesInDay) ->

                // 📌 Opaque Box Sticky Header (Prevents cards from bleeding through when scrolling)
                stickyHeader(key = dateHeader) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = dateHeader,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                // 📋 Scrollable Feed Logs list inside BabyCareHomeScreen
                itemsIndexed(
                    items = activitiesInDay,
                    key = { _, item -> item.id ?: "" }
                ) { _, item ->

                    val globalIndex = viewData.activityFeed.indexOf(item)
                    if (globalIndex >= viewData.activityFeed.size - 1 && viewData.canLoadMore && !viewData.isLoadingMore) {
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
                            item = item,
                            onEdit = {
                                when (item) {
                                    is BabyActivity.Nappy -> item.id?.let { onNavigateToEditNappyChange(it) }
                                    is BabyActivity.Feeding -> item.id?.let { onNavigateToEditFeeding(it) }
                                }
                            },
                            // 🛠️ CONNECT: Determine filter enum dynamically when clicking the row icon
                            onIconClick = {
                                val targetFilter = when (item) {
                                    is BabyActivity.Nappy -> ActivityFilter.NAPPY
                                    is BabyActivity.Feeding -> ActivityFilter.FEEDING
                                }
                                onToggleFilter(targetFilter) // Triggers your ViewModel commonised filter function!
                            }
                        )
                    }
                }
            }

            // Item 5: Bottom Loading Spinner for Pagination
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

            // Item 6: Empty Placeholder State
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

            // Item 7: Bottom Padding Spacing
            item { Spacer(modifier = Modifier.padding(bottom = 16.dp)) }
        }
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

@Composable
fun BabyCareTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                    modifier = Modifier.size(32.dp), // 📏 Slightly smaller icon to guarantee it fits under 120.dp
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1, // 🛡️ Safe guard against text wrapping
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

private fun formatHeaderDate(dateString: String): String {
    return try {
        // Safe check: extract just the YYYY-MM-DD segment if it contains time info
        val cleanDateStr = dateString.split(" ").firstOrNull() ?: dateString
        val targetDate = LocalDate.parse(cleanDateStr)
        val today = LocalDate.now()

        when (targetDate) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> {
                val day = targetDate.dayOfMonth
                val suffix = when {
                    day in 11..13 -> "th"
                    day % 10 == 1 -> "st"
                    day % 10 == 2 -> "nd"
                    day % 10 == 3 -> "rd"
                    else -> "th"
                }
                val monthFormatter = DateTimeFormatter.ofPattern(" MMMM", Locale.ENGLISH)
                "$day$suffix${targetDate.format(monthFormatter)}"
            }
        }
    } catch (e: Exception) {
        dateString // Fallback safety representation if parsing strings fails
    }
}
