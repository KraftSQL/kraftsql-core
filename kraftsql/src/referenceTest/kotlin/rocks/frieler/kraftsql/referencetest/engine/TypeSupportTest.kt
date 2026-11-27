package rocks.frieler.kraftsql.referencetest.engine

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.ConstantData
import rocks.frieler.kraftsql.engine.collect

class TypeSupportTest {
    @Test
    fun `Engine can collect non-nullable Int values`() {
        data class Value(val value: Int)
        val items = listOf(Value(0), Value(42), Value(Int.MIN_VALUE), Value(Int.MAX_VALUE))

        val collected = ConstantData(items).collect()

        collected shouldContainExactlyInAnyOrder items
    }

    @Test
    fun `Engine can collect nullable Int values`() {
        data class Value(val value: Int?)
        val items = listOf(Value(42), Value(null))

        val collected = ConstantData(items).collect()

        collected shouldContainExactlyInAnyOrder items
    }

    @Test
    fun `Engine can collect non-nullable String values`() {
        data class Value(val value: String)
        val items = listOf(Value(""), Value("foo"), Value("  padded  "), Value("it's \"quoted\""), Value("back\\slash"), Value("äöü 😀"))

        val collected = ConstantData(items).collect()

        collected shouldContainExactlyInAnyOrder items
    }

    @Test
    fun `Engine can collect nullable String values`() {
        data class Value(val value: String?)
        val items = listOf(Value("foo"), Value(""), Value(null))

        val collected = ConstantData(items).collect()

        collected shouldContainExactlyInAnyOrder items
    }

    @Test
    fun `Engine can collect non-nullable Boolean values`() {
        data class Value(val value: Boolean)
        val items = listOf(Value(true), Value(false))

        val collected = ConstantData(items).collect()

        collected shouldContainExactlyInAnyOrder items
    }

    @Test
    fun `Engine can collect nullable Boolean values, keeping null distinct from false`() {
        data class Value(val value: Boolean?)
        val items = listOf(Value(true), Value(false), Value(null))

        val collected = ConstantData(items).collect()

        collected shouldContainExactlyInAnyOrder items
    }
}
