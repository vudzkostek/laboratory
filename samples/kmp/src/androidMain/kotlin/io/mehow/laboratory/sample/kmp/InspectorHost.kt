package io.mehow.laboratory.sample.kmp

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.mehow.laboratory.Laboratory
import io.mehow.laboratory.Storage
import io.mehow.laboratory.datastore.dataStore
import io.mehow.laboratory.inspector.cmp.LaboratoryConfiguration
import io.mehow.laboratory.inspector.cmp.LaboratoryInspector

/**
 * Builds the sample [Laboratory] using DataStore-backed [Storage]. Call from your
 * `Application.onCreate` or any `ComponentActivity`.
 */
public fun buildSampleLaboratory(context: Context): Laboratory =
  createLaboratory(Storage.dataStore(context))

/** Convenience accessor so hosts can share a single configuration instance. */
public fun buildSampleInspectorConfiguration(laboratory: Laboratory): LaboratoryConfiguration =
  createInspectorConfiguration(laboratory)

/**
 * Composable wrapper that shows a button and routes to the Compose Multiplatform inspector
 * on tap. Mirrors the pattern we expect downstream debug builds to adopt.
 */
@Composable
public fun SampleInspectorScreen(configuration: LaboratoryConfiguration) {
  var showInspector by remember { mutableStateOf(false) }
  if (showInspector) {
    LaboratoryInspector(configuration)
  } else {
    Scaffold { padding ->
      Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Text(
          "Laboratory sample",
          style = MaterialTheme.typography.titleLarge,
        )
        Button(onClick = { showInspector = true }) { Text("Open inspector") }
      }
    }
  }
}
