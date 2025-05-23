package com.bsdevs.renderer

import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.SpacerTypeData
import com.bsdevs.renderer.components.CardComponent
import com.bsdevs.renderer.components.ChipComponent
import com.bsdevs.renderer.components.MMPButton
import com.bsdevs.renderer.components.SwitchComponent
import java.util.Locale

@Composable
fun ColumnScope.RenderUI(
    item: NetworkScreenData,
    context: Context,
    onClick: (String, String) -> Unit,
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
                item.size.weight?.let { Spacer(modifier = Modifier.weight(it, fill = true)) }
            } else {
                item.size.height?.let { Modifier.size(it.dp) }?.let { Spacer(modifier = it) }
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
                onClick = onClick,
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

        else -> {}
    }
}