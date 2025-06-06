package com.bsdevs.renderer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.bsdevs.data.ButtonTypeData.PRIMARY
import com.bsdevs.data.ButtonTypeData.SECONDARY
import com.bsdevs.data.ButtonTypeData.TERTIARY
import com.bsdevs.data.LocationTypeData
import com.bsdevs.data.NetworkScreenData.NavigationButtonDataNetwork

@Composable
fun MMPButton(navigationButtonData: NavigationButtonDataNetwork, onClick: (String, String) -> Unit) {
    navigationButtonData.run {
        when (sort) {
            PRIMARY -> {
                PrimaryButton(this, onClick = onClick)
            }

            SECONDARY -> {
                SecondaryButton(this, onClick = onClick)
            }

            TERTIARY -> {
                TertiaryButton(this, onClick = onClick)
            }
        }
    }
}

@Composable
fun PrimaryButton(navigationButtonData: NavigationButtonDataNetwork, onClick: (String, String) -> Unit) {
    navigationButtonData.run {
        Button(
            modifier = Modifier
                .wrapContentSize(),
            onClick = {
                onClick(destination, label)
            },
            content = {
                Text(label, color = MaterialTheme.colorScheme.onPrimary)
            },
        )
    }
}

@Composable
fun SecondaryButton(navigationButtonData: NavigationButtonDataNetwork, onClick: (String, String) -> Unit) {
    navigationButtonData.run {
        Button(
            colors = ButtonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                disabledContentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.3f),
            ),
            modifier = Modifier
                .wrapContentSize(),
            onClick = {
                onClick(destination, label)
            },
            content = {
                Text(
                    label,
                    modifier = Modifier.background(color = MaterialTheme.colorScheme.secondary),
                    color = MaterialTheme.colorScheme.onSecondary
                )
            },
        )
    }
}

@Composable
fun TertiaryButton(navigationButtonData: NavigationButtonDataNetwork, onClick: (String, String) -> Unit) {
    navigationButtonData.run {
        Text(
            text = label,
            textDecoration = TextDecoration.Underline,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .clickable { onClick(destination, label) }
                .background(color = MaterialTheme.colorScheme.onTertiary)
                .wrapContentSize(),
        )
    }
}

@Composable
@Preview
fun ButtonsPreview() {
    Column {
        MMPButton(
            navigationButtonData = NavigationButtonDataNetwork(
                label = "Click me",
                destination = "SECOND_SCREEN",
                location = LocationTypeData.INTERNAL,
                sort = PRIMARY,
                index = 1
            )
        ) { _, _ -> }
        MMPButton(
            navigationButtonData = NavigationButtonDataNetwork(
                label = "Click me",
                destination = "SECOND_SCREEN",
                location = LocationTypeData.INTERNAL,
                sort = SECONDARY,
                index = 1
            )
        ) { _, _ -> }
        MMPButton(
            navigationButtonData = NavigationButtonDataNetwork(
                label = "Click me",
                destination = "SECOND_SCREEN",
                location = LocationTypeData.INTERNAL,
                sort = TERTIARY,
                index = 1
            )
        ) { _, _ -> }

        TertiaryButton(
            navigationButtonData = NavigationButtonDataNetwork(
                label = "Click me",
                destination = "SECOND_SCREEN",
                location = LocationTypeData.INTERNAL,
                sort = TERTIARY,
                index = 1
            )
        ) { _, _ -> }

        SecondaryButton(
            navigationButtonData = NavigationButtonDataNetwork(
                label = "Click me",
                destination = "SECOND_SCREEN",
                location = LocationTypeData.INTERNAL,
                sort = SECONDARY,
                index = 1
            )
        ) { _, _ -> }

        PrimaryButton(
            navigationButtonData = NavigationButtonDataNetwork(
                label = "Click me",
                destination = "SECOND_SCREEN",
                location = LocationTypeData.INTERNAL,
                sort = PRIMARY,
                index = 1
            ),
            onClick = { _, _ -> }
        )
    }
}
