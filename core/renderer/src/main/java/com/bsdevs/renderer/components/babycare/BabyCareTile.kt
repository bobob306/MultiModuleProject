package com.bsdevs.renderer.components.babycare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bsdevs.data.BabyDashboardTileNetwork
import com.bsdevs.data.LocationTypeData
import com.bsdevs.data.NetworkScreenData

@Composable
fun BabyCareRow(
    item: NetworkScreenData.BabyDashboardTilesNetwork,
    onClick: (String, LocationTypeData, String) -> Unit
) {
    val nappyTile = item.content[0]
    val feedingTile = item.content[1]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BabyCareTile(
            modifier = Modifier
                .weight(1f)
                .height(120.dp),
            title = "Nappy Change",
            subtitle = nappyTile.lastNappyChange,
            icon = Icons.Default.ChildCare,
            item = nappyTile,
            onClick = onClick
        )
        BabyCareTile(
            modifier = Modifier
                .weight(1f)
                .height(120.dp),
            title = "Feeding",
            subtitle = feedingTile.lastFeeding,
            icon = Icons.Default.Restaurant,
            item = feedingTile,
            onClick = onClick
        )
    }
}

@Composable
fun BabyCareTile(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    item: BabyDashboardTileNetwork,
    onClick: (String, LocationTypeData, String) -> Unit
) {
    Card(
        onClick = { onClick(item.destination, item.locationTypeData, item.label) },
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