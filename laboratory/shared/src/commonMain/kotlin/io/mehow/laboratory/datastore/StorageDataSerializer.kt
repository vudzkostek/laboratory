package io.mehow.laboratory.datastore

import androidx.datastore.core.okio.OkioSerializer
import okio.BufferedSink
import okio.BufferedSource

/**
 * [OkioSerializer] that is capable of writing and reading of [DataStoreStorage] data. It can be
 * used, for example, as a delegate of an encryption serializer.
 */
public object StorageDataSerializer : OkioSerializer<StorageData> {
  override val defaultValue: StorageData = StorageData()

  override suspend fun readFrom(source: BufferedSource): StorageData {
    return StorageData.ADAPTER.decode(source)
  }

  override suspend fun writeTo(t: StorageData, sink: BufferedSink) {
    StorageData.ADAPTER.encode(sink, t)
  }
}
