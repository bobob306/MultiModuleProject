package com.bsdevs.babycare.presentation.nappy

sealed class NappyChangeEvent {
    object SaveSuccess : NappyChangeEvent()
    object DeleteSuccess : NappyChangeEvent()
    data class SaveError(val message: String) : NappyChangeEvent()
}
