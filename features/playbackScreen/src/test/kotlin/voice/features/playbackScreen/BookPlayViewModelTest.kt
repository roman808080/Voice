package voice.features.playbackScreen

import android.net.Uri
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import app.cash.turbine.test
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.ChapterMark
import voice.core.data.KioskModeDemoData
import voice.core.data.MarkData
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.featureflag.MemoryFeatureFlag
import voice.core.playback.CurrentBookResolver
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.SubtitleCue
import voice.core.playback.SubtitleReader
import voice.core.playback.SubtitleSnapshot
import voice.core.playback.overlay
import voice.core.playback.playstate.PlayStateManager
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerMode.TimedWithDuration
import voice.core.sleeptimer.SleepTimerState
import voice.features.sleepTimer.SleepTimerViewState
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class BookPlayViewModelTest {

  private val scope = TestScope()
  private val sleepTimerDataStore = MemoryDataStore(SleepTimerPreference.Default.copy(duration = 5.minutes))
  private val book = book()
  private val sleepTimer = mockk<SleepTimer> {
    val stateFlow = MutableStateFlow<SleepTimerState>(SleepTimerState.Disabled)
    every {
      state
    } returns stateFlow
    every {
      enable(any())
    } answers {
      stateFlow.value = when (val mode = firstArg<SleepTimerMode>()) {
        is TimedWithDuration -> SleepTimerState.Enabled.WithDuration(mode.duration)
        SleepTimerMode.TimedWithDefault -> SleepTimerState.Enabled.WithDuration(runBlocking { sleepTimerDataStore.data.first() }.duration)
        SleepTimerMode.EndOfChapter -> SleepTimerState.Enabled.WithEndOfChapter
      }
    }
    every {
      disable()
    } answers {
      stateFlow.value = SleepTimerState.Disabled
    }
  }

  private val player = mockk<PlayerController>()
    .also {
      every { it.subtitleFlow(book.id) } returns flowOf(SubtitleSnapshot(emptyList(), null))
    }
  private val playStateManager = mockk<PlayStateManager> {
    every { playStateFlow } returns MutableStateFlow(PlayStateManager.PlayState.Paused)
  }
  private val currentBookStoreId = MemoryDataStore<BookId?>(null)
  private val currentBookResolver = mockk<CurrentBookResolver> {
    coEvery { book(book.id) } returns book
  }
  private val viewModel = BookPlayViewModel(
    bookRepository = mockk {
      coEvery { get(book.id) } returns book
      every { flow(book.id) } returns MutableStateFlow(book)
    },
    currentBookResolver = currentBookResolver,
    player = player.apply {
      every { pauseIfCurrentBookDifferentFrom(book.id) } just Runs
    },
    subtitleReader = mockk {
      coEvery { read(any()) } returns emptyList()
    },
    sleepTimer = sleepTimer,
    playStateManager = playStateManager,
    currentBookStoreId = currentBookStoreId,
    navigator = mockk(),
    bookmarkRepository = mockk {
      coEvery { addBookmarkAtBookPosition(book, any(), any()) } returns Bookmark(
        bookId = book.id,
        chapterId = book.currentChapter.id,
        addedAt = Instant.now(),
        setBySleepTimer = true,
        id = Bookmark.Id(Uuid.random()),
        time = 0L,
        title = null,
      )
    },
    volumeGainFormatter = mockk(),
    batteryOptimization = mockk(),
    sleepTimerPreferenceStore = sleepTimerDataStore,
    bookId = book.id,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
    experimentalPlaybackPersistenceFeatureFlag = MemoryFeatureFlag(false),
    kioskModeFeatureFlag = MemoryFeatureFlag(false),
  )

  @Test
  fun sleepTimerValueChanging() = scope.runTest {
    fun assertDialogSleepTime(expected: Int) {
      assertEquals(expected = BookPlayDialogViewState.SleepTimer(SleepTimerViewState(expected)), actual = viewModel.dialogState.value)
    }

    viewModel.toggleSleepTimer()
    yield()
    assertDialogSleepTime(5)

    suspend fun incrementAndAssert(time: Int) {
      viewModel.incrementSleepTime()
      yield()
      assertDialogSleepTime(time)
    }

    suspend fun decrementAndAssert(time: Int) {
      viewModel.decrementSleepTime()
      yield()
      assertDialogSleepTime(time)
    }

    decrementAndAssert(4)
    decrementAndAssert(3)
    decrementAndAssert(2)
    decrementAndAssert(1)

    decrementAndAssert(1)

    incrementAndAssert(2)
    incrementAndAssert(3)
  }

  @Test
  fun sleepTimerSettingFixedValue() = scope.runTest {
    viewModel.toggleSleepTimer()
    viewModel.onAcceptSleepTime(10)
    assertEquals(expected = 5.minutes, actual = sleepTimerDataStore.data.first().duration)
    yield()
    verify(exactly = 1) {
      sleepTimer.enable(TimedWithDuration(10.minutes))
    }
  }

  @Test
  fun deactivateSleepTimer() = scope.runTest {
    viewModel.toggleSleepTimer()
    viewModel.onAcceptSleepTime(10)
    viewModel.toggleSleepTimer()
    yield()
    verifyOrder {
      sleepTimer.enable(TimedWithDuration(10.minutes))
      sleepTimer.disable()
    }
    assertIs<SleepTimerState.Disabled>(sleepTimer.state.value)
  }

  @Test
  fun onCurrentChapterClickShowsDialogWithCorrectState() = scope.runTest {
    viewModel.onCurrentChapterClick()
    yield()

    val dialogState = assertIs<BookPlayDialogViewState.SelectChapterDialog>(viewModel.dialogState.value)

    assertEquals(
      expected = listOf(
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 1,
          name = "Chapter Start",
          active = false,
          time = "0:00",
        ),
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 2,
          name = "Middle Section",
          active = false,
          time = "2:00",
        ),
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 3,
          name = "Final Section",
          active = false,
          time = "4:00",
        ),
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 4,
          name = "Chapter Start",
          active = false,
          time = "5:00",
        ),
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 5,
          name = "Middle Section",
          active = true,
          time = "7:00",
        ),
        BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
          number = 6,
          name = "Final Section",
          active = false,
          time = "9:00",
        ),
      ),
      actual = dialogState.items,
    )
  }

  @Test
  fun onChapterClickSetsPositionAndDismissesDialog() = scope.runTest {
    every { player.setPosition(any(), any()) } just Runs

    viewModel.onCurrentChapterClick()
    yield()

    assertIs<BookPlayDialogViewState.SelectChapterDialog>(viewModel.dialogState.value)

    viewModel.onChapterClick(number = 2)
    yield()

    // Verify player.setPosition was called with correct parameters
    // The second mark starts at 2 minutes position in the first chapter
    verify(exactly = 1) {
      player.setPosition(time = 2.minutes.inWholeMilliseconds, id = book.chapters.first().id)
    }

    assertEquals(expected = null, actual = viewModel.dialogState.value)
  }

  @Test
  fun `overlay prefers live controller position`() {
    val persistedBook = book()
    val overlaidBook = persistedBook.overlay(
      LivePlaybackState(
        bookId = persistedBook.id,
        chapterId = persistedBook.chapters.first().id,
        positionMs = 1.minutes.inWholeMilliseconds,
        isPlaying = true,
        playbackSpeed = 1F,
      ),
    )

    assertEquals(expected = persistedBook.chapters.first().id, actual = overlaidBook.currentChapter.id)
    assertEquals(expected = 1.minutes.inWholeMilliseconds, actual = overlaidBook.content.positionInChapter)
  }

  @Test
  fun `viewState prefers live playback state when feature flag is enabled`() = scope.runTest {
    val persistedBook = book()
    val livePlaybackFlow = MutableStateFlow<LivePlaybackState?>(null)
    val viewModel = viewModel(
      book = persistedBook,
      experimentalPlaybackPersistence = true,
      livePlaybackFlow = livePlaybackFlow,
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(expected = null, actual = awaitItem())
      assertEquals(expected = 30.seconds, actual = awaitItem()!!.playedTime)

      livePlaybackFlow.value = LivePlaybackState(
        bookId = persistedBook.id,
        chapterId = persistedBook.chapters.first().id,
        positionMs = 1.minutes.inWholeMilliseconds,
        isPlaying = true,
        playbackSpeed = 1F,
      )

      val state = awaitItem()!!
      assertEquals(expected = true, actual = state.playing)
      assertEquals(expected = "Chapter Start", actual = state.chapterName)
      assertEquals(expected = 1.minutes, actual = state.playedTime)
    }
  }

  @Test
  fun `viewState falls back to manager play state when live playback is unavailable`() = scope.runTest {
    val viewModel = viewModel(
      experimentalPlaybackPersistence = true,
      livePlaybackFlow = MutableStateFlow(null),
      playStateFlow = MutableStateFlow(PlayStateManager.PlayState.Playing),
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(expected = null, actual = awaitItem())
      val state = awaitItem()!!
      assertEquals(expected = true, actual = state.playing)
      assertEquals(expected = 30.seconds, actual = state.playedTime)
    }
  }

  @Test
  fun `viewState exposes current subtitles`() = scope.runTest {
    val subtitles = MutableStateFlow(SubtitleSnapshot(emptyList(), null))
    val viewModel = viewModel(subtitleFlow = subtitles)

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(expected = null, actual = awaitItem())
      assertEquals(expected = emptyList(), actual = awaitItem()!!.subtitles)

      subtitles.value = SubtitleSnapshot(listOf("First cue", "Second cue"), 150_000)
      assertEquals(expected = subtitles.value.texts, actual = awaitItem()!!.subtitles)
    }
  }

  @Test
  fun `transcript contains current section and locates active cue`() {
    val chapterId = ChapterId("chapter")
    val cues = listOf(
      SubtitleCue(startMs = 110_000, endMs = 130_000, text = "Crosses section start"),
      SubtitleCue(startMs = 140_000, endMs = 160_000, text = "Current"),
      SubtitleCue(startMs = 180_000, endMs = 190_000, text = "Upcoming"),
      SubtitleCue(startMs = 250_000, endMs = 260_000, text = "Next section"),
    ).toTranscriptCues(
      chapterId = chapterId,
      currentMark = ChapterMark(name = "Middle", startMs = 120_000, endMs = 239_999),
    )
    val position = cues.transcriptPosition(positionInChapterMs = 150_000)

    assertEquals(expected = listOf("Crosses section start", "Current", "Upcoming"), actual = cues.map { it.text })
    assertEquals(expected = listOf("0:00", "0:20", "1:00"), actual = cues.map { it.timestamp })
    assertEquals(expected = 120_000, actual = cues.first().position.inWholeMilliseconds)
    assertEquals(expected = setOf(1), actual = position.activeCueIndices)
    assertEquals(expected = 1, actual = position.currentCueIndex)
  }

  @Test
  fun `transcript locates next cue between subtitles`() {
    val cues = listOf(
      SubtitleCue(startMs = 10_000, endMs = 20_000, text = "Past"),
      SubtitleCue(startMs = 30_000, endMs = 40_000, text = "Next"),
    ).toTranscriptCues(
      chapterId = ChapterId("chapter"),
      currentMark = ChapterMark(name = "Chapter", startMs = 0, endMs = 60_000),
    )
    val position = cues.transcriptPosition(positionInChapterMs = 25_000)

    assertEquals(expected = 1, actual = position.currentCueIndex)
  }

  @Test
  fun `transcript uses player subtitle events for active cue`() {
    val cues = listOf(
      SubtitleCue(startMs = 10_000, endMs = 10_100, text = "Short cue"),
      SubtitleCue(startMs = 20_000, endMs = 30_000, text = "Later cue"),
    ).toTranscriptCues(
      chapterId = ChapterId("chapter"),
      currentMark = ChapterMark(name = "Chapter", startMs = 0, endMs = 60_000),
    )

    val position = cues.transcriptPosition(
      positionInChapterMs = 9_900,
      activeSubtitles = listOf("Short cue"),
    )

    assertEquals(expected = setOf(0), actual = position.activeCueIndices)
    assertEquals(expected = 0, actual = position.currentCueIndex)
  }

  @Test
  fun `transcript navigation uses timing when player reports no active subtitle`() {
    val cues = listOf(
      SubtitleCue(startMs = 10_000, endMs = 20_000, text = "Current cue"),
      SubtitleCue(startMs = 30_000, endMs = 40_000, text = "Next cue"),
    ).toTranscriptCues(
      chapterId = ChapterId("chapter"),
      currentMark = ChapterMark(name = "Chapter", startMs = 0, endMs = 60_000),
    )

    val position = cues.transcriptPosition(
      positionInChapterMs = 15_000,
      activeSubtitles = emptyList(),
      activeSubtitlePositionInChapterMs = 15_000,
    )

    assertEquals(expected = emptySet(), actual = position.activeCueIndices)
    assertEquals(expected = 0, actual = position.currentCueIndex)
  }

  @Test
  fun `transcript uses subtitle event position for repeated cues`() {
    val cues = listOf(
      SubtitleCue(startMs = 10_000, endMs = 10_200, text = "Repeated"),
      SubtitleCue(startMs = 10_200, endMs = 10_300, text = "Repeated"),
    ).toTranscriptCues(
      chapterId = ChapterId("chapter"),
      currentMark = ChapterMark(name = "Chapter", startMs = 0, endMs = 60_000),
    )

    val position = cues.transcriptPosition(
      positionInChapterMs = 10_050,
      activeSubtitles = listOf("Repeated"),
      activeSubtitlePositionInChapterMs = 10_200,
    )

    assertEquals(expected = setOf(1), actual = position.activeCueIndices)
    assertEquals(expected = 1, actual = position.currentCueIndex)
  }

  @Test
  fun `viewState clears old transcript while subtitle uri replacement loads`() = scope.runTest {
    val oldUri = mockk<Uri>()
    val newUri = mockk<Uri>()
    val currentChapterId = book.currentChapter.id
    val initialBook = book.withSubtitleUri(currentChapterId, oldUri)
    val bookFlow = MutableStateFlow(initialBook)
    val replacementCues = CompletableDeferred<List<SubtitleCue>>()
    val subtitleReader = mockk<SubtitleReader> {
      coEvery { read(oldUri) } returns listOf(
        SubtitleCue(startMs = 140_000, endMs = 160_000, text = "Old cue"),
      )
      coEvery { read(newUri) } coAnswers { replacementCues.await() }
    }
    val viewModel = viewModel(
      book = initialBook,
      bookFlow = bookFlow,
      subtitleReader = subtitleReader,
    )

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      assertEquals(expected = null, actual = awaitItem())
      var state = awaitItem()!!
      while (state.transcriptCues.none { it.text == "Old cue" }) {
        state = awaitItem()!!
      }

      bookFlow.value = initialBook.withSubtitleUri(currentChapterId, newUri)
      state = awaitItem()!!
      assertEquals(expected = emptyList(), actual = state.transcriptCues)

      replacementCues.complete(
        listOf(SubtitleCue(startMs = 145_000, endMs = 155_000, text = "New cue")),
      )
      state = awaitItem()!!
      assertEquals(expected = listOf("New cue"), actual = state.transcriptCues.map { it.text })
    }
  }

  @Test
  fun `subtitle click seeks without changing playback`() = scope.runTest {
    every { player.setPosition(any(), any()) } just Runs

    viewModel.seekToSubtitle(book.currentChapter.id, 140.seconds)

    verify(exactly = 1) {
      player.setPosition(time = 140.seconds.inWholeMilliseconds, id = book.currentChapter.id)
    }
    verify(exactly = 0) { player.playPause() }
  }

  @Test
  fun `viewState uses currently playing demo book in kiosk mode`() = scope.runTest {
    val viewModel = viewModel(kioskMode = true)

    backgroundScope.launchMolecule(RecompositionMode.Immediate) {
      viewModel.viewState()
    }.test {
      val state = awaitItem()!!
      assertEquals(expected = KioskModeDemoData.currentlyPlaying.title, actual = state.title)
      assertEquals(expected = KioskModeDemoData.currentlyPlaying.chapter, actual = state.chapterName)
      assertEquals(expected = KioskModeDemoData.currentlyPlaying.coverUrl, actual = state.cover)
    }
  }

  private fun viewModel(
    book: Book = this.book,
    bookFlow: MutableStateFlow<Book> = MutableStateFlow(book),
    subtitleReader: SubtitleReader = mockk {
      coEvery { read(any()) } returns emptyList()
    },
    experimentalPlaybackPersistence: Boolean = false,
    kioskMode: Boolean = false,
    livePlaybackFlow: MutableStateFlow<LivePlaybackState?> = MutableStateFlow(null),
    playStateFlow: MutableStateFlow<PlayStateManager.PlayState> = MutableStateFlow(PlayStateManager.PlayState.Paused),
    subtitleFlow: MutableStateFlow<SubtitleSnapshot> = MutableStateFlow(SubtitleSnapshot(emptyList(), null)),
  ): BookPlayViewModel {
    return BookPlayViewModel(
      bookRepository = mockk {
        coEvery { get(book.id) } returns book
        every { flow(book.id) } returns bookFlow
      },
      currentBookResolver = currentBookResolver,
      player = mockk {
        every { pauseIfCurrentBookDifferentFrom(book.id) } just Runs
        every { livePlaybackStateFlow(book.id) } returns livePlaybackFlow
        every { subtitleFlow(book.id) } returns subtitleFlow
      },
      subtitleReader = subtitleReader,
      sleepTimer = sleepTimer,
      playStateManager = mockk {
        every { this@mockk.playStateFlow } returns playStateFlow
        every { playState } returns playStateFlow.value
      },
      currentBookStoreId = MemoryDataStore(null),
      navigator = mockk(),
      bookmarkRepository = mockk(),
      volumeGainFormatter = mockk(),
      batteryOptimization = mockk(),
      sleepTimerPreferenceStore = sleepTimerDataStore,
      bookId = book.id,
      dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
      experimentalPlaybackPersistenceFeatureFlag = MemoryFeatureFlag(experimentalPlaybackPersistence),
      kioskModeFeatureFlag = MemoryFeatureFlag(kioskMode),
    )
  }
}

private fun Book.withSubtitleUri(chapterId: ChapterId, subtitleUri: Uri): Book {
  return copy(
    chapters = chapters.map { chapter ->
      if (chapter.id == chapterId) chapter.copy(subtitleUri = subtitleUri) else chapter
    },
  )
}

private fun book(
  name: String = "TestBook",
  lastPlayedAtMillis: Long = 0L,
  addedAtMillis: Long = 0L,
): Book {
  val chapters = listOf(
    chapter(),
    chapter(),
  )
  return Book(
    content = BookContent(
      author = Uuid.random().toString(),
      name = name,
      positionInChapter = 2.5.minutes.inWholeMilliseconds,
      playbackSpeed = 1F,
      addedAt = Instant.ofEpochMilli(addedAtMillis),
      chapters = chapters.map { it.id },
      cover = null,
      currentChapter = chapters[1].id,
      isActive = true,
      lastPlayedAt = Instant.ofEpochMilli(lastPlayedAtMillis),
      skipSilence = false,
      id = BookId(Uuid.random().toString()),
      gain = 0F,
      genre = null,
      narrator = null,
      series = null,
      part = null,
    ),
    chapters = chapters,
  )
}

private fun chapter(): Chapter {
  return Chapter(
    id = ChapterId("http://${Uuid.random()}"),
    duration = 5.minutes.inWholeMilliseconds,
    fileLastModified = Instant.EPOCH,
    markData = listOf(
      MarkData(startMs = 0L, name = "Chapter Start"),
      MarkData(startMs = 2.minutes.inWholeMilliseconds, name = "Middle Section"),
      MarkData(startMs = 4.minutes.inWholeMilliseconds, name = "Final Section"),
    ),
    name = "name",
    fileSize = 0,
  )
}
