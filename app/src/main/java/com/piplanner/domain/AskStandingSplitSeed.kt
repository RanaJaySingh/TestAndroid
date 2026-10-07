package com.piplanner.domain

import com.piplanner.data.model.StandingSplit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One-shot seed so Ask ChangeSplit / unavailable templates can open Standing split
 * with proposed percentages (optional PIP-64 review suggestion).
 */
@Singleton
class AskStandingSplitSeed @Inject constructor() {
    @Volatile
    private var pending: List<StandingSplit>? = null

    fun set(splits: List<StandingSplit>) {
        pending = splits
    }

    fun take(): List<StandingSplit>? {
        val value = pending
        pending = null
        return value
    }
}
