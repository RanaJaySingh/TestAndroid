package com.piplanner.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.piplanner.data.local.DataStorePersistenceService
import com.piplanner.data.local.PersistenceService
import com.piplanner.domain.BalanceSyncService
import com.piplanner.domain.GrokService
import com.piplanner.domain.MockBalanceSyncService
import com.piplanner.domain.StubGrokService
import com.piplanner.util.DemoData
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// PostSetupBalanceSync qualifier lives in PostSetupBalanceSync.kt

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindModule {

    @Binds
    @Singleton
    abstract fun bindPersistenceService(
        impl: DataStorePersistenceService,
    ): PersistenceService
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvideModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile(DATA_STORE_FILE) },
        )
    }

    /**
     * Spec §3.3 mock — injectable so a real BalanceSyncService can replace it later.
     * Only the dedicated savings account is tracked (R25 — spending SBI payments not seen).
     * Consent / setup uses the seeded opening balance (₹1,00,000).
     */
    @Provides
    @Singleton
    fun provideBalanceSyncService(): BalanceSyncService {
        return MockBalanceSyncService(
            knownAccountIds = DemoData.trackedAccountIds(),
            fetchedBalancePaisa = MockBalanceSyncService.DEMO_BALANCE_PAISA,
        )
    }

    /**
     * Goals Sync/Update demos return a higher balance so the first post-setup Sync
     * creates an open credit (₹10,000) — frames 10 / 13.
     * Spending account is intentionally omitted (R25).
     */
    @Provides
    @Singleton
    @PostSetupBalanceSync
    fun providePostSetupBalanceSyncService(): BalanceSyncService {
        return MockBalanceSyncService(
            knownAccountIds = DemoData.trackedAccountIds(),
            fetchedBalancePaisa = MockBalanceSyncService.DEMO_HIGHER_BALANCE_PAISA,
        )
    }

    /**
     * Spec §3.3 stub — available by default; tests can construct
     * [StubGrokService] with `isUnavailable = true` for frame 5c.
     */
    @Provides
    @Singleton
    fun provideGrokService(): GrokService = StubGrokService()

    private const val DATA_STORE_FILE: String = "piplanner_demo"
}
