package voice.features.bookmark

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet

class MemoryDataStore<T>(initial: T) : DataStore<T> {
  private val state = MutableStateFlow(initial)

  override val data: Flow<T> = state

  override suspend fun updateData(transform: suspend (t: T) -> T): T {
    return state.updateAndGet { transform(it) }
  }
}
