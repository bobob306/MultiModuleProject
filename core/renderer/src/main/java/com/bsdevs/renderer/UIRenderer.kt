package com.bsdevs.renderer

import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.bsdevs.data.ScreenData
import com.bsdevs.data.SpacerTypeData
import com.bsdevs.renderer.components.CardComponent

@Composable
fun ColumnScope.RenderUI(item: ScreenData, context: Context) {
    when (item) {
        is ScreenData.TitleData -> Text(item.content)
        is ScreenData.SubtitleData -> Text(item.content)
        is ScreenData.Unknown -> {}
        is ScreenData.SpacerData -> {
            if (item.size.type == SpacerTypeData.WEIGHT) {
                item.size.weight?.let { Spacer(modifier = Modifier.weight(it, fill = true)) }
            } else {
                item.size.height?.let { Modifier.size(it.dp) }?.let { Spacer(modifier = it) }
            }
        }
        is ScreenData.ImageData -> {
            AsyncImage(
                model = ImageRequest.Builder(context).data(item.url).build(),
                contentDescription = item.contentDescription,
                modifier = Modifier.size(item.height.dp, item.width.dp),
            )
        }
        is ScreenData.CardData -> CardComponent(cardData = item, context = context)
    }
}