package com.bsdevs.data.local

import androidx.room.TypeConverter
import com.bsdevs.network.dto.BabyDto
import com.bsdevs.network.dto.FormSchemaDto
import com.bsdevs.network.dto.FormSubmissionDto
import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.dto.ShoppingListDto
import com.bsdevs.network.dto.UserDto
import com.bsdevs.network.dto.BabyEvent
import com.bsdevs.network.dto.CoffeeDto
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
    fun fromBabyEvent(value: BabyEvent): String = json.encodeToString(value)

    @TypeConverter
    fun toBabyEvent(value: String): BabyEvent = json.decodeFromString(value)

    @TypeConverter
    fun fromFormSchemaDto(value: FormSchemaDto): String = json.encodeToString(value)

    @TypeConverter
    fun toFormSchemaDto(value: String): FormSchemaDto = json.decodeFromString(value)

    @TypeConverter
    fun fromFormSubmissionDto(value: FormSubmissionDto): String = json.encodeToString(value)

    @TypeConverter
    fun toFormSubmissionDto(value: String): FormSubmissionDto = json.decodeFromString(value)

    @TypeConverter
    fun fromCoffeeDto(value: CoffeeDto): String = json.encodeToString(value)

    @TypeConverter
    fun toCoffeeDto(value: String): CoffeeDto = json.decodeFromString(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = json.encodeToString(value)

    @TypeConverter
    fun toStringList(value: String): List<String> = json.decodeFromString(value)
}
