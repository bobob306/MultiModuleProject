package com.bsdevs.babycare.presentation.babyhome

import com.bsdevs.data.NetworkScreenData

sealed class UiIntent {
    open class HomeUiIntent() : UiIntent()
}

sealed class HomeUiIntent : UiIntent() {
    data class CollapseHeader(val dateHeader: String) : HomeUiIntent()
    data class ToggleFilter(val activityType: String) : HomeUiIntent()
    data class EditActivityRow(val id: String, val activityType: String) : HomeUiIntent()
    data object LoadMore: HomeUiIntent()
}

data class BabyCareHomeViewData(
    val lastNappyChange: String? = null,
    val lastFeeding: String? = null,
    val activityFeed: List<NetworkScreenData> = emptyList(),
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val currentFilter: ActivityFilter = ActivityFilter.NONE,
    val collapsedHeaders: Set<String> = emptySet(),
)

enum class ActivityFilter { NONE, NAPPY, FEEDING }

sealed class BabyCareHomeNavigationEvent {
    data class NavigateToDeepLink(val uriString: String) : BabyCareHomeNavigationEvent()
}