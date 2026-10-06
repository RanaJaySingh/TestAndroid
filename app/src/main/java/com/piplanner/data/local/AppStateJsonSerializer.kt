package com.piplanner.data.local

import com.piplanner.data.model.AppState
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JSON encode/decode for [AppState]. Failures surface as [PersistenceException].
 */
@Singleton
class AppStateJsonSerializer @Inject constructor() {

    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(state: AppState): String = json.encodeToString(state)

    fun decode(raw: String): AppState {
        return try {
            json.decodeFromString(AppState.serializer(), raw)
        } catch (error: SerializationException) {
            throw PersistenceException("Failed to decode AppState JSON", error)
        } catch (error: IllegalArgumentException) {
            throw PersistenceException("Invalid AppState JSON", error)
        }
    }
}

class PersistenceException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
