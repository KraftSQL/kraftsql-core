package rocks.frieler.kraftsql.local.engine

import io.kotest.matchers.collections.shouldContainExactly
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.ConstantData
import rocks.frieler.kraftsql.data.Data
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.Selection
import rocks.frieler.kraftsql.expressions.get

class KraftSqlLocalEngineTest {
    private val engine = KraftSqlLocalEngine()

    @Test
    fun `collect() can collect ConstantData of DataRows`() {
        val items = listOf(mockk<DataRow>(), mockk<DataRow>())
        val data = mockk<ConstantData<DataRow>> { every { this@mockk.items } returns items }

        val collectedData = engine.collect(data, DataRow::class)

        collectedData shouldContainExactly items
    }

    @Test
    fun `collect() can collect ConstantData of data-class instances`() {
        data class Answer(val answer: Int)
        val item = Answer(42)
        @Suppress("UNCHECKED_CAST") val data = mockk<ConstantData<Answer>> { every { this@mockk.items } returns listOf(item) } as Data<DataRow>

        val collectedData = engine.collect(data, DataRow::class)

        collectedData shouldContainExactly listOf(DataRow("answer" to 42))
    }

    @Test
    fun `collect() can collect a Selection of columns`() {
        val source = ConstantData(
            DataRow("id" to 1, "name" to "foo", "category" to "bar"),
            DataRow("id" to 2, "name" to "baz", "category" to "qux"),
        )
        val selection = Selection(source, listOf(source.get<String>("category"), source.get<Int>("id")))

        val collectedData = engine.collect(selection, DataRow::class)

        collectedData shouldContainExactly listOf(
            DataRow("category" to "bar", "id" to 1),
            DataRow("category" to "qux", "id" to 2),
        )
    }

    @Test
    fun `collect() can collect values of all supported types`() {
        data class AllTypes(val int: Int, val nullableInt: Int?, val string: String, val nullableString: String?)
        val items = listOf(
            AllTypes(42, 42, "foo", "foo"),
            AllTypes(0, null, "", ""),
            AllTypes(-1, 0, "it's \"quoted\"", null),
        )
        val data = mockk<ConstantData<AllTypes>> { every { this@mockk.items } returns items }

        val collectedData = engine.collect(data, AllTypes::class)

        collectedData shouldContainExactly items
    }

    @Test
    fun `collect() can collect Data as data-class instances`() {
        data class Answer(val answer: Int)
        val item = DataRow("answer" to 42)
        @Suppress("UNCHECKED_CAST") val data = mockk<ConstantData<DataRow>> { every { this@mockk.items } returns listOf(item) } as Data<Answer>

        val collectedData = engine.collect(data, Answer::class)

        collectedData shouldContainExactly listOf(Answer(42))
    }
}
