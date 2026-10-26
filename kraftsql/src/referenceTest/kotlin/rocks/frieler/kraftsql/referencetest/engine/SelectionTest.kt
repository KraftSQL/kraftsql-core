package rocks.frieler.kraftsql.referencetest.engine

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.ConstantData
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.named
import rocks.frieler.kraftsql.data.select
import rocks.frieler.kraftsql.engine.collect
import rocks.frieler.kraftsql.expressions.Column
import rocks.frieler.kraftsql.expressions.get

class SelectionTest {
    @Test
    fun `Engine can select columns`() {
        val data = ConstantData(
            DataRow("id" to 1, "name" to "Something", "category" to "stuff"),
            DataRow("id" to 2, "name" to "Something else", "category" to "stuff"),
            DataRow("id" to 3, "name" to "Mystery", "category" to null),
        )

        val selection = data.select(Column<String?>("category"), Column<String>("name"))

        selection.collect() shouldContainExactlyInAnyOrder listOf(
            DataRow("category" to "stuff", "name" to "Something"),
            DataRow("category" to "stuff", "name" to "Something else"),
            DataRow("category" to null, "name" to "Mystery"),
        )
    }

    @Test
    fun `Engine can select columns of Data`() {
        val data = ConstantData(
            DataRow("id" to 1, "name" to "Something", "category" to "stuff"),
            DataRow("id" to 2, "name" to "Something else", "category" to "stuff"),
            DataRow("id" to 3, "name" to "Mystery", "category" to null),
        )

        val selection = data.select(data.get<String?>("category"), data.get<String>("name"))

        selection.collect() shouldContainExactlyInAnyOrder listOf(
            DataRow("category" to "stuff", "name" to "Something"),
            DataRow("category" to "stuff", "name" to "Something else"),
            DataRow("category" to null, "name" to "Mystery"),
        )
    }

    @Test
    fun `Engine can select columns by properties of data-class instances`() {
        data class Thing(val id: Int, val name: String, val category: String?)
        val data = ConstantData(
            Thing(1, "Something", "stuff"),
            Thing(2, "Mystery", null),
        )

        val selection = data.select(data[Thing::name], data[Thing::category])

        selection.collect() shouldContainExactlyInAnyOrder listOf(
            DataRow("name" to "Something", "category" to "stuff"),
            DataRow("name" to "Mystery", "category" to null),
        )
    }

    @Test
    fun `Engine can select renamed columns of Data`() {
        val data = ConstantData(
            DataRow("id" to 1),
            DataRow("id" to 2),
        )

        val selection = data.select(Column<Int>("id") named "key")

        selection.collect() shouldContainExactlyInAnyOrder listOf(
            DataRow("key" to 1),
            DataRow("key" to 2),
        )
    }
}
