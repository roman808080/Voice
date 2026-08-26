package voice.core.scanner

import dev.zacsweers.metro.Inject
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.isAudioFile
import voice.core.data.isSubRipFile
import voice.core.data.repo.ChapterRepo
import voice.core.data.repo.getOrPut
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.nameWithoutExtension
import java.time.Instant

internal data class ChapterParseResult(
  val chapters: List<Chapter>,
  val firstChapterMetadata: Metadata?,
)

@Inject
internal class ChapterParser(
  private val chapterRepo: ChapterRepo,
  private val mediaAnalyzer: MediaAnalyzer,
) {

  suspend fun parse(documentFile: CachedDocumentFile): ChapterParseResult {
    val result = mutableListOf<Chapter>()
    val analyzedMetadata = mutableMapOf<ChapterId, Metadata>()

    suspend fun parseChapters(
      file: CachedDocumentFile,
      siblings: List<CachedDocumentFile>,
    ) {
      if (file.isAudioFile()) {
        val id = ChapterId(file.uri)
        val subtitleUri = siblings
          .filter { sibling ->
            sibling.isSubRipFile() && sibling.nameWithoutExtension().equals(file.nameWithoutExtension(), ignoreCase = true)
          }
          .singleOrNull()
          ?.uri
        var chapter = chapterRepo.getOrPut(
          id = id,
          lastModified = Instant.ofEpochMilli(file.lastModified),
          fileSize = file.length,
        ) {
          val metaData = mediaAnalyzer.analyze(file) ?: return@getOrPut null
          analyzedMetadata[id] = metaData
          Chapter(
            id = id,
            duration = metaData.duration,
            fileLastModified = Instant.ofEpochMilli(file.lastModified),
            name = metaData.title ?: metaData.fileName,
            markData = metaData.chapters,
            fileSize = file.length,
            subtitleUri = subtitleUri,
          )
        }
        if (chapter != null && chapter.subtitleUri != subtitleUri) {
          chapter = chapter.copy(subtitleUri = subtitleUri)
          chapterRepo.put(chapter)
        }
        if (chapter != null) {
          result.add(chapter)
        }
      } else if (file.isDirectory) {
        val children = file.children
        children.forEach {
          parseChapters(file = it, siblings = children)
        }
      }
    }

    parseChapters(file = documentFile, siblings = emptyList())
    val chapters = result.sorted()
    return ChapterParseResult(
      chapters = chapters,
      firstChapterMetadata = chapters.firstOrNull()?.let { analyzedMetadata[it.id] },
    )
  }
}
