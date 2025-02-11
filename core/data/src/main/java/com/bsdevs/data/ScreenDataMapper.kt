package com.bsdevs.data

import com.bsdevs.network.DataMapper
import com.bsdevs.network.dto.ButtonType
import com.bsdevs.network.dto.LocationType
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

                is ScreenDto.ButtonDto -> ScreenData.ButtonData(
                    index = item.index,
                    label = item.label,
                    destination = item.destination,
                    location = item.location.toLocationTypeData,
                    sort = item.sort.toButtonTypeData,
                )
            }
        }
        return listOfData
    }

    private val LocationType?.toLocationTypeData: LocationTypeData
        get() = when (this) {
            LocationType.INTERNAL -> LocationTypeData.INTERNAL
            LocationType.EXTERNAL -> LocationTypeData.EXTERNAL
            else -> LocationTypeData.INTERNAL
        }

    private val ButtonType?.toButtonTypeData
        get() = when (this) {
            ButtonType.PRIMARY -> ButtonTypeData.PRIMARY
            ButtonType.SECONDARY -> ButtonTypeData.SECONDARY
            ButtonType.TERTIARY -> ButtonTypeData.TERTIARY
            else -> ButtonTypeData.PRIMARY
        }
}