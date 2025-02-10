package com.bsdevs.data

import com.bsdevs.network.DataMapper
import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.dto.SpacerType
import javax.inject.Inject

interface ScreenDataMapper : DataMapper<List<ScreenDto>, List<ScreenData>> {}

class ScreenDataMapperImpl @Inject constructor() : ScreenDataMapper {
    override fun mapToData(dto: List<ScreenDto>): List<ScreenData> {
        val listOfData = dto.map { item ->
            when (item) {
                is ScreenDto.SpacerDto -> ScreenData.SpacerData(
                    index = item.index,
                    size = SizeData(
                        type = when
                                       (item.size.type) {
                            SpacerType.HEIGHT -> SpacerTypeData.HEIGHT
                            SpacerType.WEIGHT -> SpacerTypeData.WEIGHT
                            else -> SpacerTypeData.HEIGHT
                        },
                        height = item.size.size ?: 0,
                        weight = item.size.weight ?: 0f
                    )
                )

                is ScreenDto.SubtitleDto -> ScreenData.SubtitleData(
                    index = item.index,
                    content = item.content
                )

                is ScreenDto.TitleDto -> ScreenData.TitleData(
                    index = item.index,
                    content = item.content
                )

                is ScreenDto.ImageDto -> ScreenData.ImageData(
                    index = item.index,
                    url = item.url,
                    contentDescription = item.contentDescription,
                    height = item.height,
                    width = item.width
                )

                is ScreenDto.CardDto -> ScreenData.CardData(
                    index = item.index,
                    image = ScreenData.ImageData(
                        index = item.index,
                        url = item.image.url,
                        contentDescription = item.image.contentDescription,
                        height = item.image.height,
                        width = item.image.width
                    ),
                    title = item.title,
                    subtitle = item.subtitle,
                    backgroundColor = item.backgroundColor
                )

                is ScreenDto.Unknown -> ScreenData.Unknown(99)
            }
        }
        return listOfData
    }
}