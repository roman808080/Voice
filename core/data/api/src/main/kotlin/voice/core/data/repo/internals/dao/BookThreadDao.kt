package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.BookThread
import voice.core.data.ThreadId

@Dao
public interface BookThreadDao {

  @Query("SELECT * FROM bookThread WHERE bookId = :bookId ORDER BY addedAt")
  public fun flow(bookId: BookId): Flow<List<BookThread>>

  @Query("SELECT * FROM bookThread WHERE bookId = :bookId ORDER BY addedAt")
  public suspend fun all(bookId: BookId): List<BookThread>

  @Query("SELECT * FROM bookThread WHERE bookId = :bookId AND id = :threadId")
  public suspend fun get(
    bookId: BookId,
    threadId: ThreadId,
  ): BookThread?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  public suspend fun insert(thread: BookThread)

  @Query("DELETE FROM bookThread WHERE bookId = :bookId AND id = :threadId")
  public suspend fun delete(
    bookId: BookId,
    threadId: ThreadId,
  )
}
