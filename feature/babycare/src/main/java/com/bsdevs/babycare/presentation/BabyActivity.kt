package com.bsdevs.babycare.presentation

import com.bsdevs.babycare.network.FeedingDto
import com.bsdevs.babycare.network.NappyChangeDto

sealed class BabyActivity {
    abstract val id: String?
    abstract val date: String?
    abstract val time: String?
    abstract val dateTime: String
    abstract val comment: String?

    data class Nappy(val dto: NappyChangeDto) : BabyActivity() {
        override val id: String? = dto.id
        override val date: String? = dto.date
        override val time: String? = dto.time
        override val dateTime: String = dto.dateTime
        override val comment: String? = dto.comment
    }

    data class Feeding(val dto: FeedingDto) : BabyActivity() {
        override val id: String? = dto.id
        override val date: String? = dto.date
        override val time: String? = dto.startTime
        override val dateTime: String = dto.dateTime
        override val comment: String? = dto.comment
    }

    val dateTimeString: String
        get() = "${date ?: ""}_${time ?: ""}"
}
