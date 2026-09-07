package com.bsdevs.data.local

import androidx.room.TypeConverter
import com.bsdevs.network.dto.BabyDto
import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.dto.ShoppingListDto
import com.bsdevs.network.dto.UserDto
import com.bsdevs.network.dto.UnifiedEventDto
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MMPTypeConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @TypeConverter
    fun fromScreenDtoList(value: List<ScreenDto>): String = json.encodeToString(value)

    @TypeConverter
    fun toScreenDtoList(value: String): List<ScreenDto> = json.decodeFromString(value)

    @TypeConverter
    fun fromUserDto(value: UserDto): String = json.encodeToString(value)

    @TypeConverter
    fun toUserDto(value: String): UserDto = json.decodeFromString(value)

    @TypeConverter
    fun fromBabyDto(value: BabyDto): String = json.encodeToString(value)

    @TypeConverter
    fun toBabyDto(value: String): BabyDto = json.decodeFromString(value)

    @TypeConverter
    fun fromShoppingListDto(value: ShoppingListDto): String = json.encodeToString(value)

    @TypeConverter
    fun toShoppingListDto(value: String): ShoppingListDto = json.decodeFromString(value)

    @TypeConverter
    fun fromUnifiedEventDto(value: UnifiedEventDto): String = json.encodeToString(value)

    @TypeConverter
    fun toUnifiedEventDto(value: String): UnifiedEventDto = json.decodeFromString(value)
}
