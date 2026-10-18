package io.mehow.laboratory.inspector.cmp

/**
 * Case-insensitive, letter-and-digit-only subsequence matcher used by the inspector's search bar.
 *
 * Mirrors the behavior of the Android `:inspector` module's `QueryString` so filtering lands with
 * the same affordances on both inspectors.
 */
internal class QueryString private constructor(private val value: String) {
  val length: Int
    get() = value.length

  fun isEmpty(): Boolean = value.isEmpty()

  fun isNotEmpty(): Boolean = value.isNotEmpty()

  operator fun get(index: Int): Char = value[index]

  operator fun contains(other: QueryString): Boolean =
    when {
      other.isEmpty() -> true
      this.isEmpty() -> false
      else ->
        value.asSequence().runningFold(0, accumulateMatchingLength(other)).any { matchingLength ->
          matchingLength == other.length
        }
    }

  companion object {
    val Empty: QueryString = QueryString("")

    fun create(value: String): QueryString {
      val cleanValue = buildString {
        for (character in value) {
          if (character.isLetterOrDigit()) {
            append(character.lowercase())
          }
        }
      }
      return QueryString(cleanValue)
    }
  }
}

private fun accumulateMatchingLength(other: QueryString) = { index: Int, char: Char ->
  if (index < other.length && char == other[index]) {
    index + 1
  } else {
    index
  }
}
