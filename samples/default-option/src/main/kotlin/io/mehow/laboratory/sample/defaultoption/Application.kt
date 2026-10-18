package io.mehow.laboratory.sample.defaultoption

import android.app.Application as AndroidApplication
import android.content.Context
import io.mehow.laboratory.DefaultOptionFactory
import io.mehow.laboratory.FeatureFactory
import io.mehow.laboratory.Laboratory
import io.mehow.laboratory.Storage
import io.mehow.laboratory.datastore.dataStore
import io.mehow.laboratory.inspector.LaboratoryActivity

class Application : AndroidApplication() {
  private lateinit var laboratory: Laboratory

  override fun onCreate() {
    super.onCreate()
    val storage = Storage.dataStore(this)
    laboratory =
      Laboratory.builder()
        .localStorage(storage)
        .defaultOptionFactory(DefaultOptionFactory.create())
        .build()
    LaboratoryActivity.configure(laboratory, FeatureFactory.generated())
  }

  companion object {
    val Context.laboratory
      get() = (applicationContext as Application).laboratory
  }
}
