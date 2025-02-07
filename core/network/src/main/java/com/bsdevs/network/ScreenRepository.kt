package com.bsdevs.network

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import jakarta.inject.Inject
import kotlinx.coroutines.tasks.await

sealed class ScreenDto(
    open val index: Int
) {
    data class Unknown(override val index: Int) : ScreenDto(index)
    data class TitleDto(
        override val index: Int,
        val content: String,
    ) : ScreenDto(index)

    data class SubtitleDto(
        override val index: Int,
        val content: String,
    ) : ScreenDto(index)

    data class SpacerDto(
        override val index: Int,
        val size: SizeDto,
    ) : ScreenDto(index)
}

data class SizeDto(
    val type: SpacerType,
    val size: Int? = null,
    val weight: Float? = null,
)

enum class SpacerType {
    HEIGHT, WEIGHT,
}

interface ScreenRepository {
    suspend fun getScreen(screen: String): Task<DocumentSnapshot>

    suspend fun getScreenFlow(screen: String): List<ScreenDto>?
}

class ScreenRepositoryImpl @Inject constructor(
    private val scr: CollectionReference
) : ScreenRepository {
    private var cache: Task<DocumentSnapshot>? = null
    private var cacheFlow: List<ScreenDto>? = null

    override suspend fun getScreen(screen: String): Task<DocumentSnapshot> {
        return if (cache != null) {
            cache as Task<DocumentSnapshot>
        } else {
            cache = scr.document(screen).get()
            return cache as Task<DocumentSnapshot>
        }
    }

    override suspend fun getScreenFlow(screen: String): List<ScreenDto>? {
        if (cacheFlow != null) {
            cacheFlow
        } else {
            try {
                val document = scr.document(screen).get().await().data
                cacheFlow = map(document as HashMap)
            } catch (e: Exception) {
                println(e.message)
                cacheFlow = null
            }
        }

        return cacheFlow
    }

    private fun map(data: HashMap<*, *>): List<ScreenDto> {
        val listOfLists = data.map {
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