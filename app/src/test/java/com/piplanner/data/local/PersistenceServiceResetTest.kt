package com.piplanner.data.local

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Persistence reset tests run on the JVM with a temp-file DataStore.
 *
 * Intentionally avoids Robolectric / ApplicationProvider so CI cannot flake on
 * Robolectric's runtime MavenArtifactFetcher downloads (FileNotFoundException).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PersistenceServiceResetTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dataStoreScope: CoroutineScope
    private lateinit var persistenceService: PersistenceService

    @Before
    fun setUp() {
        dataStoreScope = CoroutineScope(testDispatcher + SupervisorJob())
        val dataStoreFile = File(temporaryFolder.newFolder(), "piplanner_demo_test.preferences_pb")
        val dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { dataStoreFile },
        )
        persistenceService = DataStorePersistenceService(
            dataStore = dataStore,
            serializer = AppStateJsonSerializer(),
            ioDispatcher = testDispatcher,
        )
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
    }

    @Test
    fun freshInstall_loadState_isEmpty() = runTest(testDispatcher) {
        val state = persistenceService.loadState()
        assertThat(state).isEqualTo(AppState.EMPTY)
    }

    @Test
    fun resetDemo_clearsAllPersistedData() = runTest(testDispatcher) {
        val seeded = AppState(
            accounts = listOf(
                Account(
                    id = "a1",
                    bankName = "HDFC",
                    maskedNumber = "••4821",
                    balance = 10_000_000L,
                    isDedicated = true,
                    isPaytmLinked = true,
                    consentAutoUpdate = true,
                ),
            ),
            goals = listOf(
                Goal(
                    id = "g1",
                    name = "Car",
                    targetAmount = 6_000_000L,
                    startDate = "2026-01-01",
                    endDate = "2027-01-01",
                    savedAmount = 1_000_000L,
                    shareOfNewCredits = 1.0,
                    createdAt = "2026-01-01T00:00:00Z",
                    updatedAt = "2026-01-01T00:00:00Z",
                ),
            ),
            history = listOf(
                HistoryEntry(
                    id = "h1",
                    type = HistoryEntryType.NewCredit,
                    createdAt = "2026-02-01T00:00:00Z",
                    isLocked = true,
                    creditAmount = 50_000L,
                ),
            ),
            standingSplits = listOf(StandingSplit(goalId = "g1", percentage = 1.0)),
            hasCompletedSetup = true,
        )

        persistenceService.saveState(seeded)
        assertThat(persistenceService.loadState()).isEqualTo(seeded)

        persistenceService.resetDemo()

        val afterReset = persistenceService.loadState()
        assertThat(afterReset).isEqualTo(AppState.EMPTY)
        assertThat(afterReset.accounts).isEmpty()
        assertThat(afterReset.goals).isEmpty()
        assertThat(afterReset.history).isEmpty()
        assertThat(afterReset.standingSplits).isEmpty()
        assertThat(afterReset.hasCompletedSetup).isFalse()
    }
}
