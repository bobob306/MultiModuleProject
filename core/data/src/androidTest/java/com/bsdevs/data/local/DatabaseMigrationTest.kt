package com.bsdevs.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        MMPDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate2ToLatest() {
        // We start at version 2 because schema exporting was not enabled for version 1.
        // In the future, when you move to version 3, you would change this to:
        // helper.createDatabase(TEST_DB, 2)
        // helper.runMigrationsAndValidate(TEST_DB, 3, true)
        
        helper.createDatabase(TEST_DB, 2).apply {
            close()
        }

        // Currently, 2 is the latest, so this just validates that the current 
        // code matches the generated 2.json schema file.
        helper.runMigrationsAndValidate(TEST_DB, 2, true)
    }
}
