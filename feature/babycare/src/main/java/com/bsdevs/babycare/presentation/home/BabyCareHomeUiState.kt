package com.bsdevs.babycare.presentation.home

import com.bsdevs.babycare.presentation.common.BabyActivity

sealed class HomeFeedItem {
    data class Header(val title: String, val feedingCount: Int, val nappyCount: Int) :
        HomeFeedItem()

    data class ActivityRow(val activity: BabyActivity) : HomeFeedItem()
}

data class BabyCareHomeViewData(
    val lastNappyChange: String? = null,
    val lastFeeding: String? = null,
    val activityFeed: List<HomeFeedItem> = emptyList(),
    val canLoadMore: Boolean = true,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val currentFilter: ActivityFilter = ActivityFilter.NONE,
    val collapsedHeaders: Set<String> = emptySet(),
)

enum class ActivityFilter { NONE, NAPPY, FEEDING }
