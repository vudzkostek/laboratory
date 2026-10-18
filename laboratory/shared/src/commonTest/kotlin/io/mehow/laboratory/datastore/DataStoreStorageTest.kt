package io.mehow.laboratory.datastore

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import io.mehow.laboratory.Storage
import io.mehow.laboratory.testing.AbstractStorageTest
import kotlin.random.Random
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

class DataStoreStorageTest : AbstractStorageTest() {
  override fun storage(): Storage {
    val fileSystem = FakeFileSystem()
    val path = "/laboratory-${Random.nextLong().toULong().toString(16)}.pb".toPath()
    val dataStore =
      DataStoreFactory.create(storage = OkioStorage(fileSystem, StorageDataSerializer) { path })
    return Storage.dataStore(dataStore)
  }
}
