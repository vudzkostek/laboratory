package io.mehow.laboratory.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import io.mehow.laboratory.Storage
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Resolves the default data store file under the user's home directory (`~/.laboratory/<name>.pb`).
 */
internal actual fun defaultDataStorePath(name: String): Path {
  val home = System.getProperty("user.home") ?: "."
  return "$home/.laboratory/$name.pb".toPath()
}

/**
 * Creates a [Storage] backed by a Protobuf-serialised [androidx.datastore.core.DataStore].
 *
 * The data file lives at `~/.laboratory/$name.pb`.
 */
public fun Storage.Companion.dataStore(name: String = "laboratory"): Storage {
  val path = defaultDataStorePath(name)
  val dataStore =
    DataStoreFactory.create(
      storage = OkioStorage(FileSystem.SYSTEM, StorageDataSerializer) { path }
    )
  return DataStoreStorage(dataStore)
}
