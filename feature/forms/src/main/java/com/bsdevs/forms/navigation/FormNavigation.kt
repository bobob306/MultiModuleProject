package com.bsdevs.forms.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.bsdevs.forms.presentation.FormScreen
import kotlinx.serialization.Serializable

@Serializable
data class FormRoute(val formId: String, val entityId: String? = null)

fun NavController.navigateToForm(
    formId: String,
    entityId: String? = null,
    navOptions: NavOptions? = null,
) = navigate(route = FormRoute(formId, entityId), navOptions = navOptions)

fun NavGraphBuilder.formSection(
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable<FormRoute> {
        FormScreen(onNavigate = onNavigate, onNavigateBack = onNavigateBack)
    }
}
