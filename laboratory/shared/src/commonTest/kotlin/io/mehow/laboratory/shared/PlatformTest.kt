package io.mehow.laboratory.shared

import kotlin.test.Test
import kotlin.test.assertTrue

internal class PlatformTest {
  @Test
  fun platformReturnsNonEmptyString() {
    assertTrue(platform().isNotEmpty())
  }
}
