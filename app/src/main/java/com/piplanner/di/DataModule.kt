package com.piplanner.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.piplanner.data.local.DataStorePersistenceService
import com.piplanner.data.local.PersistenceService
import com.piplanner.domain.BalanceSyncService
import com.piplanner.domain.MockBalanceSyncService
import com.piplanner.util.DemoData
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

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
     * Known IDs match demo persona accounts (HDFC / SBI).
     */
    @Provides
    @Singleton
    fun provideBalanceSyncService(): BalanceSyncService {
        return MockBalanceSyncService(
            knownAccountIds = setOf(
                DemoData.DEMO_SAVINGS_ACCOUNT_ID,
                DemoData.DEMO_SPENDING_ACCOUNT_ID,
            ),
        )
    }

    private const val DATA_STORE_FILE: String = "piplanner_demo"
}
