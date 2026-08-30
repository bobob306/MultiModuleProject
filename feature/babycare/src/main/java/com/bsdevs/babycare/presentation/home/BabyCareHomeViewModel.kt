package com.bsdevs.babycare.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.babycare.network.DailyLogDto
import com.bsdevs.babycare.network.FeedingDto
import com.bsdevs.babycare.network.MeasurementDto
import com.bsdevs.babycare.network.NappyChangeDto
import com.bsdevs.babycare.network.TemperatureDto
import com.bsdevs.babycare.network.UnifiedEventDto
import com.bsdevs.babycare.presentation.common.BabyActivity
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.network.repository.ScreenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class BabyCareHomeViewModel @Inject constructor(
    private val repository: BabyCareRepository,
    private val accountService: AccountService,
    private val screenRepository: ScreenRepository,
    private val mapper: ScreenDataMapper,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val pageSize = 20

    // Internal trackers for configuration states
    private val _currentFilter = MutableStateFlow(ActivityFilter.NONE)
    private val _collapsedHeaders = MutableStateFlow<Set<String>>(emptySet())
    private val _dynamicUi = MutableStateFlow<List<NetworkScreenData>>(emptyList())

    private val _viewData = MutableStateFlow<Result<BabyCareHomeViewData>>(Result.Loading)
    val viewData: StateFlow<Result<BabyCareHomeViewData>> = _viewData.asStateFlow()

    init {
        // 🌟 2. Observe the repository cache in the background to handle instant updates
        // without letting empty states lock up our initialization pipeline
        viewModelScope.launch {
            repository.cachedDays.collect { dailyLogs ->
                // Only map to Success if we aren't currently waiting on a full initial pull
                if (_viewData.value !is Result.Loading || dailyLogs.isNotEmpty()) {
                    updateDisplayFeed(dailyLogs)
                }
            }
        }

        // Trigger initial data load immediately on launch
        initialLoad()
    }

    private fun initialLoad() {
        viewModelScope.launch {
            // Load dynamic UI config from Firebase
            launch {
                screenRepository.getScreenFlow("baby_home").collect { result ->
                    if (result is Result.Success) {
                        val mappedData = withContext(dispatchers.default) {
                            mapper.mapToData(result.data)
                        }
                        _dynamicUi.value = mappedData
                        // Trigger update to include dynamic UI in state if already initialized
                        if (_viewData.value is Result.Success) {
                            updateDisplayFeed(repository.cachedDays.value)
                        }
                    }
                }
            }

            try {
                // Fetch the current month document from Firestore
                val fetchResult = repository.loadInitialData(accountService.currentUserId, pageSize)

                // Force switch the state to success immediately, even if the month is brand new/empty
                updateDisplayFeed(
                    dailyLogs = repository.cachedDays.value,
                    canLoadMore = fetchResult.hasMoreData,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                Log.e("HOME_INIT_ERROR", "Failed initial data block fetch", e)
                _viewData.value = Result.Error(e)
            }
        }
    }

    private suspend fun updateDisplayFeed(
        dailyLogs: List<DailyLogDto>,
        canLoadMore: Boolean? = null,
        isRefreshing: Boolean? = null,
        isLoadingMore: Boolean? = null
    ) {
        val currentState = (_viewData.value as? Result.Success)?.data
        val processedFeed = processFeed(
            dailyLogs = dailyLogs,
            filter = _currentFilter.value,
            collapsed = _collapsedHeaders.value
        )
        _viewData.value = Result.Success(
            processedFeed.copy(
                isRefreshing = isRefreshing ?: currentState?.isRefreshing ?: false,
                isLoadingMore = isLoadingMore ?: currentState?.isLoadingMore ?: false,
                canLoadMore = canLoadMore ?: currentState?.canLoadMore ?: true
            )
        )
    }

    fun refreshData() {
        val current = when (val currentState = _viewData.value) {
            is Result.Success -> currentState.data
            else -> BabyCareHomeViewData(isRefreshing = true)
        }

        if (current.isLoadingMore) return

        viewModelScope.launch {
            // Turn on pull-to-refresh spinner indicator
            _viewData.value = Result.Success(current.copy(isRefreshing = true))

            // Refresh Screen Config too
            launch {
                screenRepository.getScreenFlow("baby_home", forceRefresh = true).collect { result ->
                    if (result is Result.Success) {
                        val mappedData = withContext(dispatchers.default) {
                            mapper.mapToData(result.data)
                        }
                        _dynamicUi.value = mappedData
                    }
                }
            }

            try {
                val refreshResult = repository.refreshData(accountService.currentUserId, pageSize)
                updateDisplayFeed(
                    dailyLogs = repository.cachedDays.value,
                    canLoadMore = refreshResult.hasMoreData,
                    isRefreshing = false
                )
            } catch (exception: Exception) {
                Log.e("REFRESH_ERROR", "Failed to clear refresh cycle", exception)
                _viewData.value = Result.Success(current.copy(isRefreshing = false))
            }
        }
    }

    fun loadMore() {
        val currentResult = _viewData.value
        // 🛡️ Safeguard: check that we are in a stable Success state first
        if (currentResult !is Result.Success || currentResult.data.isLoadingMore || !currentResult.data.canLoadMore) return

        val visibleRowsCount =
            currentResult.data.activityFeed.count { it is HomeFeedItem.ActivityRow }

        // If the user has collapsed everything, stop automatic background network triggers
        if (visibleRowsCount == 0) return

        viewModelScope.launch {
            setLoadingMoreState(true)
            try {
                // 🔄 Trigger your clean day-block repository pagination method
                val result = repository.loadMoreData(accountService.currentUserId, pageSize)

                // Update the loading flags based on the returned repository signals
                _viewData.update { current ->
                    if (current is Result.Success) {
                        Result.Success(
                            current.data.copy(
                                canLoadMore = result.hasMoreData,
                                isLoadingMore = false
                            )
                        )
                    } else {
                        current
                    }
                }
            } catch (e: Exception) {
                setLoadingMoreState(false)
            }
        }
    }

    private suspend fun processFeed(
        dailyLogs: List<DailyLogDto>,
        filter: ActivityFilter,
        collapsed: Set<String>
    ): BabyCareHomeViewData = withContext(dispatchers.default) {
        val finalizedFeed = mutableListOf<HomeFeedItem>()

        // 🌟 IMPROVED: Find absolute latest readings across all cached logs, not just today
        val allEventsFlattened = dailyLogs.flatMap { day ->
            day.events.sortedByDescending { it.dateTimeString }
        }

        val absoluteLastNappy = allEventsFlattened.firstOrNull {
            it.type == "NAPPY" || it.type == "Wet" || it.type == "Dirty" || it.type == "Both"
        }?.let { "Last nappy: ${it.time}" }

        val absoluteLastFeeding = allEventsFlattened.firstOrNull {
            it.type == "FEEDING"
        }?.let { "Last feed: ${it.time}" }

        val lastTempEvent = allEventsFlattened.firstOrNull {
            it.type == "TEMPERATURE" && it.temperature != null && it.temperature != 0.0
        }

        val absoluteLastTemperature = lastTempEvent?.let {
            "Last temp: ${it.temperature}°C"
        }

        val lastMeasurementEvent = allEventsFlattened.firstOrNull {
            it.type == "MEASUREMENT" && (it.weight != null || it.height != null)
        }

        val absoluteLastMeasurement = lastMeasurementEvent?.let {
            val weight = it.weight?.let { w -> String.format(Locale.getDefault(), "%.2fkg", w) } ?: ""
            val height = it.height?.let { h -> String.format(Locale.getDefault(), "%.1fcm", h) } ?: ""
            "Last: $weight $height".trim()
        }

        dailyLogs.forEach { dayLog ->

            // 🌟 FIXED: Force all nested events for this calendar day to sort by time (newest first)
            val sortedDayEvents = dayLog.events.sortedByDescending { it.dateTimeString }

            // 🔄 Apply active filter rules onto the cleanly sorted list array instead of the raw one
            val visibleEvents = sortedDayEvents.filter { event ->
                when (filter) {
                    ActivityFilter.NONE -> true
                    ActivityFilter.NAPPY -> event.type == "NAPPY" || event.type == "Wet" || event.type == "Dirty" || event.type == "Both"
                    ActivityFilter.FEEDING -> event.type == "FEEDING"
                    ActivityFilter.TEMPERATURE -> event.type == "TEMPERATURE"
                    ActivityFilter.MEASUREMENT -> event.type == "MEASUREMENT"
                }
            }

            val feedingCount = visibleEvents.count { it.type == "FEEDING" }
            val nappyCount =
                visibleEvents.count { it.type == "NAPPY" || it.type == "Wet" || it.type == "Dirty" || it.type == "Both" }
            val temperatureCount = visibleEvents.count { it.type == "TEMPERATURE" }
            val measurementCount = visibleEvents.count { it.type == "MEASUREMENT" }
            val displayHeaderTitle = formatHeaderDate(dayLog.date)

            finalizedFeed.add(
                HomeFeedItem.Header(
                    displayHeaderTitle,
                    feedingCount,
                    nappyCount,
                    temperatureCount,
                    measurementCount
                )
            )

            if (!collapsed.contains(displayHeaderTitle)) {
                visibleEvents.forEach { unifiedEvent ->
                    val babyActivityModel = mapToBabyActivity(unifiedEvent, dayLog.date)
                    finalizedFeed.add(HomeFeedItem.ActivityRow(babyActivityModel))
                }
            }
        }

        BabyCareHomeViewData(
            lastNappyChange = absoluteLastNappy,
            lastFeeding = absoluteLastFeeding,
            lastTemperature = absoluteLastTemperature,
            lastMeasurement = absoluteLastMeasurement,
            activityFeed = finalizedFeed,
            dynamicUi = _dynamicUi.value,
            currentFilter = filter,
            collapsedHeaders = collapsed
        )
    }


    private fun mapToBabyActivity(event: UnifiedEventDto, parentDate: String): BabyActivity {
        // 🔄 Fix 1: Extract the "HH:mm" time segment dynamically from the dateTimeString if the time field is blank
        val extractedTime = if (event.time.isNotEmpty()) {
            event.time
        } else {
            event.dateTimeString.split(" ").getOrNull(1) ?: ""
        }

        // 🔄 Fix 2: If the type field was corrupted (e.g., set to "Wet"), recognize it as a nappy activity
        val isNappy =
            event.type == "NAPPY" || event.type == "Wet" || event.type == "Dirty" || event.type == "Both"
        val isTemperature = event.type == "TEMPERATURE"
        val isMeasurement = event.type == "MEASUREMENT"

        return if (isNappy) {
            // Fallback: If nappyType is missing because it was saved under 'type', recover it here
            val correctedNappyType =
                if (!event.nappyType.isNullOrEmpty()) event.nappyType else event.type

            BabyActivity.Nappy(
                NappyChangeDto(
                    id = event.id,
                    date = parentDate,
                    time = extractedTime, // ✨ Time is now safely populated
                    dateTime = event.dateTimeString,
                    type = correctedNappyType,
                    comment = event.comment,
                )
            )
        } else if (isTemperature) {
            BabyActivity.Temperature(
                TemperatureDto(
                    id = event.id,
                    date = parentDate,
                    time = extractedTime,
                    dateTime = event.dateTimeString,
                    temperature = event.temperature ?: 37.0,
                    comment = event.comment
                )
            )
        } else if (isMeasurement) {
            BabyActivity.Measurement(
                MeasurementDto(
                    id = event.id,
                    date = parentDate,
                    time = extractedTime,
                    dateTime = event.dateTimeString,
                    height = event.height,
                    weight = event.weight,
                    isMedical = event.isMedical ?: false,
                    comment = event.comment
                )
            )
        } else {
            BabyActivity.Feeding(
                FeedingDto(
                    id = event.id,
                    date = parentDate,
                    startTime = extractedTime,
                    dateTime = event.dateTimeString,
                    mainFeedingSide = event.mainFeedingSide,
                    leftDuration = event.leftDuration,
                    rightDuration = event.rightDuration,
                    totalDuration = event.totalDuration,
                    bottleAmountMl = event.bottleAmountMl,
                    comment = event.comment
                )
            )
        }
    }

    private fun formatHeaderDate(dateString: String): String {
        return try {
            // Safe check: extract just the YYYY-MM-DD segment if it contains time info
            val cleanDateStr = dateString.split(" ").firstOrNull() ?: dateString
            val targetDate = LocalDate.parse(cleanDateStr)
            val today = LocalDate.now()

            when (targetDate) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> {
                    val yearShort = targetDate.year.toString().substring(2)
                    val month = String.format(Locale.getDefault(), "%02d", targetDate.monthValue)
                    val day = String.format(Locale.getDefault(), "%02d", targetDate.dayOfMonth)
                    "$day $month $yearShort"
                }
            }
        } catch (e: Exception) {
            dateString // Fallback safety representation if parsing strings fails
        }
    }

    fun toggleHeaderCollapse(title: String) {
        _collapsedHeaders.update { current ->
            if (current.contains(title)) current - title else current + title
        }
        // 🌟 FORCE UI REFRESH: Immediately push the recalculated state to the screen
        viewModelScope.launch {
            updateDisplayFeed(repository.cachedDays.value)
        }
    }

    fun toggleActivityFilter(filter: ActivityFilter) {
        _currentFilter.update { current ->
            if (current == filter) ActivityFilter.NONE else filter
        }
        // 🌟 FORCE UI REFRESH: Immediately push the filtered state to the screen
        viewModelScope.launch {
            updateDisplayFeed(repository.cachedDays.value)
        }
    }

    private fun setLoadingMoreState(value: Boolean) {
        val currentResult = _viewData.value
        if (currentResult is Result.Success) {
            _viewData.value = Result.Success(
                currentResult.data.copy(isLoadingMore = value)
            )
        }
    }

    fun deleteActivity(activity: BabyActivity) {
        val userId = accountService.currentUserId
        val (date, eventId) = when (activity) {
            is BabyActivity.Feeding -> activity.dto.date to activity.dto.id
            is BabyActivity.Nappy -> activity.dto.date to activity.dto.id
            is BabyActivity.Temperature -> activity.dto.date to activity.dto.id
            is BabyActivity.Measurement -> activity.dto.date to activity.dto.id
        }

        if (date != null && eventId != null) {
            viewModelScope.launch {
                try {
                    repository.deleteActivityEvent(userId, date, eventId)
                } catch (e: Exception) {
                    Log.e("DELETE_ERROR", "Failed to delete activity", e)
                }
            }
        }
    }

}
