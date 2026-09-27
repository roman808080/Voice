package voice.core.data

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
public data class ThreadId(val value: String) {

  public companion object {
    public val Default: ThreadId = ThreadId("default")

    public fun random(): ThreadId = ThreadId(Uuid.random().toString())
  }
}
