package com.bsdevs.babycare

import com.bsdevs.babycare.network.FeedingDto
import com.bsdevs.babycare.network.NappyChangeDto

sealed class BabyActivity {
    abstract val id: String?
    abstract val date: String?
    abstract val time: String?

    data class Nappy(val dto: NappyChangeDto) : BabyActivity() {
        override val id: String? = dto.id
        override val date: String? = dto.date
        override val time: String? = dto.time
    }

    data class Feeding(val dto: FeedingDto) : BabyActivity() {
        override val id: String? = dto.id
        override val date: String? = dto.date
        override val time: String? = dto.startTime
    }

    val dateTimeString: String
        get() = "${date ?: ""}_${time ?: ""}"
}
