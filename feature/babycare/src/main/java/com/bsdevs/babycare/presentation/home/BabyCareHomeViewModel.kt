package com.bsdevs.babycare.presentation.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bsdevs.authentication.AccountService
import com.bsdevs.babycare.core.domain.BabyCareRepository
import com.bsdevs.babycare.presentation.common.BabyActivity
import com.bsdevs.common.DateTimeUtils
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.NetworkScreenData
import com.bsdevs.data.ScreenDataMapper
import com.bsdevs.data.repository.ScreenRepository
import com.bsdevs.data.repository.UserRepository
import com.bsdevs.network.dto.DailyLogDto
import com.bsdevs.network.dto.FeedingDto
import com.bsdevs.network.dto.MeasurementDto
import com.bsdevs.network.dto.NappyChangeDto
import com.bsdevs.network.dto.TemperatureDto
import com.bsdevs.network.dto.BabyEvent
import com.bsdevs.network.dto.VaccinationDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class BabyCareHomeViewModel @Inject constructor(
    private val repository: BabyCareRepository,
    private val accountService: AccountService,
    private val screenRepository: ScreenRepository,
    private val userRepository: UserRepository,
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
        // 🌟 1. Observe the baby profile for server-side predictions reactively
        viewModelScope.launch {
            userRepository.userProfile
                .flatMapLatest { user ->
                    val babyId = user?.babyId
                    if (babyId != null) {
                        userRepository.getBabyFlow(babyId)
                    } else {
                        flowOf(null)
                    }
                }
                .collect {
                    // Trigger UI update when baby profile (including predictions) changes
                    updateDisplayFeed(repository.cachedDays.value)
                }
        }

        // 🌟 2. Observe the repository cache in the background
        viewModelScope.launch {
            repository.cachedDays
                .debounce(100) // 🛡️ Prevent rapid-fire UI updates during batch sync or pagination
                .collect { dailyLogs ->
                    // Transition to success if we have data to show (Offline-first)
                    if (dailyLogs.isNotEmpty() || (_viewData.value !is Result.Loading)) {
                        updateDisplayFeed(dailyLogs)
                    }
                }
        }

        // Trigger initial data load immediately on launch
        initialLoad()
    }

    private fun initialLoad() {
        viewModelScope.launch {
            // 1. 🔄 SEQUENCE: Refresh User Profile first to ensure we have the correct babyId and auth
            // This prevents stale/unauthorized data if the user just signed in or changed babies.
            try {
                val userId = accountService.currentUserId
                if (userId.isNotEmpty()) {
                    userRepository.getUser(userId, forceRefresh = true)
                }
            } catch (e: Exception) {
                Log.e("HOME_INIT", "Failed to refresh user profile on launch", e)
            }

            // 2. ⚡ PARALLEL: Load dynamic UI and baby activity data
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
                // 📂 Trust cache first for the initial load to improve startup speed and offline support.
                val fetchResult = repository.loadInitialData(
                    userId = accountService.currentUserId,
                    pageSize = pageSize,
                    forceRefresh = false,
                )

                // Force switch the state to success immediately, even if the month is brand new/empty
                updateDisplayFeed(
                    dailyLogs = repository.cachedDays.value,
                    canLoadMore = fetchResult.hasMoreData,
                    isRefreshing = false,
                    forceSuccess = true,
                )
            } catch (_: Exception) {
                Log.e("HOME_INIT_ERROR", "Failed initial data block fetch")
                // If we have cached data, don't show error screen, just stop loading
                repository.cachedDays.value.takeIf { it.isNotEmpty() }?.let { logs ->
                    updateDisplayFeed(
                        dailyLogs = logs,
                        isRefreshing = false,
                        forceSuccess = true,
                    )
                } ?: run {
                    _viewData.value = Result.Error(Exception("Failed to fetch initial data"))
                }
            }
        }
    }

    private suspend fun updateDisplayFeed(
        dailyLogs: List<DailyLogDto>,
        canLoadMore: Boolean? = null,
        isRefreshing: Boolean? = null,
        isLoadingMore: Boolean? = null,
        forceSuccess: Boolean = false
    ) {
        val currentResult = _viewData.value

        // 🛡️ Optimization: If we are in Loading state and have no data yet, 
        // don't switch to Success with empty data UNLESS forceSuccess is true (initial load complete)
        if (currentResult is Result.Loading && dailyLogs.isEmpty() && !forceSuccess) {
            return
        }

        val currentState = (currentResult as? Result.Success)?.data
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

            try {
                // Refresh Screen Config too
                launch {
                    try {
                        screenRepository.getScreenFlow("baby_home", forceRefresh = true)
                            .collect { result ->
                                if (result is Result.Success) {
                                    val mappedData = withContext(dispatchers.default) {
                                        mapper.mapToData(result.data)
                                    }
                                    _dynamicUi.value = mappedData
                                }
                            }
                    } catch (e: Exception) {
                        Log.e("REFRESH_ERROR", "Failed to refresh screen config", e)
                    }
                }

                // Force re-fetch the baby profile to bypass local cache
                accountService.currentUserId.takeIf { it.isNotEmpty() }?.let { userId ->
                    userRepository.getUser(userId, forceRefresh = true)?.babyId?.let { babyId ->
                        userRepository.getBaby(babyId, forceRefresh = true)
                    }
                }

                val refreshResult = repository.refreshData(accountService.currentUserId, pageSize)
                updateDisplayFeed(
                    dailyLogs = repository.cachedDays.value,
                    canLoadMore = refreshResult.hasMoreData,
                    isRefreshing = false,
                    forceSuccess = true,
                )
            } catch (_: Exception) {
                Log.e("REFRESH_ERROR", "Failed to complete refresh cycle")
                // Stop the spinner even on failure
                updateDisplayFeed(
                    dailyLogs = repository.cachedDays.value,
                    isRefreshing = false,
                    forceSuccess = true,
                )
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
            setLoadingMoreState(isLoading = true)
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
            } catch (_: Exception) {
                setLoadingMoreState(isLoading = false)
            }
        }
    }

    private suspend fun processFeed(
        dailyLogs: List<DailyLogDto>,
        filter: ActivityFilter,
        collapsed: Set<String>
    ): BabyCareHomeViewData = withContext(dispatchers.default) {
        val finalizedFeed = mutableListOf<HomeFeedItem>()

        val eventComparator = Comparator<BabyEvent> { a, b ->
            val instantA = DateTimeUtils.parseToInstant(a.dateTimeString)
            val instantB = DateTimeUtils.parseToInstant(b.dateTimeString)
            instantB.compareTo(instantA) // Newest first
        }

        // 🌟 IMPROVED: Find absolute latest readings across all cached logs, not just today
        val allEventsFlattened =
            dailyLogs.asSequence().flatMap { it.events }.sortedWith(eventComparator).toList()

        val absoluteLastNappy = allEventsFlattened.firstOrNull {
            it is BabyEvent.Nappy
        }?.let { "Last nappy: ${it.time}" }

        val absoluteLastFeeding = allEventsFlattened.firstOrNull {
            it is BabyEvent.Feeding
        }?.let { "Last feed: ${it.time}" }

        val babyId = userRepository.userProfile.value?.babyId
        val baby = babyId?.let { userRepository.getBaby(it) }

        // Use server-side prediction from Firebase
        val zone = ZoneId.systemDefault()

        val feedingPrediction = when {
            baby?.effectiveNextFeedingTimeMin != null && baby.effectiveNextFeedingTimeMax != null -> {
                val min = DateTimeUtils.formatIsoToTime(baby.effectiveNextFeedingTimeMin, zone)
                val max = DateTimeUtils.formatIsoToTime(baby.effectiveNextFeedingTimeMax, zone)
                "Next: $min - $max"
            }

            baby?.effectiveNextFeedingTime != null -> {
                "Next: ${DateTimeUtils.formatIsoToTime(baby.effectiveNextFeedingTime, zone)}"
            }

            else -> null
        }

        val lastTempEvent = allEventsFlattened.firstOrNull {
            it is BabyEvent.Temperature && it.temperature != null && it.temperature != 0.0
        } as? BabyEvent.Temperature

        val absoluteLastTemperature = lastTempEvent?.let {
            "Last temp: ${it.temperature}°C"
        }

        val lastMeasurementEvent = allEventsFlattened.firstOrNull {
            it is BabyEvent.Measurement && (it.weight != null || it.height != null)
        } as? BabyEvent.Measurement

        val absoluteLastMeasurement = lastMeasurementEvent?.let {
            val weight =
                it.weight?.let { w -> String.format(Locale.getDefault(), "%.2fkg", w) } ?: ""
            val height =
                it.height?.let { h -> String.format(Locale.getDefault(), "%.1fcm", h) } ?: ""
            val head =
                it.headCircumference?.let { hc -> String.format(Locale.getDefault(), "%.1fcm", hc) }
                    ?: ""
            "Last: $weight $height $head".trim()
        }

        val absoluteLastVaccination = allEventsFlattened.firstOrNull {
            it is BabyEvent.Vaccination
        }?.let { "Last vaccine: ${it.time}" }

        dailyLogs.forEach { dayLog ->

            // 🌟 FIXED: Force all nested events for this calendar day to sort by time (newest first)
            val sortedDayEvents = dayLog.events.sortedWith(eventComparator)

            // 🔄 Apply active filter rules onto the cleanly sorted list array instead of the raw one
            val visibleEvents = sortedDayEvents.filter { event ->
                when (filter) {
                    ActivityFilter.NONE -> true
                    ActivityFilter.NAPPY -> event is BabyEvent.Nappy
                    ActivityFilter.FEEDING -> event is BabyEvent.Feeding
                    ActivityFilter.TEMPERATURE -> event is BabyEvent.Temperature
                    ActivityFilter.MEASUREMENT -> event is BabyEvent.Measurement
                    ActivityFilter.VACCINATION -> event is BabyEvent.Vaccination
                }
            }

            val feedingCount = visibleEvents.count { it is BabyEvent.Feeding }
            val nappyCount = visibleEvents.count { it is BabyEvent.Nappy }
            val temperatureCount = visibleEvents.count { it is BabyEvent.Temperature }
            val measurementCount = visibleEvents.count { it is BabyEvent.Measurement }
            val vaccinationCount = visibleEvents.count { it is BabyEvent.Vaccination }
            val displayHeaderTitle = formatHeaderDate(dayLog.date)

            val isVitaminDTakenForDay =
                dayLog.events.any { it is BabyEvent.Feeding && it.hasVitaminD == true }

            finalizedFeed.add(
                HomeFeedItem.Header(
                    displayHeaderTitle,
                    feedingCount,
                    nappyCount,
                    temperatureCount,
                    measurementCount,
                    vaccinationCount
                )
            )

            // 🌟 NEW: Insert Prediction Card if header is "Today" and filter is FEEDING
            if (displayHeaderTitle == "Today" && filter == ActivityFilter.FEEDING) {
                baby?.effectivePredictionsByModel?.takeIf { it.isNotEmpty() }?.let { predictions ->
                    finalizedFeed.add(
                        HomeFeedItem.PredictionCard(
                            predictions = predictions,
                            activeModel = baby.effectiveActiveModel
                        )
                    )
                }
            }

            if (!collapsed.contains(displayHeaderTitle)) {
                visibleEvents.forEach { event ->
                    val babyActivityModel =
                        mapToBabyActivity(event, dayLog.date, isVitaminDTakenForDay)
                    finalizedFeed.add(HomeFeedItem.ActivityRow(babyActivityModel))
                }
            }
        }

        BabyCareHomeViewData(
            lastNappyChange = absoluteLastNappy,
            lastFeeding = absoluteLastFeeding,
            nextFeedingPrediction = feedingPrediction,
            lastTemperature = absoluteLastTemperature,
            lastMeasurement = absoluteLastMeasurement,
            lastVaccination = absoluteLastVaccination,
            activityFeed = finalizedFeed,
            dynamicUi = _dynamicUi.value,
            currentFilter = filter,
            collapsedHeaders = collapsed
        )
    }


    private fun mapToBabyActivity(
        event: BabyEvent,
        parentDate: String,
        isVitaminDTakenForDay: Boolean
    ): BabyActivity {
        return when (event) {
            is BabyEvent.Nappy -> {
                BabyActivity.Nappy(
                    NappyChangeDto(
                        id = event.id,
                        date = parentDate,
                        time = event.time,
                        dateTime = event.dateTimeString,
                        type = event.nappyType ?: "Nappy",
                        comment = event.comment,
                    )
                )
            }
            is BabyEvent.Temperature -> {
                BabyActivity.Temperature(
                    TemperatureDto(
                        id = event.id,
                        date = parentDate,
                        time = event.time,
                        dateTime = event.dateTimeString,
                        temperature = event.temperature ?: 37.0,
                        comment = event.comment
                    )
                )
            }
            is BabyEvent.Measurement -> {
                BabyActivity.Measurement(
                    MeasurementDto(
                        id = event.id,
                        date = parentDate,
                        time = event.time,
                        dateTime = event.dateTimeString,
                        height = event.height,
                        weight = event.weight,
                        headCircumference = event.headCircumference,
                        isMedical = event.isMedical ?: false,
                        comment = event.comment
                    )
                )
            }
            is BabyEvent.Vaccination -> {
                BabyActivity.Vaccination(
                    VaccinationDto(
                        id = event.id,
                        date = parentDate,
                        time = event.time,
                        dateTime = event.dateTimeString,
                        vaccinationNames = event.vaccinationNames ?: emptyList(),
                        location = event.location,
                        seriesId = event.seriesId,
                        comment = event.comment
                    )
                )
            }
            is BabyEvent.Feeding -> {
                val hasVitaminD = event.hasVitaminD ?: false
                val showToggle = !isVitaminDTakenForDay || hasVitaminD

                BabyActivity.Feeding(
                    dto = FeedingDto(
                        id = event.id,
                        date = parentDate,
                        startTime = event.time,
                        dateTime = event.dateTimeString,
                        mainFeedingSide = event.mainFeedingSide,
                        leftDuration = event.leftDuration,
                        rightDuration = event.rightDuration,
                        totalDuration = event.totalDuration,
                        bottleAmountMl = event.bottleAmountMl,
                        comment = event.comment,
                        hasVitaminD = hasVitaminD
                    ),
                    showVitaminDToggle = showToggle
                )
            }
            else -> {
                // Fallback for Unknown or VitaminD (if handled separately)
                BabyActivity.Nappy(NappyChangeDto(id = event.id, date = parentDate, type = "Unknown"))
            }
        }
    }

    private fun formatHeaderDate(dateString: String): String {
        return try {
            // Safe check: extract just the YYYY-MM-DD segment if it contains time info (handles space or T)
            val cleanDateStr = dateString.substringBefore("T").substringBefore(" ")
            val targetDate = LocalDate.parse(cleanDateStr)
            val today = repository.getCurrentDate()

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
        } catch (_: Exception) {
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

    private fun setLoadingMoreState(isLoading: Boolean) {
        (_viewData.value as? Result.Success)?.data?.let { data ->
            _viewData.value = Result.Success(
                data.copy(isLoadingMore = isLoading),
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
            is BabyActivity.Vaccination -> activity.dto.date to activity.dto.id
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

    fun toggleVitaminD(activity: BabyActivity.Feeding) {
        viewModelScope.launch {
            try {
                val userId = accountService.currentUserId
                val date = activity.date ?: return@launch
                val eventId = activity.id ?: return@launch

                val currentEvent = repository.getFeedingEventById(userId, eventId) ?: return@launch

                val updatedEvent = currentEvent.copy(
                    hasVitaminD = !(currentEvent.hasVitaminD ?: false)
                )

                repository.updateActivityEvent(userId, date, eventId, updatedEvent)
            } catch (e: Exception) {
                Log.e("HOME_VITD_ERROR", "Failed to toggle Vitamin D", e)
            }
        }
    }
}
