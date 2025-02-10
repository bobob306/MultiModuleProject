package com.bsdevs.renderer.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bsdevs.data.ScreenData

@Composable
fun CardComponent(cardData: ScreenData.CardData, context: Context) {
    cardData.run {
        Card(
            modifier = Modifier
                .wrapContentSize(),
            shape = RoundedCornerShape(corner = CornerSize(size = 16.dp)),
            border = BorderStroke(width = 2.dp, color = Color.LightGray),
            colors = CardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.3f),
            )
        ) {
            Column(
                modifier = Modifier.wrapContentWidth().padding(24.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(image.url).build(),
                        contentDescription = image.contentDescription,
                        contentScale = ContentScale.Inside,
                        modifier = Modifier
                            .background(color = Color.White, shape = CircleShape)
                            .size(56.dp)
                            .clip(shape = CircleShape)
                            .padding(8.dp)
                        ,
                    )
                    Text(text = title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.wrapContentWidth())
                }
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, modifier = Modifier.wrapContentWidth())
            }

        }
    }
}