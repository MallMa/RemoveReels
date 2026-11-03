package io.github.mallma.removereels.storage

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A [DataStore] without a file, for tests; updates are serialised like the real one's. */
internal class InMemoryDataStore<T>(
    initial: T,
) : DataStore<T> {
    private val value = MutableStateFlow(initial)
    private val mutex = Mutex()

    override val data: Flow<T> = value

    override suspend fun updateData(transform: suspend (t: T) -> T): T =
        mutex.withLock { transform(value.value).also { value.value = it } }
}
