package com.bsdevs.forms.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.bsdevs.forms.navigation.FormRoute
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.FormDataMapper
import com.bsdevs.data.FormFieldData
import com.bsdevs.data.FormSchemaData
import com.bsdevs.network.repository.FormDeleter
import com.bsdevs.network.repository.FormPrefiller
import com.bsdevs.network.repository.FormRepository
import com.bsdevs.network.repository.FormSubmitter
import com.bsdevs.network.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class FormSubmitState {
    object Idle : FormSubmitState()
    object Loading : FormSubmitState()
    data class Success(val destination: String) : FormSubmitState()
    object Deleted : FormSubmitState()
    data class Error(val message: String) : FormSubmitState()
}

@HiltViewModel
class FormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val formRepository: FormRepository,
    private val formDataMapper: FormDataMapper,
    private val formSubmitter: FormSubmitter,
    private val formPrefiller: FormPrefiller,
    private val formDeleter: FormDeleter,
    private val userRepository: UserRepository,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val route = savedStateHandle.toRoute<FormRoute>()
    private val formId: String = route.formId
    val entityId: String? = route.entityId

    private val _formSchema = MutableStateFlow<Result<FormSchemaData>>(Result.Loading)
    val formSchema: StateFlow<Result<FormSchemaData>> = _formSchema.asStateFlow()

    private val _fieldValues = MutableStateFlow<Map<String, Any>>(emptyMap())
    val fieldValues: StateFlow<Map<String, Any>> = _fieldValues.asStateFlow()

    private val _submitState = MutableStateFlow<FormSubmitState>(FormSubmitState.Idle)
    val submitState: StateFlow<FormSubmitState> = _submitState.asStateFlow()

    init {
        loadForm()
    }

    private fun loadForm() {
        viewModelScope.launch(dispatchers.io) {
            formRepository.getFormSchema(formId).collect { result ->
                when (result) {
                    is Result.Success -> {
                        val schema = formDataMapper.mapToData(formId, result.data)
                        val defaults = buildMap<String, Any> {
                            schema.fields.forEach { field ->
                                when (field) {
                                    is FormFieldData.SwitchFieldData -> put(field.fieldKey, field.default)
                                    is FormFieldData.DateInputData -> put(field.fieldKey, LocalDate.now().toString())
                                    is FormFieldData.TimeInputData -> put(field.fieldKey, LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")))
                                    else -> {}
                                }
                            }
                        }

                        val prefilled = if (entityId != null) {
                            val userId = userRepository.userProfile.value?.id
                            if (userId != null) {
                                defaults + (formPrefiller.loadExistingValues(userId, schema.submitTarget, entityId) ?: emptyMap())
                            } else defaults
                        } else defaults

                        _fieldValues.value = prefilled
                        _formSchema.value = Result.Success(schema)
                    }
                    is Result.Error -> _formSchema.value = Result.Error(result.exception)
                    Result.Loading -> _formSchema.value = Result.Loading
                }
            }
        }
    }

    fun onFieldChanged(fieldKey: String, value: Any) {
        _fieldValues.update { it + (fieldKey to value) }
    }

    fun onSubmit() {
        val schema = (_formSchema.value as? Result.Success)?.data ?: return
        val values = _fieldValues.value

        val missingLabels = schema.fields
            .filter { field ->
                val visible = field.showWhen?.let { values[it.fieldKey] == it.equals } ?: true
                field.required && visible && !values.containsKey(field.fieldKey)
            }
            .map { it.label }

        if (missingLabels.isNotEmpty()) {
            _submitState.value = FormSubmitState.Error("Please fill in: ${missingLabels.joinToString()}")
            return
        }

        viewModelScope.launch(dispatchers.io) {
            val userId = userRepository.userProfile.value?.id ?: run {
                _submitState.value = FormSubmitState.Error("Not signed in")
                return@launch
            }
            _submitState.value = FormSubmitState.Loading
            _submitState.value = when (val result = formSubmitter.submit(userId, schema.submitTarget, entityId, values)) {
                is Result.Success -> FormSubmitState.Success(schema.submitDestination)
                is Result.Error -> FormSubmitState.Error(result.exception.message ?: "Submit failed")
                Result.Loading -> FormSubmitState.Idle
            }
        }
    }

    fun onDelete() {
        val schema = (_formSchema.value as? Result.Success)?.data ?: return
        val id = entityId ?: return

        viewModelScope.launch(dispatchers.io) {
            val userId = userRepository.userProfile.value?.id ?: run {
                _submitState.value = FormSubmitState.Error("Not signed in")
                return@launch
            }
            _submitState.value = FormSubmitState.Loading
            _submitState.value = when (val result = formDeleter.delete(userId, schema.submitTarget, id)) {
                is Result.Success -> FormSubmitState.Deleted
                is Result.Error -> FormSubmitState.Error(result.exception.message ?: "Delete failed")
                Result.Loading -> FormSubmitState.Idle
            }
        }
    }

    fun clearSubmitState() {
        _submitState.value = FormSubmitState.Idle
    }
}
