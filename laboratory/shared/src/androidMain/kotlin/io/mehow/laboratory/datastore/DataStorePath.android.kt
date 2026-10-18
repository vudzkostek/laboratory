package io.mehow.laboratory.datastore

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import io.mehow.laboratory.Storage
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Not usable on Android — use [dataStore(Context, String)] instead.
 *
 * Android needs a [Context] to resolve the per-app files directory, so there is no sensible default
 * resolution from a bare [name].
 */
internal actual fun defaultDataStorePath(name: String): Path {
  throw UnsupportedOperationException(
    "Use Storage.Companion.dataStore(context) on Android — a Context is required to resolve files dir."
  )
}

/**
 * Creates a [Storage] backed by a Protobuf-serialised [androidx.datastore.core.DataStore].
 *
 * The data file lives at `context.filesDir/datastore/$name.pb`.
 */
public fun Storage.Companion.dataStore(
  context: Context,
  name: String = "laboratory",
): Storage {
  val path = "${context.filesDir.absolutePath}/datastore/$name.pb".toPath()
  val dataStore =
    DataStoreFactory.create(
      storage = OkioStorage(FileSystem.SYSTEM, StorageDataSerializer) { path }
    )
  return DataStoreStorage(dataStore)
}
