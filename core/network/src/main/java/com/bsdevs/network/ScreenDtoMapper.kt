package com.bsdevs.network

import com.bsdevs.network.dto.ButtonType
import com.bsdevs.network.dto.LocationType
import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.dto.SizeDto
import com.bsdevs.network.dto.SpacerType
import javax.inject.Inject

interface ScreenDtoMapper : FirebaseMapper<HashMap<*, *>, List<ScreenDto>>

class ScreenDtoMapperImpl @Inject constructor() : ScreenDtoMapper {
    override fun mapToDto(map: HashMap<*, *>): List<ScreenDto> {
        println("map = $map")
        val listOfLists = map.map {
            val listedItems = it.value as List<HashMap<*, *>>
            println(("listedItems size = " + listedItems.size + map.toString()))
            listedItems.map { item ->
                println("type = " + item["type"])
                when (item["type"]) {
                    "CARD" -> {
                        val image = item["IMAGE"] as HashMap<*, *>
                        ScreenDto.CardDto(
                            index = item["index"].toString().toInt(),
                            title = item["title"] as String,
                            subtitle = item["subtitle"] as String,
                            backgroundColor = item["backgroundColor"].toString().toIntOrNull(),
                            image = ScreenDto.ImageDto(
                                index = image["index"].toString().toInt(),
                                url = image["url"] as String,
                                contentDescription = image["contentDescription"] as String,
                                height = image["height"].toString().toInt(),
                                width = image["width"].toString().toInt(),
                            ),
                        )
                    }

                    "IMAGE" -> {
                        ScreenDto.ImageDto(
                            index = item["index"].toString().toInt(),
                            url = item["url"] as String,
                            contentDescription = item["contentDescription"] as String,
                            height = item["height"].toString().toInt(),
                            width = item["width"].toString().toInt(),
                        )
                    }

                    "SMALL_TITLE" -> {
                        ScreenDto.SmallTitleDto(
                            index = item["index"].toString().toInt(),
                            content = item["content"] as String
                        )
                    }

                    "MEDIUM_TITLE" -> {
                        ScreenDto.MediumTitleDto(
                            index = item["index"].toString().toInt(),
                            content = item["content"] as String
                        )
                    }

                    "LARGE_TITLE" -> {
                        ScreenDto.LargeTitleDto(
                            index = item["index"].toString().toInt(),
                            content = item["content"] as String
                        )
                    }

                    "SUBTITLE" -> {
                        ScreenDto.SubtitleDto(
                            index = item["index"].toString().toInt(),
                            content = item["content"] as String
                        )
                    }

                    "SPACER" -> {
                        val sizeArray = item["size"] as? ArrayList<*>
                        val sizeObject = item["sizeobject"] as? HashMap<*, *>

                        val typeString = when {
                            sizeArray != null -> sizeArray[0].toString()
                            sizeObject != null -> sizeObject["type"].toString()
                            else -> "HEIGHT"
                        }

                        val resolvedSize = when {
                            sizeArray != null -> sizeArray[1].toString().toIntOrNull() ?: 16
                            sizeObject != null -> sizeObject["size"].toString().toIntOrNull() ?: 16
                            else -> 16
                        }

                        val resolvedWeight = when {
                            sizeArray != null && typeString == "WEIGHT" -> sizeArray[1].toString().toFloatOrNull()
                            sizeObject != null && typeString == "WEIGHT" -> sizeObject["weight"].toString().toFloatOrNull()
                            else -> null
                        }

                        ScreenDto.SpacerDto(
                            index = item["index"].toString().toIntOrNull() ?: 0,
                            size = SizeDto(
                                type = typeString.toSpacerType,
                                size = if (typeString == "HEIGHT") resolvedSize else null,
                                weight = resolvedWeight
                            )
                        )
                    }

                    "NAVIGATION_BUTTON" -> {
                        ScreenDto.NavigationButtonDto(
                            index = item["index"].toString().toInt(),
                            label = item["label"] as String,
                            destination = item["destination"] as String,
                            location = item["location"].toString().toLocationType,
                            sort = item["sort"].toString().toButtonType
                        )
                    }

                    else -> {
                        ScreenDto.Unknown(99)
                    }

                }
            }
        }
        val flattenedList = listOfLists.flatten().sortedBy { it.index }
        return flattenedList
    }

    private val String.toSpacerType: SpacerType
        get() = when (this) {
            "HEIGHT" -> SpacerType.HEIGHT
            "WEIGHT" -> SpacerType.WEIGHT
            else -> SpacerType.HEIGHT
        }

    private val String.toLocationType: LocationType
        get() = when (this) {
            "INTERNAL" -> LocationType.INTERNAL
            "EXTERNAL" -> LocationType.EXTERNAL
            else -> LocationType.INTERNAL
        }

    private val String.toButtonType: ButtonType
        get() = when (this) {
            "PRIMARY" -> ButtonType.PRIMARY
            "SECONDARY" -> ButtonType.SECONDARY
            "TERTIARY" -> ButtonType.TERTIARY
            else -> ButtonType.PRIMARY
        }
}
