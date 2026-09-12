package com.bsdevs.network

import com.bsdevs.network.dto.ButtonType
import com.bsdevs.network.dto.LocationType
import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.dto.SizeDto
import com.bsdevs.network.dto.SpacerType
import javax.inject.Inject

interface ScreenDtoMapper : FirebaseMapper<HashMap<*, *>, List<ScreenDto>> {
    fun mapToFirebase(dtos: List<ScreenDto>): Map<String, Any?>
}

class ScreenDtoMapperImpl @Inject constructor() : ScreenDtoMapper {
    override fun mapToFirebase(dtos: List<ScreenDto>): Map<String, Any?> {
        val components = dtos.map { dto ->
            when (dto) {
                is ScreenDto.CardDto -> mapOf(
                    "type" to "CARD",
                    "index" to dto.index,
                    "title" to dto.title,
                    "subtitle" to dto.subtitle,
                    "backgroundColor" to dto.backgroundColor,
                    "requiredRoles" to dto.requiredRoles,
                    "IMAGE" to mapOf(
                        "index" to dto.image.index,
                        "url" to dto.image.url,
                        "contentDescription" to dto.image.contentDescription,
                        "height" to dto.image.height,
                        "width" to dto.image.width,
                        "requiredRoles" to dto.image.requiredRoles
                    )
                )
                is ScreenDto.ImageDto -> mapOf(
                    "type" to "IMAGE",
                    "index" to dto.index,
                    "url" to dto.url,
                    "contentDescription" to dto.contentDescription,
                    "height" to dto.height,
                    "width" to dto.width,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.TitleDto -> mapOf(
                    "type" to "TITLE",
                    "index" to dto.index,
                    "content" to dto.content,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.SubtitleDto -> mapOf(
                    "type" to "SUBTITLE",
                    "index" to dto.index,
                    "content" to dto.content,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.SpacerDto -> mapOf(
                    "type" to "SPACER",
                    "index" to dto.index,
                    "size" to arrayListOf(
                        dto.size.type.name,
                        if (dto.size.type == SpacerType.HEIGHT) dto.size.size else dto.size.weight
                    ),
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.NavigationButtonDto -> mapOf(
                    "type" to "NAVIGATION_BUTTON",
                    "index" to dto.index,
                    "label" to dto.label,
                    "destination" to dto.destination,
                    "location" to dto.location?.name,
                    "sort" to dto.sort?.name,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.SmallTitleDto -> mapOf(
                    "type" to "SMALL_TITLE",
                    "index" to dto.index,
                    "content" to dto.content,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.ActivityFeedDto -> mapOf(
                    "type" to "ACTIVITY_FEED",
                    "index" to dto.index,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.TileRowDto -> mapOf(
                    "type" to "TILE_ROW",
                    "index" to dto.index,
                    "tiles" to dto.tiles.map { tile ->
                        mapOf(
                            "index" to tile.index,
                            "title" to tile.title,
                            "iconName" to tile.iconName,
                            "destination" to tile.destination,
                            "subtitleType" to tile.subtitleType,
                            "sharedElementKey" to tile.sharedElementKey,
                            "requiredRoles" to tile.requiredRoles
                        )
                    },
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.GrowthChartDto -> mapOf(
                    "type" to "GROWTH_CHART",
                    "index" to dto.index,
                    "title" to dto.title,
                    "dataType" to dto.dataType,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.MeasurementHistoryDto -> mapOf(
                    "type" to "MEASUREMENT_HISTORY",
                    "index" to dto.index,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.VaccinationHistoryDto -> mapOf(
                    "type" to "VACCINATION_HISTORY",
                    "index" to dto.index,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.TemperatureHistoryDto -> mapOf(
                    "type" to "TEMPERATURE_HISTORY",
                    "index" to dto.index,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.TemperatureChartDto -> mapOf(
                    "type" to "TEMPERATURE_CHART",
                    "index" to dto.index,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.FeedingFrequencyChartDto -> mapOf(
                    "type" to "FEEDING_FREQUENCY_CHART",
                    "index" to dto.index,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.FeedingGapChartDto -> mapOf(
                    "type" to "FEEDING_GAP_CHART",
                    "index" to dto.index,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.FeedingInsightCardDto -> mapOf(
                    "type" to "FEEDING_INSIGHT_CARD",
                    "index" to dto.index,
                    "requiredRoles" to dto.requiredRoles
                )
                is ScreenDto.ShoppingListDto -> mapOf(
                    "type" to "SHOPPING_LIST",
                    "index" to dto.index,
                    "requiredRoles" to dto.requiredRoles
                )
                else -> emptyMap<String, Any?>()
            }
        }
        // By default, we'll put everything under a "components" key to keep it simple
        // since the current mapper just flattens everything anyway.
        return mapOf("components" to components)
    }

    override fun mapToDto(map: HashMap<*, *>): List<ScreenDto> {
        val listOfLists = map.map {
            val listedItems = it.value as List<HashMap<*, *>>
            listedItems.map { item ->
                when (item["type"]) {
                    "CARD" -> {
                        val image = item["IMAGE"] as HashMap<*, *>
                        ScreenDto.CardDto(
                            index = item["index"].toString().toInt(),
                            title = item["title"] as String,
                            subtitle = item["subtitle"] as String,
                            backgroundColor = item["backgroundColor"].toString().toIntOrNull(),
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>(),
                            image = ScreenDto.ImageDto(
                                index = image["index"].toString().toInt(),
                                url = image["url"] as String,
                                contentDescription = image["contentDescription"] as String,
                                height = image["height"].toString().toInt(),
                                width = image["width"].toString().toInt(),
                                requiredRoles = (image["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
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
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "TITLE" -> {
                        ScreenDto.TitleDto(
                            index = item["index"].toString().toInt(),
                            content = item["content"] as String,
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "SUBTITLE" -> {
                        ScreenDto.SubtitleDto(
                            index = item["index"].toString().toInt(),
                            content = item["content"] as String,
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "SPACER" -> {
                        val size = item["size"] as ArrayList<*>
                        val type = size[0].toString()
                        ScreenDto.SpacerDto(
                            index = item["index"].toString().toInt(), size = SizeDto(
                                type = type.toSpacerType,
                                size = if (type == "HEIGHT") size[1].toString().toInt() else null,
                                weight = if (type == "WEIGHT") size[1].toString()
                                    .toFloat() else null,
                            ),
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "NAVIGATION_BUTTON" -> {
                        ScreenDto.NavigationButtonDto(
                            index = item["index"].toString().toInt(),
                            label = item["label"] as String,
                            destination = item["destination"] as String,
                            location = item["location"].toString().toLocationType,
                            sort = item["sort"].toString().toButtonType,
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "SMALL_TITLE" -> {
                        ScreenDto.SmallTitleDto(
                            index = item["index"].toString().toInt(),
                            content = item["content"] as String,
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "ACTIVITY_FEED" -> {
                        ScreenDto.ActivityFeedDto(
                            index = item["index"].toString().toInt(),
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "TILE_ROW" -> {
                        val tiles = (item["tiles"] as? List<HashMap<*, *>>)?.map { tileMap ->
                            ScreenDto.TileDto(
                                index = tileMap["index"].toString().toInt(),
                                title = tileMap["title"] as String,
                                iconName = tileMap["iconName"] as String,
                                destination = tileMap["destination"] as String,
                                subtitleType = tileMap["subtitleType"] as? String,
                                sharedElementKey = tileMap["sharedElementKey"] as? String,
                                requiredRoles = (tileMap["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                            )
                        } ?: emptyList()
                        ScreenDto.TileRowDto(
                            index = item["index"].toString().toInt(),
                            tiles = tiles,
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "GROWTH_CHART" -> {
                        ScreenDto.GrowthChartDto(
                            index = item["index"].toString().toInt(),
                            title = item["title"] as String,
                            dataType = item["dataType"] as String,
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "MEASUREMENT_HISTORY" -> {
                        ScreenDto.MeasurementHistoryDto(
                            index = item["index"].toString().toInt(),
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "VACCINATION_HISTORY" -> {
                        ScreenDto.VaccinationHistoryDto(
                            index = item["index"].toString().toInt(),
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "TEMPERATURE_HISTORY" -> {
                        ScreenDto.TemperatureHistoryDto(
                            index = item["index"].toString().toInt(),
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "TEMPERATURE_CHART" -> {
                        ScreenDto.TemperatureChartDto(
                            index = item["index"].toString().toInt(),
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "FEEDING_FREQUENCY_CHART" -> {
                        ScreenDto.FeedingFrequencyChartDto(
                            index = item["index"].toString().toInt(),
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "FEEDING_GAP_CHART" -> {
                        ScreenDto.FeedingGapChartDto(
                            index = item["index"].toString().toInt(),
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "FEEDING_INSIGHT_CARD" -> {
                        ScreenDto.FeedingInsightCardDto(
                            index = item["index"].toString().toInt(),
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    "SHOPPING_LIST" -> {
                        ScreenDto.ShoppingListDto(
                            index = item["index"].toString().toInt(),
                            requiredRoles = (item["requiredRoles"] as? List<*>)?.filterIsInstance<String>()
                        )
                    }

                    else -> {
                        ScreenDto.Unknown(99, null)
                    }

                }
            }
        }
        val flattenedList = listOfLists.flatten()
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
