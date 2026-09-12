package com.bsdevs.babycare.presentation.shopping

import com.bsdevs.babycare.core.domain.TaskRepository
import com.bsdevs.network.dto.TaskDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeTaskRepository : TaskRepository {
    private val _taskList = MutableStateFlow<List<TaskDto>>(emptyList())
    override val taskList: StateFlow<List<TaskDto>> = _taskList.asStateFlow()

    var startListeningCallCount = 0
    var stopListeningCallCount = 0
    var lastBabyId: String? = null

    override suspend fun startListening(babyId: String) {
        startListeningCallCount++
        lastBabyId = babyId
    }

    override suspend fun stopListening() {
        stopListeningCallCount++
    }

    override suspend fun addTaskItem(babyId: String, item: TaskDto) {
        _taskList.value = _taskList.value + item.copy(id = "new_task_id")
    }

    override suspend fun updateTaskItem(babyId: String, item: TaskDto) {
        _taskList.value = _taskList.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun deleteTaskItem(babyId: String, itemId: String) {
        _taskList.value = _taskList.value.filterNot { it.id == itemId }
    }

    fun emitItems(items: List<TaskDto>) {
        _taskList.value = items
    }
}
