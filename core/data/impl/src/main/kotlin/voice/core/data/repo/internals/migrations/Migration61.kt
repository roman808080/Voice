package voice.core.data.repo.internals.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
public class Migration61 : IncrementalMigration(61) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE content2 ADD COLUMN activeThreadId TEXT NOT NULL DEFAULT 'default'")
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS bookThread (
        bookId TEXT NOT NULL,
        id TEXT NOT NULL,
        title TEXT,
        chapterId TEXT NOT NULL,
        positionInChapter INTEGER NOT NULL,
        addedAt TEXT NOT NULL,
        PRIMARY KEY(bookId, id)
      )
      """.trimIndent(),
    )
    db.execSQL("CREATE INDEX IF NOT EXISTS index_bookThread_bookId ON bookThread(bookId)")
    db.execSQL(
      """
      INSERT INTO bookThread (bookId, id, title, chapterId, positionInChapter, addedAt)
      SELECT id, 'default', NULL, currentChapter, positionInChapter, addedAt
      FROM content2
      """.trimIndent(),
    )
  }
}
