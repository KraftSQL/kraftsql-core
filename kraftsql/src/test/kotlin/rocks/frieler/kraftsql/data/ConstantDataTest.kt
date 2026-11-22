package rocks.frieler.kraftsql.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class ConstantDataTest {
    @Test
    fun `ConstantData can hold DataRows as items`() {
        val item1 = DataRow("c1" to 1, "c2" to 2)
        val item2 = DataRow("c1" to 11, "c2" to 22)

        val data = ConstantData(listOf(item1, item2))

        data.items shouldBe listOf(item1, item2)
        data.schema shouldBe listOf(Column("c1", Type.INTEGER), Column("c2", Type.INTEGER))
    }

    @Test
    fun `ConstantData can hold data-class instances as items`() {
        data class Item(val id: Int, val value: Int)
        val item1 = mockk<Item>()
        val item2 = mockk<Item>()

        val data = ConstantData(listOf(item1, item2))

        data.items shouldBe listOf(item1, item2)
        data.schema shouldBe listOf(Column("id", Type.INTEGER), Column("value", Type.INTEGER))
    }

    @Test
    fun `ConstantData rejects empty items`() {
        shouldThrow<IllegalArgumentException> {
            ConstantData(emptyList())
        }
    }

    @Test
    fun `ConstantData rejects items of different types`() {
        shouldThrow<IllegalArgumentException> {
            ConstantData(listOf(mockk<DataRow>(), mockk<Any>()))
        }
    }

    @Test
    fun `ConstantData rejects DataRow items with different columns`() {
        val item1 = mockk<DataRow> { every { this@mockk.fieldNames }.returns(listOf("c1", "c2")) }
        val item2 = mockk<DataRow> { every { this@mockk.fieldNames }.returns(listOf("x", "y")) }

        shouldThrow<IllegalArgumentException> {
            ConstantData(listOf(item1, item2))
        }
    }

    @Test
    fun `ConstantData infers schema with nullable fields from DataRows`() {
        val item1 = DataRow("c1" to 1, "c2" to null)
        val item2 = DataRow("c1" to null, "c2" to 22)

        val data = ConstantData(listOf(item1, item2))

        data.schema shouldBe listOf(Column("c1", Type.NULLABLE_INTEGER), Column("c2", Type.NULLABLE_INTEGER))
    }

    @Test
    fun `ConstantData can also be created from vararg items`() {
        val item1 = DataRow("c1" to 1, "c2" to 2)
        val item2 = DataRow("c1" to 11, "c2" to 22)

        val data = ConstantData(item1, item2)

        data.items shouldBe listOf(item1, item2)
        data.schema shouldBe listOf(Column("c1", Type.INTEGER), Column("c2", Type.INTEGER))
    }
}
