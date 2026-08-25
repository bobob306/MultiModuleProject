package com.bsdevs.babycare.presentation.feeding

sealed class FeedingEvent {
    object SaveSuccess : FeedingEvent()
    data class SaveError(val message: String) : FeedingEvent()
}
