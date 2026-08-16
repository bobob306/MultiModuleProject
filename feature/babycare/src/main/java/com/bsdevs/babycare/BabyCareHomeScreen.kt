package com.bsdevs.babycare

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BabyCareHomeScreen(
    viewData: BabyCareHomeViewData,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onNavigateToNappyChange: () -> Unit,
    onNavigateToFeeding: () -> Unit,
    onLoadMore: () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()
    // 1. Direct PullToRefreshBox wrapper at the root level

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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BabyCareTile(
                        modifier = Modifier.weight(1f),
                        title = "Nappy Change",
                        subtitle = viewData.lastNappyChange,
                        icon = Icons.Default.ChildCare,
                        onClick = onNavigateToNappyChange
                    )
                    BabyCareTile(
                        modifier = Modifier.weight(1f),
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

            // Item 4: Dynamic Activity List Feed
            itemsIndexed(
                items = viewData.activityFeed,
                key = { _, item -> item.id ?: "" }
            ) { index, item ->
                // Pagination check
                if (index >= viewData.activityFeed.size - 1 && viewData.canLoadMore && !viewData.isLoadingMore) {
                    LaunchedEffect(Unit) {
                        onLoadMore()
                    }
                }
                ActivityFeedItem(item)
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

@Composable
fun ActivityFeedItem(item: NappyChangeDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChildCare,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Nappy Change: ${item.type}",
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
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
