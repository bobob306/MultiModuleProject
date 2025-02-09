package com.bsdevs.renderer

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bsdevs.data.ScreenData
import com.bsdevs.data.SpacerTypeData

@Composable
fun ColumnScope.RenderUI(item: ScreenData) {
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
    }
}