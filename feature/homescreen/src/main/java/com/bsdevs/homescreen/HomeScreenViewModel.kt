package com.bsdevs.homescreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.common.result.Result
import com.bsdevs.network.ScreenDto
import com.bsdevs.network.ScreenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed class HomeScreenData(
    open val index: Int
) {
    data class Unknown(override val index: Int) : HomeScreenData(index)

    data class TitleData(
        override val index: Int,
        val content: String,
    ) : HomeScreenData(index)

    data class SubtitleData(
        override val index: Int,
        val content: String,
    ) : HomeScreenData(index)

    data class SpacerData(
        override val index: Int,
        val size: SizeData,
    ) : HomeScreenData(index)
}

data class SizeData(
    val type: String,
    val height: Int,
)

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val repository: ScreenRepository
) : ViewModel() {

    private val _viewData = MutableStateFlow<Result<List<ScreenDto>>>(value = Result.Loading)
    val viewData: StateFlow<Result<List<ScreenDto>>> get() = _viewData


    fun getScreen() {
        viewModelScope.launch {
            val result = repository.getScreenFlow("home")
                ?.let { Result.Success(it) }
                ?: Result.Error(Exception("Error"))
            _viewData.update { result }
//            repository.getScreen("home")
//                .addOnSuccessListener { doc ->
//                    doc?.let { data ->
//                        val list = data.data as HashMap
//                        _viewData.update {
//                            Result.Success(mapToHomeScreenData(list))
//                        }
//                    }
//                }
//                .addOnFailureListener {
//                    println(it.message)
//                }
        }
    }

    private fun mapToHomeScreenData(list: HashMap<*, *>): List<HomeScreenData> {
        val listOfLists = list.map {
            val listedItems = it.value as List<HashMap<*, *>>
            listedItems.map { item ->
                println("type = " + item["type"])
                when (item["type"]) {
                    "TITLE" -> {
                        HomeScreenData.TitleData(
                            index = item["index"].toString().toInt(),
                            content = item["content"] as String
                        )
                    }

                    "SUBTITLE" -> {
                        HomeScreenData.SubtitleData(
                            index = item["index"].toString().toInt(),
                            content = item["content"] as String
                        )
                    }

                    "SPACER" -> {
                        val size = item["size"] as ArrayList<*>
                        val type = size[0].toString()
                        val height = size[1].toString().toInt()
                        HomeScreenData.SpacerData(
                            index = item["index"].toString().toInt(),
                            size = SizeData(
                                type = type,
                                height = height,
                            )
                        )
                    }

                    else -> {
                        HomeScreenData.Unknown(99)
                    }
                }
            }
        }
        val flattenedList = listOfLists.flatten()
        return flattenedList
    }
}