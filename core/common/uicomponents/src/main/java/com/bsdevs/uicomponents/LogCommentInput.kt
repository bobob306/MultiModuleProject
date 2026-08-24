package com.bsdevs.uicomponents

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LogCommentInput(
    comment: String,
    onCommentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = comment,
        onValueChange = onCommentChange,
        label = { Text("Notes / Comments") },
        placeholder = { Text("e.g., Spit up a little, extra fussy, etc.") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Create,
                contentDescription = "Comment icon"
            )
        },
        modifier = modifier
            .fillMaxWidth(),
        maxLines = 3,
        minLines = 2
    )
}