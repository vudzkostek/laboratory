package io.mehow.laboratory.inspector.cmp

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.mehow.laboratory.Feature
import io.mehow.laboratory.Laboratory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Entry-point composable for the Laboratory Compose Multiplatform inspector.
 *
 * Renders a top tab row (one tab per [LaboratoryConfiguration.featureFactories] entry), a search
 * field, and a list of features with a horizontal strip of option chips. Tapping a chip calls
 * [Laboratory.setOption] immediately; the chip row observes the Laboratory's current option through
 * [Laboratory.observeRaw] so edits made elsewhere in the app surface here too.
 */
@Composable
public fun LaboratoryInspector(configuration: LaboratoryConfiguration) {
  Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
      InspectorContent(configuration = configuration, contentPadding = padding)
    }
  }
}

@Composable
private fun InspectorContent(
  configuration: LaboratoryConfiguration,
  contentPadding: PaddingValues,
) {
  val sections = remember(configuration) { configuration.featureFactories.entries.toList() }
  var selectedTab by remember(configuration) { mutableStateOf(0) }
  val query = remember(configuration) { mutableStateOf("") }

  Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
    if (sections.size > 1) {
      PrimaryTabRow(selectedTabIndex = selectedTab) {
        sections.forEachIndexed { index, (name, _) ->
          Tab(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            text = { Text(text = name) },
          )
        }
      }
    }

    OutlinedTextField(
      value = query.value,
      onValueChange = { query.value = it },
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      label = { Text(text = "Search features") },
      singleLine = true,
    )

    val sectionIndex = selectedTab.coerceAtMost(sections.lastIndex)
    val (_, factory) = sections[sectionIndex]
    val metadata =
      remember(factory, configuration.deprecationHandler) {
        loadMetadata(factory, configuration.deprecationHandler)
      }
    val queryString by remember { derivedStateOf { QueryString.create(query.value) } }
    val visible by remember {
      derivedStateOf { metadata.filter { feature -> feature.matches(queryString) } }
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      items(items = visible, key = { it.name }) { feature ->
        FeatureRow(laboratory = configuration.laboratory, feature = feature)
      }
    }
  }
}

@Composable
private fun FeatureRow(laboratory: Laboratory, feature: FeatureMetadata) {
  val coroutineScope = rememberCoroutineScope()
  val fallback = remember(feature) { MutableStateFlow<Feature<*>>(feature.defaultOption) }
  val currentOption by
    remember(laboratory, feature) {
        @OptIn(io.mehow.laboratory.internal.InternalLaboratoryApi::class)
        laboratory.observeRaw(feature.type)
      }
      .collectAsState(initial = feature.defaultOption)

  // Seed the fallback flow with the latest observed value so the chip row stays correct across
  // recompositions (collectAsState emits the initial value on first collection too).
  LaunchedEffect(currentOption) { fallback.value = currentOption }

  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = feature.name,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.SemiBold,
      textDecoration =
        when (feature.style) {
          FeatureStyle.Strikethrough -> TextDecoration.LineThrough
          else -> TextDecoration.None
        },
    )
    if (feature.description.isNotBlank()) {
      Text(
        text = feature.description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(modifier = Modifier.height(4.dp))

    val scroll = rememberScrollState()
    Column(modifier = Modifier.horizontalScroll(scroll)) {
      androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (option in feature.options) {
          val selected = option.name == currentOption.name
          FilterChip(
            selected = selected,
            onClick = {
              coroutineScope.launch { laboratory.setOption(option) }
            },
            label = { Text(text = option.name) },
            colors = FilterChipDefaults.filterChipColors(),
          )
        }
      }
    }
  }
}
