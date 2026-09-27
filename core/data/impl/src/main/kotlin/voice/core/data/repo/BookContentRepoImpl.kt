package voice.core.data.repo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.repo.internals.dao.BookContentDao

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
public class BookContentRepoImpl(private val dao: BookContentDao) : BookContentRepo {

  private val cacheMutex = Mutex()
  private val mutationMutex = Mutex()
  private var cacheFilled = false
  private val cache = MutableStateFlow<List<BookContent>?>(null)

  private suspend fun fillCache() {
    if (cacheFilled) return
    cacheMutex.withLock {
      if (cacheFilled) return@withLock
      cache.value = dao.all()
      cacheFilled = true
    }
  }

  override fun flow(): Flow<List<BookContent>> {
    return cache.onStart { fillCache() }.filterNotNull()
  }

  override suspend fun all(): List<BookContent> {
    fillCache()
    return cache.value!!
  }

  override fun flow(id: BookId): Flow<BookContent?> {
    return cache.onStart { fillCache() }
      .filterNotNull()
      .map { contents -> contents.find { it.id == id } }
      .distinctUntilChanged()
  }

  override suspend fun get(id: BookId): BookContent? {
    fillCache()
    return cache.value!!.find { it.id == id }
  }

  override suspend fun setAllInactiveExcept(ids: List<BookId>) {
    mutationMutex.withLock {
      fillCache()
      val updated = cache.value!!.map { content ->
        content.copy(isActive = content.id in ids)
      }
      updated.forEach { dao.insert(it) }
      cache.value = updated
    }
  }

  override suspend fun put(content: BookContent) {
    mutationMutex.withLock {
      fillCache()
      putLocked(content)
    }
  }

  override suspend fun update(
    id: BookId,
    update: (BookContent) -> BookContent,
  ): Unit = mutationMutex.withLock {
    fillCache()
    val current = cache.value!!.find { it.id == id } ?: return@withLock
    val updated = update(current)
    if (updated != current) putLocked(updated)
  }

  private suspend fun putLocked(content: BookContent) {
    dao.insert(content)
    cache.update { contents ->
      contents!!.filterNot { it.id == content.id } + content
    }
  }
}
