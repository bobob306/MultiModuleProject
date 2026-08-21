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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bsdevs.data.LocationTypeData
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.SpacerTypeData
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
        is NetworkScreenData.TitleDataNetwork -> Text(
            item.content.uppercase(Locale.getDefault()),
            style = MaterialTheme.typography.titleMedium
        )

        is NetworkScreenData.SubtitleDataNetwork -> Text(item.content)
        is NetworkScreenData.Unknown -> {}
        is NetworkScreenData.SpacerDataNetwork -> {
            if (item.size.type == SpacerTypeData.WEIGHT) {
                // 🚀 FIXED: Uses the layout modifier passed down natively by the parent container!
                item.size.weight?.let { Spacer(modifier = modifier) }
            } else {
                item.size.height?.let { Spacer(modifier = Modifier.size(it.dp)) }
            }
        }

        is NetworkScreenData.ImageDataNetwork -> {
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.url).build(),
                contentDescription = item.contentDescription,
                modifier = Modifier.size(item.height.dp, item.width.dp),
            )
        }

        is NetworkScreenData.NavigationButtonDataNetwork -> {
            MMPButton(
                navigationButtonData = item,
                onClick = onNavigationClick!!,
            )
        }

        is NetworkScreenData.CardDataNetwork -> {
            CardComponent(cardData = item, context = context)
        }

        is NetworkScreenData.DividerDataNetwork -> HorizontalDivider()

        is NetworkScreenData.ChipDataNetwork -> {
            ChipComponent(chipData = item, context = context, onClick = onChipClick)
        }

        is NetworkScreenData.SwitchDataNetwork -> {
            SwitchComponent(switchData = item, context, onSwitchClick = onSwitchClick)
        }

        is NetworkScreenData.BabyDashboardTilesNetwork -> {
            BabyCareRow(item, onNavigationClick!!)
        }

        // 📌 2. AUTOMATICALLY RENDER AGGREGATE DAY STICKY HEADERS
        is NetworkScreenData.BabyFeedHeaderNetwork -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { onClick?.invoke(item.title, "COLLAPSE") }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.feedingCount > 0) {
                        Text(text = "🍼 ${item.feedingCount}", style = MaterialTheme.typography.labelMedium)
                    }
                    if (item.nappyCount > 0) {
                        Text(text = "👶 ${item.nappyCount}", style = MaterialTheme.typography.labelMedium) // Using your clean Baby emoji!
                    }
                }
            }
        }

        // 📋 3. AUTOMATICALLY RENDER ACTIVITY LOG CARD ENTRIES
        is NetworkScreenData.BabyFeedRowNetwork -> {
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
                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Select vector graphics cleanly based on node attributes parameters
                    val rowIcon = if (item.activityType == "NAPPY") Icons.Default.ChildCare else Icons.Default.Restaurant

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .clickable { onClick?.invoke(item.activityType, "FILTER") }
                    ) {
                        Icon(imageVector = rowIcon, contentDescription = null, modifier = Modifier.align(
                            Alignment.Center))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(text = item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Text(text = item.time, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        }

        else -> {}
    }
}