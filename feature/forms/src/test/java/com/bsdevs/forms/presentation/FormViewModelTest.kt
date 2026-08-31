package com.bsdevs.forms.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bsdevs.common.DispatcherProvider
import com.bsdevs.common.result.Result
import com.bsdevs.data.FormDataMapperImpl
import com.bsdevs.data.FormFieldData
import com.bsdevs.network.dto.FormFieldConditionDto
import com.bsdevs.network.dto.FormFieldDto
import com.bsdevs.network.dto.FormSchemaDto
import com.bsdevs.network.dto.FormSubmissionDto
import com.bsdevs.network.repository.FormDeleter
import com.bsdevs.network.repository.FormPrefiller
import com.bsdevs.network.repository.FormRepository
import com.bsdevs.network.repository.FormSubmitter
import com.bsdevs.network.repository.UserRepository
import com.bsdevs.network.dto.UserDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FormViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeRepository: FakeFormRepository
    private val formDataMapper = FormDataMapperImpl()
    private val formSubmitter = mockk<FormSubmitter>()
    private val formPrefiller = mockk<FormPrefiller>()
    private val formDeleter = mockk<FormDeleter>()
    private val userRepository = mockk<UserRepository>()

    private val dispatchers = object : DispatcherProvider {
        override val main = testDispatcher
        override val io = testDispatcher
        override val default = testDispatcher
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeFormRepository()
        every { userRepository.userProfile } returns MutableStateFlow(UserDto(id = "user123")).asStateFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(formId: String = "testForm", entityId: String? = null): FormViewModel {
        val args = if (entityId != null) mapOf("formId" to formId, "entityId" to entityId)
                   else mapOf("formId" to formId)
        return FormViewModel(
            SavedStateHandle(args), fakeRepository, formDataMapper,
            formSubmitter, formPrefiller, formDeleter, userRepository, dispatchers,
        )
    }

    private fun sampleSchema(
        submitTarget: String = "testTarget",
        submitDestination: String = "home",
        deletable: Boolean = false,
        fields: List<FormFieldDto> = emptyList(),
    ) = FormSchemaDto("Test Form", submitTarget, submitDestination, deletable, fields)

    // --- Schema loading ---

    @Test
    fun `initial state is Loading`() = runTest {
        val vm = createViewModel()
        assertEquals(Result.Loading, vm.formSchema.value)
    }

    @Test
    fun `schema loads to Success when repository emits`() = runTest {
        val vm = createViewModel()
        vm.formSchema.test {
            assertEquals(Result.Loading, awaitItem())
            fakeRepository.emitSchema("testForm", sampleSchema())
            assertTrue(awaitItem() is Result.Success)
        }
    }

    @Test
    fun `schema error state forwarded from repository`() = runTest {
        val vm = createViewModel()
        vm.formSchema.test {
            assertEquals(Result.Loading, awaitItem())
            fakeRepository.emitError("testForm", Exception("Network failure"))
            val error = awaitItem() as Result.Error
            assertEquals("Network failure", error.exception.message)
        }
    }

    @Test
    fun `switch field default is pre-populated in fieldValues`() = runTest {
        val switchField = FormFieldDto("newsletter", "SWITCH", "Newsletter", false, 0, defaultValue = true)
        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema(fields = listOf(switchField)))
        assertEquals(true, vm.fieldValues.value["newsletter"])
    }

    // --- onFieldChanged ---

    @Test
    fun `onFieldChanged updates field value`() = runTest {
        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema())
        vm.onFieldChanged("name", "Alice")
        assertEquals("Alice", vm.fieldValues.value["name"])
    }

    @Test
    fun `onFieldChanged merges with existing values`() = runTest {
        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema())
        vm.onFieldChanged("first", "Alice")
        vm.onFieldChanged("last", "Smith")
        assertEquals("Alice", vm.fieldValues.value["first"])
        assertEquals("Smith", vm.fieldValues.value["last"])
    }

    // --- Pre-filling ---

    @Test
    fun `pre-fills values from prefiller when entityId is set`() = runTest {
        coEvery { formPrefiller.loadExistingValues("user123", "testTarget", "entity1") } returns
            mapOf("name" to "Pre-filled")

        val vm = createViewModel(entityId = "entity1")
        fakeRepository.emitSchema("testForm", sampleSchema())

        assertEquals("Pre-filled", vm.fieldValues.value["name"])
    }

    @Test
    fun `entityId is null for new form`() {
        val vm = createViewModel()
        assertNull(vm.entityId)
    }

    @Test
    fun `entityId is set for edit mode`() {
        val vm = createViewModel(entityId = "entity1")
        assertEquals("entity1", vm.entityId)
    }

    // --- Submit ---

    @Test
    fun `submit with missing required field emits Error using field label`() = runTest {
        val requiredField = FormFieldDto("name", "TEXT_INPUT", "Full Name", required = true, index = 0)
        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema(fields = listOf(requiredField)))

        vm.submitState.test {
            assertEquals(FormSubmitState.Idle, awaitItem())
            vm.onSubmit()
            val error = awaitItem() as FormSubmitState.Error
            assertTrue("Error should contain field label not key", error.message.contains("Full Name"))
            assertFalse("Error should not contain raw field key", error.message.contains("name") && !error.message.contains("Full Name"))
        }
    }

    @Test
    fun `DATE_INPUT field is pre-populated with today when no entityId`() = runTest {
        val dateField = FormFieldDto("date", "DATE_INPUT", "Date", required = true, index = 0)
        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema(fields = listOf(dateField)))
        assertNotNull(vm.fieldValues.value["date"])
        assertTrue((vm.fieldValues.value["date"] as String).matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun `TIME_INPUT field is pre-populated with current time when no entityId`() = runTest {
        val timeField = FormFieldDto("time", "TIME_INPUT", "Time", required = true, index = 0)
        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema(fields = listOf(timeField)))
        assertNotNull(vm.fieldValues.value["time"])
        assertTrue((vm.fieldValues.value["time"] as String).matches(Regex("\\d{2}:\\d{2}")))
    }

    @Test
    fun `prefilled values override date-time defaults in edit mode`() = runTest {
        coEvery { formPrefiller.loadExistingValues("user123", "testTarget", "e1") } returns
            mapOf("date" to "2025-01-15", "time" to "09:30")
        val dateField = FormFieldDto("date", "DATE_INPUT", "Date", required = true, index = 0)
        val timeField = FormFieldDto("time", "TIME_INPUT", "Time", required = true, index = 1)

        val vm = createViewModel(entityId = "e1")
        fakeRepository.emitSchema("testForm", sampleSchema(fields = listOf(dateField, timeField)))

        assertEquals("2025-01-15", vm.fieldValues.value["date"])
        assertEquals("09:30", vm.fieldValues.value["time"])
    }

    @Test
    fun `submit success emits Success with destination`() = runTest {
        coEvery { formSubmitter.submit("user123", "testTarget", any(), any()) } returns Result.Success(Unit)

        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema(submitDestination = "home"))

        vm.submitState.test {
            assertEquals(FormSubmitState.Idle, awaitItem())
            vm.onSubmit()
            assertEquals(FormSubmitState.Loading, awaitItem())
            val success = awaitItem() as FormSubmitState.Success
            assertEquals("home", success.destination)
        }
    }

    @Test
    fun `submit failure emits Error state`() = runTest {
        coEvery { formSubmitter.submit(any(), any(), any(), any()) } returns Result.Error(Exception("Server error"))

        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema())

        vm.submitState.test {
            assertEquals(FormSubmitState.Idle, awaitItem())
            vm.onSubmit()
            assertEquals(FormSubmitState.Loading, awaitItem())
            val error = awaitItem() as FormSubmitState.Error
            assertEquals("Server error", error.message)
        }
    }

    @Test
    fun `submit without signed-in user emits Error`() = runTest {
        every { userRepository.userProfile } returns MutableStateFlow<UserDto?>(null).asStateFlow()

        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema())

        vm.submitState.test {
            assertEquals(FormSubmitState.Idle, awaitItem())
            vm.onSubmit()
            val error = awaitItem() as FormSubmitState.Error
            assertTrue(error.message.contains("signed in", ignoreCase = true))
        }
    }

    // --- Delete ---

    @Test
    fun `delete in edit mode emits Deleted state`() = runTest {
        coEvery { formDeleter.delete("user123", "testTarget", "entity1") } returns Result.Success(Unit)

        val vm = createViewModel(entityId = "entity1")
        fakeRepository.emitSchema("testForm", sampleSchema(deletable = true))

        vm.submitState.test {
            assertEquals(FormSubmitState.Idle, awaitItem())
            vm.onDelete()
            assertEquals(FormSubmitState.Loading, awaitItem())
            assertEquals(FormSubmitState.Deleted, awaitItem())
        }
    }

    @Test
    fun `delete failure emits Error state`() = runTest {
        coEvery { formDeleter.delete(any(), any(), any()) } returns Result.Error(Exception("Delete failed"))

        val vm = createViewModel(entityId = "entity1")
        fakeRepository.emitSchema("testForm", sampleSchema())

        vm.submitState.test {
            assertEquals(FormSubmitState.Idle, awaitItem())
            vm.onDelete()
            assertEquals(FormSubmitState.Loading, awaitItem())
            val error = awaitItem() as FormSubmitState.Error
            assertEquals("Delete failed", error.message)
        }
    }

    @Test
    fun `delete does nothing when entityId is null`() = runTest {
        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema())

        vm.submitState.test {
            assertEquals(FormSubmitState.Idle, awaitItem())
            vm.onDelete()
            expectNoEvents()
        }
    }

    // --- conditional fields (showWhen) ---

    @Test
    fun `required field hidden by showWhen condition does not block submit`() = runTest {
        coEvery { formSubmitter.submit(any(), any(), any(), any()) } returns Result.Success(Unit)

        // height_value is required but only visible when record_height == true
        val conditionalField = FormFieldDto(
            "height_value", "WHEEL_INPUT", "Height", required = true, index = 1,
            showWhen = FormFieldConditionDto("record_height", true),
        )
        val switchField = FormFieldDto("record_height", "SWITCH", "Record Height", required = false, index = 0, defaultValue = false)

        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema(fields = listOf(switchField, conditionalField)))

        // record_height is false (default) so height_value is hidden - submit should pass
        vm.submitState.test {
            assertEquals(FormSubmitState.Idle, awaitItem())
            vm.onSubmit()
            assertEquals(FormSubmitState.Loading, awaitItem())
            assertTrue(awaitItem() is FormSubmitState.Success)
        }
    }

    @Test
    fun `required field visible via showWhen condition blocks submit when empty`() = runTest {
        val conditionalField = FormFieldDto(
            "height_value", "WHEEL_INPUT", "Height", required = true, index = 1,
            showWhen = FormFieldConditionDto("record_height", true),
        )
        val switchField = FormFieldDto("record_height", "SWITCH", "Record Height", required = false, index = 0, defaultValue = false)

        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema(fields = listOf(switchField, conditionalField)))

        // turn the switch on - height_value is now visible and required
        vm.onFieldChanged("record_height", true)

        vm.submitState.test {
            assertEquals(FormSubmitState.Idle, awaitItem())
            vm.onSubmit()
            val error = awaitItem() as FormSubmitState.Error
            assertTrue(error.message.contains("height_value"))
        }
    }

    @Test
    fun `required field becomes visible and passes when value provided`() = runTest {
        coEvery { formSubmitter.submit(any(), any(), any(), any()) } returns Result.Success(Unit)

        val conditionalField = FormFieldDto(
            "height_value", "WHEEL_INPUT", "Height", required = true, index = 1,
            showWhen = FormFieldConditionDto("record_height", true),
        )
        val switchField = FormFieldDto("record_height", "SWITCH", "Record Height", required = false, index = 0, defaultValue = false)

        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema(fields = listOf(switchField, conditionalField)))

        vm.onFieldChanged("record_height", true)
        vm.onFieldChanged("height_value", 650)

        vm.submitState.test {
            assertEquals(FormSubmitState.Idle, awaitItem())
            vm.onSubmit()
            assertEquals(FormSubmitState.Loading, awaitItem())
            assertTrue(awaitItem() is FormSubmitState.Success)
        }
    }

    // --- clearSubmitState ---

    @Test
    fun `clearSubmitState resets to Idle`() = runTest {
        coEvery { formSubmitter.submit(any(), any(), any(), any()) } returns Result.Success(Unit)
        val vm = createViewModel()
        fakeRepository.emitSchema("testForm", sampleSchema())
        vm.onSubmit()
        vm.clearSubmitState()
        assertEquals(FormSubmitState.Idle, vm.submitState.value)
    }
}

private class FakeFormRepository : FormRepository {
    private val flows = mutableMapOf<String, MutableStateFlow<Result<FormSchemaDto>>>()

    fun emitSchema(formId: String, dto: FormSchemaDto) {
        getOrCreate(formId).value = Result.Success(dto)
    }

    fun emitError(formId: String, exception: Exception) {
        getOrCreate(formId).value = Result.Error(exception)
    }

    private fun getOrCreate(formId: String) =
        flows.getOrPut(formId) { MutableStateFlow(Result.Loading) }

    override suspend fun getFormSchema(formId: String): Flow<Result<FormSchemaDto>> = getOrCreate(formId)
    override suspend fun submitForm(userId: String, formId: String, values: Map<String, Any>): Result<Unit> = Result.Success(Unit)
    override suspend fun getPreviousSubmission(userId: String, formId: String): FormSubmissionDto? = null
    override suspend fun seedFormIfAbsent(formId: String, data: Map<String, Any>) {}
}
