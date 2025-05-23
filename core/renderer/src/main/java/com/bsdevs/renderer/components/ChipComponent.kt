package com.bsdevs.renderer.components

import android.content.Context
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bsdevs.data.NetworkScreenData.ChipDataNetwork

@Composable
fun ChipComponent(chipData: ChipDataNetwork, context: Context, onClick: (Boolean) -> Unit) {
    var enabledState by remember { mutableStateOf(false) }
    FilterChip(
        onClick = {
            enabledState = !enabledState
            onClick(enabledState)
        },
        modifier = Modifier.wrapContentSize(),
        selected = enabledState,
        trailingIcon = {
            AsyncImage(
                model = ImageRequest.Builder(context).data(chipData.imageUrl).build(),
                contentDescription = "",
                modifier = Modifier.size(16.dp),
                placeholder = rememberVectorPainter(image = Icons.Default.Add)
            )
        },
        label = { Text(chipData.label) },
    )
}


@Preview
@Composable
fun ChipComponentPreview() {
    ChipComponent(
        chipData = ChipDataNetwork(0, "Chip", "https://picsum.photos/200"),
        context = LocalContext.current,
        onClick = { _ -> }
    )
}