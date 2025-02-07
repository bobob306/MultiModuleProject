package com.bsdevs.network

import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.dto.SizeDto
import com.bsdevs.network.dto.SpacerType
import javax.inject.Inject

interface ScreenDtoMapper : FirebaseMapper<HashMap<*, *>, List<ScreenDto>>

class ScreenDtoMapperImpl @Inject constructor() : ScreenDtoMapper {
    override fun mapToDto(map: HashMap<*, *>): List<ScreenDto> {
        val listOfLists = map.map {
            val listedItems = it.value as List<HashMap<*, *>>
            listedItems.map { item ->
                println("type = " + item["type"])
                when (item["type"]) {
                    "TITLE" -> {
                        ScreenDto.TitleDto(
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
                        val size = item["size"] as ArrayList<*>
                        val type = size[0].toString()
                        ScreenDto.SpacerDto(
                            index = item["index"].toString().toInt(), size = SizeDto(
                                type = type.toSpacerType,
                                size = if (type == "HEIGHT") size[1].toString().toInt() else null,
                                weight = if (type == "WEIGHT") size[1].toString()
                                    .toFloat() else null,
                            )
                        )
                    }

                    else -> {
                        ScreenDto.Unknown(99)
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
}
