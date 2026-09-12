package com.bsdevs.babycare.core.domain

import com.bsdevs.network.dto.TaskDto
import kotlinx.coroutines.flow.StateFlow

interface TaskRepository {
    val taskList: StateFlow<List<TaskDto>>
    suspend fun startListening(babyId: String)
    suspend fun stopListening()
    suspend fun addTaskItem(babyId: String, item: TaskDto)
    suspend fun updateTaskItem(babyId: String, item: TaskDto)
    suspend fun deleteTaskItem(babyId: String, itemId: String)
}
