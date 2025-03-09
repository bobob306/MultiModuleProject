package com.bsdevs.renderer.components

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.bsdevs.data.ScreenData.SwitchData

@Composable
fun SwitchComponent(switchData: SwitchData, context: Context, onSwitchClick: (Boolean) -> Unit) {
    var checked by remember { mutableStateOf(switchData.checked) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(switchData.label)
        Spacer(Modifier.weight(weight = 1f))
        Switch(
            onCheckedChange = {
                checked = !checked
                onSwitchClick(checked)
            },
            checked = checked,
            modifier = Modifier,
            thumbContent = {
                if (checked) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            }
        )
    }
}

@Preview
@Composable
fun SwitchComponentPreview() {
    SwitchComponent(
        switchData = SwitchData(0, "Should I change this option", null, false),
        context = LocalContext.current,
        onSwitchClick = { _ -> },
    )
}