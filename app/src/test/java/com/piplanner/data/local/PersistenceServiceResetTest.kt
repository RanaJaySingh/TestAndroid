package com.piplanner.data.local

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.piplanner.data.model.Account
import com.piplanner.data.model.AppState
import com.piplanner.data.model.Goal
import com.piplanner.data.model.HistoryEntry
import com.piplanner.data.model.HistoryEntryType
import com.piplanner.data.model.StandingSplit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26])
class PersistenceServiceResetTest {

    private lateinit var persistenceService: PersistenceService
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = kotlinx.coroutines.CoroutineScope(testDispatcher),
            produceFile = {
                context.preferencesDataStoreFile("piplanner_demo_test_${System.nanoTime()}")
            },
        )
        persistenceService = DataStorePersistenceService(
            dataStore = dataStore,
            serializer = AppStateJsonSerializer(),
            ioDispatcher = testDispatcher,
        )
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
