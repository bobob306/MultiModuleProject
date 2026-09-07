package com.bsdevs.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bsdevs.data.local.MMPDatabase
import com.bsdevs.data.local.entities.FormSchemaEntity
import com.bsdevs.data.local.entities.FormSubmissionEntity
import com.bsdevs.data.local.entities.ScreenEntity
import com.bsdevs.network.dto.FormSchemaDto
import com.bsdevs.network.dto.FormSubmissionDto
import com.bsdevs.network.dto.ScreenDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class DaoTests {

    private lateinit var db: MMPDatabase
    private lateinit var screenDao: ScreenDao
    private lateinit var formDao: FormDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, MMPDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        screenDao = db.screenDao()
        formDao = db.formDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun insertAndGetScreen() = runTest {
        val screenId = "home"
        val components = listOf(ScreenDto.TitleDto(0, "Welcome"))
        val entity = ScreenEntity(screenId, components)
        
        screenDao.insertScreen(entity)
        
        val result = screenDao.getScreen(screenId)
        assertEquals(screenId, result?.screenId)
        assertEquals(1, result?.components?.size)
        assertEquals("Welcome", (result?.components?.first() as ScreenDto.TitleDto).content)
    }

    @Test
    fun deleteScreen() = runTest {
        val screenId = "home"
        screenDao.insertScreen(ScreenEntity(screenId, emptyList()))
        
        screenDao.deleteScreen(screenId)
        
        assertNull(screenDao.getScreen(screenId))
    }

    @Test
    fun screenFlowUpdatesCorrectly() = runTest {
        val screenId = "home"
        
        val flow = screenDao.getScreenFlow(screenId)
        // Note: Room Flow emits immediately if there's a listener. 
        // For in-memory and first call it might emit null if empty.
        
        screenDao.insertScreen(ScreenEntity(screenId, emptyList()))
        assertEquals(screenId, flow.first()?.screenId)
    }

    @Test
    fun insertAndGetFormSchema() = runTest {
        val formId = "testForm"
        val schema = FormSchemaDto(title = "Test Form")
        val entity = FormSchemaEntity(formId, schema)

        formDao.insertSchema(entity)

        val result = formDao.getSchema(formId)
        assertEquals("Test Form", result?.schema?.title)
    }

    @Test
    fun insertAndGetPendingSubmissions() = runTest {
        val userId = "u1"
        val formId = "f1"
        val submission = FormSubmissionDto(
            submittedAt = 123L,
            values = mapOf("field" to JsonPrimitive("value"))
        )
        val entity = FormSubmissionEntity(
            id = "u1_f1",
            userId = userId,
            formId = formId,
            submission = submission,
            isPendingSync = true
        )

        formDao.insertSubmission(entity)

        val pending = formDao.getPendingSubmissions()
        assertEquals(1, pending.size)
        assertEquals("value", (pending[0].submission.values["field"] as JsonPrimitive).content)
    }
}
