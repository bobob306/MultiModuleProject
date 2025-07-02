package com.bsdevs.coffeescreen.screens.detailscreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bsdevs.coffeescreen.screens.detailscreen.ShotDto

@Composable
internal fun SecondHalfContent(onAddShotClicked: () -> Unit, shotList: List<ShotDto>?) {
    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                "Shot recordings",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            shotList?.let {
                LazyVerticalGrid(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    columns = GridCells.Fixed(1)
                )
                {
                    items(count = it.size, itemContent = { index ->
                        ShotCard(shot = it[index])
                    }
                    )
                }
            }
        }
        IconButton(
            modifier = Modifier
                .padding(bottom = 4.dp, end = 4.dp)
                .align(Alignment.BottomEnd),
            onClick = { onAddShotClicked() }) {
            Icon(
                Icons.Default.AddCircle, contentDescription = "Add New Shot", Modifier.size(32.dp)
            )
        }
    }
}

@Composable
internal fun ShotCard(shot: ShotDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "${shot.date}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.wrapContentWidth()
                )
                shot.rating?.let {
                    Row {
                        Text(
                            text = "${shot.rating}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Weight In: ${shot.weightIn} g",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Weight Out: ${shot.weightOut} g",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Time: ${shot.time} s",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}