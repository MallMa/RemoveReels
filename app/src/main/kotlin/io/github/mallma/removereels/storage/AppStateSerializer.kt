package io.github.mallma.removereels.storage

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/** Reads and writes [AppState] as JSON. A file that cannot be read, or breaks a product rule, counts as corrupt. */
internal object AppStateSerializer : Serializer<AppState> {
    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = true
        }

    override val defaultValue: AppState = AppState.INITIAL

    override suspend fun readFrom(input: InputStream): AppState {
        val text = input.readBytes().decodeToString()
        return try {
            json.decodeFromString<StoredState>(text).toAppState()
        } catch (e: IllegalArgumentException) {
            // Also catches kotlinx.serialization's SerializationException, a subclass.
            throw CorruptionException("Stored state is unreadable or breaks a product rule", e)
        }
    }

    override suspend fun writeTo(
        t: AppState,
        output: OutputStream,
    ) {
        output.write(json.encodeToString(t.toStoredState()).encodeToByteArray())
    }
}
