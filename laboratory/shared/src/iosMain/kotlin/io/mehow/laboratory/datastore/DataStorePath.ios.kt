package io.mehow.laboratory.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import io.mehow.laboratory.Storage
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

/**
 * Resolves the default data store file in the app's Application Support directory, under a
 * `laboratory/` subfolder so the file is private and not exposed via iTunes/Files sharing.
 */
@OptIn(ExperimentalForeignApi::class)
internal actual fun defaultDataStorePath(name: String): Path {
  val fileManager = NSFileManager.defaultManager
  val base: NSURL =
    fileManager.URLForDirectory(
      directory = NSApplicationSupportDirectory,
      inDomain = NSUserDomainMask,
      appropriateForURL = null,
      create = true,
      error = null,
    ) ?: error("Unable to resolve NSApplicationSupportDirectory for Laboratory data store")
  val root = requireNotNull(base.path) { "NSApplicationSupportDirectory URL has no path" }
  val parent = "$root/laboratory".toPath()
  // Ensure the parent folder exists; DataStore does not create intermediate directories.
  fileManager.createDirectoryAtPath(
    path = parent.toString(),
    withIntermediateDirectories = true,
    attributes = null,
    error = null,
  )
  return "$parent/$name.pb".toPath()
}

/**
 * Creates a [Storage] backed by a Protobuf-serialised [androidx.datastore.core.DataStore].
 *
 * The data file lives at `<Application Support>/laboratory/$name.pb`.
 */
public fun Storage.Companion.dataStore(name: String = "laboratory"): Storage {
  val path = defaultDataStorePath(name)
  val dataStore =
    DataStoreFactory.create(
      storage = OkioStorage(FileSystem.SYSTEM, StorageDataSerializer) { path }
    )
  return DataStoreStorage(dataStore)
}
