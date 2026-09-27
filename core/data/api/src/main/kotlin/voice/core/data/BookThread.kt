package voice.core.data

import androidx.room.Entity
import androidx.room.Index
import java.time.Instant

@Entity(
  tableName = "bookThread",
  primaryKeys = ["bookId", "id"],
  indices = [Index("bookId")],
)
public data class BookThread(
  val bookId: BookId,
  val id: ThreadId,
  val title: String?,
  val chapterId: ChapterId,
  val positionInChapter: Long,
  val addedAt: Instant,
)
