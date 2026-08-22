package com.bsdevs.renderer

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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bsdevs.data.BabyDashboardTileNetwork
import com.bsdevs.data.LocationTypeData
import com.bsdevs.data.LocationTypeData.INTERNAL
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.NetworkScreenData.ActivityFeedNetwork
import com.bsdevs.data.NetworkScreenData.BabyDashboardTilesNetwork
import com.bsdevs.data.NetworkScreenData.BabyFeedHeaderNetwork
import com.bsdevs.data.NetworkScreenData.BabyFeedRowNetwork
import com.bsdevs.data.NetworkScreenData.CardDataNetwork
import com.bsdevs.data.NetworkScreenData.ChipDataNetwork
import com.bsdevs.data.NetworkScreenData.DividerDataNetwork
import com.bsdevs.data.NetworkScreenData.ImageDataNetwork
import com.bsdevs.data.NetworkScreenData.LargeTitleDataNetwork
import com.bsdevs.data.NetworkScreenData.MediumTitleDataNetwork
import com.bsdevs.data.NetworkScreenData.NavigationButtonDataNetwork
import com.bsdevs.data.NetworkScreenData.SmallTitleDataNetwork
import com.bsdevs.data.NetworkScreenData.SpacerDataNetwork
import com.bsdevs.data.NetworkScreenData.SubtitleDataNetwork
import com.bsdevs.data.NetworkScreenData.SwitchDataNetwork
import com.bsdevs.data.NetworkScreenData.Unknown
import com.bsdevs.data.SizeData
import com.bsdevs.data.SpacerTypeData
import com.bsdevs.data.SpacerTypeData.HEIGHT
import com.bsdevs.renderer.components.CardComponent
import com.bsdevs.renderer.components.ChipComponent
import com.bsdevs.renderer.components.MMPButton
import com.bsdevs.renderer.components.SwitchComponent
import com.bsdevs.renderer.components.babycare.BabyCareRow
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RenderUI(
    modifier: Modifier = Modifier,
    item: NetworkScreenData,
    context: Context,
    onClick: ((id: String, type: String?) -> Unit)?,
    onNavigationClick: ((String, LocationTypeData, String) -> Unit)?,
    onChipClick: (Boolean) -> Unit,
    onSwitchClick: (Boolean) -> Unit,
) {
    when (item) {
        is LargeTitleDataNetwork -> Text(
            item.content.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.headlineLarge,
        )

        is MediumTitleDataNetwork -> Text(
            item.content.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleLarge
        )

        is SmallTitleDataNetwork -> Text(
            item.content.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleMedium
        )

        is SubtitleDataNetwork -> Text(item.content)
        is Unknown -> {}
        is SpacerDataNetwork -> {
            if (item.size.type == SpacerTypeData.WEIGHT) {
                // 🚀 FIXED: Uses the layout modifier passed down natively by the parent container!
                item.size.weight?.let { Spacer(modifier = modifier) }
            } else {
                item.size.height?.let { Spacer(modifier = Modifier.size(it.dp)) }
            }
        }

        is ImageDataNetwork -> {
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.url).build(),
                contentDescription = item.contentDescription,
                modifier = Modifier.size(item.height.dp, item.width.dp),
            )
        }

        is NavigationButtonDataNetwork -> {
            MMPButton(
                navigationButtonData = item,
                onClick = onNavigationClick!!,
            )
        }

        is CardDataNetwork -> {
            CardComponent(cardData = item, context = context)
        }

        is DividerDataNetwork -> HorizontalDivider()

        is ChipDataNetwork -> {
            ChipComponent(chipData = item, context = context, onClick = onChipClick)
        }

        is SwitchDataNetwork -> {
            SwitchComponent(switchData = item, context, onSwitchClick = onSwitchClick)
        }

        is BabyDashboardTilesNetwork -> {
            BabyCareRow(item, onNavigationClick!!)
        }

        // 📌 2. AUTOMATICALLY RENDER AGGREGATE DAY STICKY HEADERS
        is BabyFeedHeaderNetwork -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onClick?.invoke(item.title, "COLLAPSE") }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.feedingCount > 0) {
                        Text(
                            text = "🍼 ${item.feedingCount}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    if (item.nappyCount > 0) {
                        Text(
                            text = "👶 ${item.nappyCount}",
                            style = MaterialTheme.typography.labelMedium
                        ) // Using your clean Baby emoji!
                    }
                }
            }
        }

        // 📋 3. AUTOMATICALLY RENDER ACTIVITY LOG CARD ENTRIES
        is BabyFeedRowNetwork -> {
            // Reconstruct your existing ActivityFeedItem or map it right inside the architecture wrapper
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onLongClick = { onClick?.invoke(item.id, "EDIT") },
                        onClick = {},
                    )
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Select vector graphics cleanly based on node attributes parameters
                    val rowIcon =
                        if (item.activityType == "NAPPY") Icons.Default.ChildCare else Icons.Default.Restaurant

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .clickable { onClick?.invoke(item.activityType, "FILTER") }
                    ) {
                        Icon(
                            imageVector = rowIcon,
                            contentDescription = null,
                            modifier = Modifier.align(
                                Alignment.Center
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = item.time,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        else -> {}
    }
}

val datalist = listOf<NetworkScreenData>(
    BabyDashboardTilesNetwork(
        index = 0,
        content = listOf(
            BabyDashboardTileNetwork(
                lastNappyChange = "05:44",
                lastFeeding = null,
                destination = "babycare://nappy",
                locationTypeData = INTERNAL,
                label = "Feeding nav"
            ),
            BabyDashboardTileNetwork(
                lastNappyChange = null,
                lastFeeding = "Left 05:44",
                destination = "babycare://feeding",
                locationTypeData = INTERNAL,
                label = "Feeding nav"
            ),
        ),
    ),
    SpacerDataNetwork(index = 1, size = SizeData(type = HEIGHT, height = 16, weight = null)),
    BabyFeedHeaderNetwork(index = 2, title = "Today", feedingCount = 3, nappyCount = 1),
    BabyFeedRowNetwork(
        index = 3,
        id = "e73e9b49-55bd-4f01-9afa-f85ecf8f25e1",
        activityType = "NAPPY",
        title = "Nappy Change",
        subtitle = "Nappy Change: Both",
        time = "05:44",
        rawDate = "2026-08-22"
    ),
    BabyFeedRowNetwork(
        index = 4,
        id = "f68deb4f-aad5-4ccb-9d60-6cd2fea95904",
        activityType = "FEEDING",
        title = "Feeding Session",
        subtitle = "Left, 10 mins",
        time = "05:44",
        rawDate = "2026-08-22"
    ),
)

val previewData = listOf(
    MediumTitleDataNetwork(index = 5, content = "Recent Activity"),
    LargeTitleDataNetwork(index = 1, content = "Baby Care"),
    SpacerDataNetwork(2, size = SizeData(HEIGHT, height = 12, weight = null)),
    SpacerDataNetwork(4, size = SizeData(HEIGHT, height = 12, weight = null)),
    SpacerDataNetwork(6, size = SizeData(HEIGHT, height = 12, weight = null)),
    SpacerDataNetwork(8, size = SizeData(HEIGHT, height = 12, weight = null)),
    BabyDashboardTilesNetwork(
        3, content = listOf(
            BabyDashboardTileNetwork(
                lastNappyChange = "23:01",
                lastFeeding = null,
                destination = "null",
                locationTypeData = INTERNAL,
                label = ""
            ),
            BabyDashboardTileNetwork(
                lastNappyChange = null,
                lastFeeding = "23:00",
                destination = "",
                locationTypeData = INTERNAL,
                label = ""
            )
        )
    ),
    ActivityFeedNetwork(
        index = 7,
        content = datalist
    ),
)


@Preview
@Composable
private fun PreviewRenderUI() {
    MaterialTheme {
        val context = LocalContext.current
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                previewData.sortedBy { it.index }.forEach {
                    RenderUI(
                        modifier = Modifier,
                        item = it,
                        context = context,
                        onClick = { _, _ -> },
                        onNavigationClick = { _, _, _ -> },
                        onChipClick = {},
                    ) { }
                }
            }
        }
    }
}