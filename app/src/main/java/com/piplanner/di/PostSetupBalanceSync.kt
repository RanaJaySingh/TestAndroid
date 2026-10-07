package com.piplanner.di

import javax.inject.Qualifier

/** Qualifier for Goals-tab Sync/Update mock that returns a higher demo balance. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PostSetupBalanceSync
