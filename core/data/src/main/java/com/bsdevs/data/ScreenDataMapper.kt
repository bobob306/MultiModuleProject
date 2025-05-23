package com.bsdevs.data

import com.bsdevs.network.DataMapper
import com.bsdevs.network.dto.ButtonType
import com.bsdevs.network.dto.LocationType
import com.bsdevs.network.dto.ScreenDto
import com.bsdevs.network.dto.SpacerType
import javax.inject.Inject

interface ScreenDataMapper : DataMapper<List<ScreenDto>, List<NetworkScreenData>> {}

class ScreenDataMapperImpl @Inject constructor() : ScreenDataMapper {
    override fun mapToData(dto: List<ScreenDto>): List<NetworkScreenData> {
        val listOfData = dto.map { item ->
            when (item) {
                is ScreenDto.SpacerDto -> NetworkScreenData.SpacerDataNetwork(
                    index = item.index,
                    size = SizeData(
                        type = when (item.size.type) {
                            SpacerType.HEIGHT -> SpacerTypeData.HEIGHT
                            SpacerType.WEIGHT -> SpacerTypeData.WEIGHT
                            else -> SpacerTypeData.HEIGHT
                        },
                        height = item.size.size ?: 0,
                        weight = item.size.weight ?: 0f
                    )
                )

                is ScreenDto.SubtitleDto -> NetworkScreenData.SubtitleDataNetwork(
                    index = item.index,
                    content = item.content
                )

                is ScreenDto.TitleDto -> NetworkScreenData.TitleDataNetwork(
                    index = item.index,
                    content = item.content
                )

                is ScreenDto.ImageDto -> NetworkScreenData.ImageDataNetwork(
                    index = item.index,
                    url = item.url,
                    contentDescription = item.contentDescription,
                    height = item.height,
                    width = item.width
                )

                is ScreenDto.CardDto -> NetworkScreenData.CardDataNetwork(
                    index = item.index,
                    image = NetworkScreenData.ImageDataNetwork(
                        index = item.index,
                        url = item.image.url,
                        contentDescription = item.image.contentDescription,
                        height = item.image.height,
                        width = item.image.width
                    ),
                    title = item.title,
                    subtitle = item.subtitle,
                    backgroundColor = item.backgroundColor,
                    sort = null,
                    buttonRow = null,
                    subheading = null,
                    iconButtonRow = null,
                )

                is ScreenDto.Unknown -> NetworkScreenData.Unknown(99)

                is ScreenDto.NavigationButtonDto -> NetworkScreenData.NavigationButtonDataNetwork(
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