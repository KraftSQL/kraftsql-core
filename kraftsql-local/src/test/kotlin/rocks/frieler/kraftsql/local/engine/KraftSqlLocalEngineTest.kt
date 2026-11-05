package rocks.frieler.kraftsql.local.engine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.Column
import rocks.frieler.kraftsql.data.ConstantData
import rocks.frieler.kraftsql.data.Data
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.Selection
import rocks.frieler.kraftsql.data.Table
import rocks.frieler.kraftsql.data.Type
import rocks.frieler.kraftsql.data.named
import rocks.frieler.kraftsql.expressions.Constant
import rocks.frieler.kraftsql.expressions.get

class KraftSqlLocalEngineTest {
    private val engineState = mockk<EngineState>()
    private val engine = KraftSqlLocalEngine(engineState)

    @Test
    fun `collect() can collect ConstantData of DataRows as DataRows`() {
        val items = listOf(mockk<DataRow>(), mockk<DataRow>())
        val data = mockk<ConstantData<DataRow>> { every { this@mockk.items } returns items }

        val collectedData = engine.collect(data, DataRow::class)

        collectedData shouldContainExactly items
    }

    @Test
    fun `collect() can collect ConstantData of data-class instances as DataRows`() {
        data class Answer(val answer: Int)
        val item = Answer(42)
        @Suppress("UNCHECKED_CAST") val data = mockk<ConstantData<Answer>> { every { this@mockk.items } returns listOf(item) } as Data<DataRow>

        val collectedData = engine.collect(data, DataRow::class)

        collectedData shouldContainExactly listOf(DataRow("answer" to 42))
    }

    @Test
    fun `collect() can collect the rows of a Table`() {
        val table = Table<DataRow>("db", "ns", "things", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        every { engineState.findTable(table.fqdn) } returns table
        val data = listOf(DataRow("id" to 1, "value" to "foo"))
        every { engineState.readTable(table) } returns data

        val collectedRows = engine.collect(table, DataRow::class)

        collectedRows shouldContainExactlyInAnyOrder data
    }

    @Test
    fun `collect() fails to collect a Table that does not exist`() {
        val table = Table<DataRow>("db", "ns", "things", emptyList())
        every { engineState.findTable(table.fqdn) } returns null

        shouldThrow<IllegalStateException> {
            engine.collect(table, DataRow::class)
        }
    }

    @Test
    fun `collect() collects only the columns of a specified Table to collect with an incomplete schema`() {
        val actualTable = Table<DataRow>("db", "ns", "things", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        val actualData = listOf(DataRow("id" to 1, "value" to "foo"))
        every { engineState.readTable(actualTable) } returns actualData
        val tableToCollect = Table<DataRow>("db", "ns", "things", listOf(
            Column("id", Type.INTEGER),
        ))
        every { engineState.findTable(tableToCollect.fqdn) } returns actualTable

        val collectedRows = engine.collect(tableToCollect, DataRow::class)

        collectedRows shouldContainExactlyInAnyOrder listOf(DataRow("id" to 1))
    }

    @Test
    fun `collect() fails to collect a table when a column does not exist in the actual table`() {
        val actualTable = Table<DataRow>("db", "ns", "things", listOf(
            Column("id", Type.INTEGER),
        ))
        val tableToCollect = Table<DataRow>("db", "ns", "things", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        every { engineState.findTable(tableToCollect.fqdn) } returns actualTable

        shouldThrow<IllegalStateException> {
            engine.collect(tableToCollect, DataRow::class)
        }
    }

    @Test
    fun `collect() fails to collect a table when a column does not have the same type in the actual table apart from non-nullability`() {
        val actualTable = Table<DataRow>("db", "ns", "things", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_INTEGER),
        ))
        val tableToCollect = Table<DataRow>("db", "ns", "things", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        every { engineState.findTable(tableToCollect.fqdn) } returns actualTable

        shouldThrow<IllegalStateException> {
            engine.collect(tableToCollect, DataRow::class)
        }
    }

    @Test
    fun `collect() can collect a table with a nullable-column when the column is non-nullable in the actual table`() {
        val actualTable = Table<DataRow>("db", "ns", "things", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.STRING),
        ))
        val actualData = listOf(DataRow("id" to 1, "value" to "foo"))
        every { engineState.readTable(actualTable) } returns actualData
        val tableToCollect = Table<DataRow>("db", "ns", "things", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        every { engineState.findTable(tableToCollect.fqdn) } returns actualTable

        val collectedRows = engine.collect(tableToCollect, DataRow::class)

        collectedRows shouldContainExactlyInAnyOrder actualData
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
    fun `collect() can collect a Selection of renamed columns`() {
        val source = ConstantData(
            DataRow("id" to 1),
            DataRow("id" to 2),
        )
        val selection = Selection(source, listOf(source.get<Int>("id") named "key"))

        val collectedData = engine.collect(selection, DataRow::class)

        collectedData shouldContainExactlyInAnyOrder listOf(
            DataRow("key" to 1),
            DataRow("key" to 2),
        )
    }

    @Test
    fun `KraftSQLLocalEngine can evaluate Constant Expressions`() {
        val constant = Constant(42)
        val selectionOfConstant = Selection(
            ConstantData(DataRow("id" to 1)),
            listOf(constant named "answer"),
        )

        val result = engine.collect(selectionOfConstant, DataRow::class)

        result.single()["answer"] shouldBe 42
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

    @Test
    fun `createTable() creates a table`() {
        val table = Table<DataRow>("db", "ns", "table", emptyList())
        every { engineState.findTable(table.fqdn) } returns null
        every { engineState.putTable(table) } just runs

        engine.createTable(table)

        verify { engineState.putTable(table) }
    }

    @Test
    fun `createTable() fails to create a table that already exists`() {
        val table = Table<DataRow>("db", "ns", "table", emptyList())
        every { engineState.findTable(table.fqdn) } returns table

        shouldThrow<IllegalStateException> {
            engine.createTable(table)
        }
    }

    @Test
    fun `createTable() distinguishes the default database from named databases`() {
        val tableInDefaultDb = Table<DataRow>(namespace = "ns", name = "table", schema = emptyList())
        val tableInNamedDb = Table<DataRow>("db", "ns", "table", emptyList())
        every { engineState.findTable(tableInDefaultDb.fqdn) } returns null
        every { engineState.findTable(tableInNamedDb.fqdn) } returns null
        every { engineState.putTable(any()) } just runs

        engine.createTable(tableInDefaultDb)
        engine.createTable(tableInNamedDb)

        verify { engineState.putTable(tableInDefaultDb) }
        verify { engineState.putTable(tableInNamedDb) }
    }

    @Test
    fun `dropTable() removes the table`() {
        val table = Table<DataRow>("db", "ns", "table", emptyList())
        every { engineState.findTable(table.fqdn) } returns table
        every { engineState.removeTable(table) } just runs

        engine.dropTable(table)

        verify { engineState.removeTable(table) }
    }

    @Test
    fun `dropTable() fails to drop a table that does not exist`() {
        val table = Table<DataRow>("db", "ns", "table", emptyList())
        every { engineState.findTable(table.fqdn) } returns null

        shouldThrow<IllegalStateException> {
            engine.dropTable(table)
        }
    }

    @Test
    fun `dropTable() ignores a table that does not exist if requested`() {
        val table = Table<DataRow>("db", "ns", "table", emptyList())
        every { engineState.findTable(table.fqdn) } returns null
        every { engineState.removeTable(table) } just runs

        engine.dropTable(table, ifExists = true)
    }

    @Test
    fun `insertInto() appends the rows of the Data to the table`() {
        val table = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        every { engineState.findTable(table.fqdn) } returns table
        every { engineState.appendToTable(table, any()) } just runs
        val data = ConstantData(
            DataRow("id" to 1, "value" to "foo"),
            DataRow("id" to 2, "value" to "bar"),
        )

        val insertedRows = engine.insertInto(table, data)

        verify { engineState.appendToTable(table, data.items.toList()) }
        insertedRows shouldBe data.items.count()
    }

    @Test
    fun `insertInto() matches the columns of the Data by name`() {
        val table = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        every { engineState.findTable(table.fqdn) } returns table
        every { engineState.appendToTable(table, any()) } just runs
        val data = ConstantData(
            DataRow("value" to "foo", "id" to 1),
        )

        engine.insertInto(table, data)

        verify { engineState.appendToTable(table, listOf(DataRow("id" to 1, "value" to "foo"))) }
    }

    @Test
    fun `insertInto() fills columns missing in the Data with NULL`() {
        val table = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        every { engineState.findTable(table.fqdn) } returns table
        every { engineState.appendToTable(table, any()) } just runs
        val data = ConstantData(
            DataRow("id" to 1),
        )

        engine.insertInto(table, data)

        verify { engineState.appendToTable(table, listOf(DataRow("id" to 1, "value" to null))) }
    }

    @Test
    fun `insertInto() fails to fill columns missing in the Data with NULL when the column is non-nullable`() {
        val table = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.STRING),
        ))
        every { engineState.findTable(table.fqdn) } returns table
        every { engineState.appendToTable(table, any()) } throws IllegalArgumentException("incompatible type")
        val data = ConstantData(
            DataRow("id" to 1),
        )

        shouldThrow<IllegalArgumentException> {
            engine.insertInto(table, data)
        }
    }

    @Test
    fun `insertInto() fails to insert into a table that does not exist`() {
        val table = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        every { engineState.findTable(table.fqdn) } returns null

        shouldThrow<IllegalStateException> {
            engine.insertInto(table, mockk())
        }
    }

    @Test
    fun `insertInto() fails to insert into a column that does not exist in the actual target table`() {
        val actualTable = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
        ))
        val targetTable = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        every { engineState.findTable(targetTable.fqdn) } returns actualTable
        val data = ConstantData(
            DataRow("id" to 1, "value" to "foo"),
        )

        shouldThrow<IllegalArgumentException> {
            engine.insertInto(targetTable, data)
        }
    }

    @Test
    fun `insertInto() fails if a column of the actual target table has another type than in the given target table apart from additional nullability`() {
        val actualTable = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.STRING),
        ))
        val targetTable = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.INTEGER),
        ))
        every { engineState.findTable(targetTable.fqdn) } returns actualTable
        every { engineState.appendToTable(actualTable, any()) } just runs
        val data = ConstantData(
            DataRow("id" to 1, "value" to 42),
        )

        shouldThrow<IllegalArgumentException> {
            engine.insertInto(targetTable, data)
        }
    }

    @Test
    fun `insertInto() can insert into a nullable column of the actual target table although it is non-nullable in the target table`() {
        val actualTable = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        val targetTable = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.STRING),
        ))
        every { engineState.findTable(targetTable.fqdn) } returns actualTable
        every { engineState.appendToTable(actualTable, any()) } just runs
        val data = ConstantData(
            DataRow("id" to 1, "value" to "foo"),
        )
        every { engineState.appendToTable(actualTable, any()) } just runs

        engine.insertInto(targetTable, data)

        verify { engineState.appendToTable(actualTable, listOf(DataRow("id" to 1, "value" to "foo"))) }
    }

    @Test
    fun `insertInto() fails to insert NULL into a column that is nullable in the target table's schema, but non-nullable in the actual target table`() {
        val actualTable = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.STRING),
        ))
        val targetTable = Table<DataRow>("db", "ns", "table", listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        ))
        every { engineState.findTable(targetTable.fqdn) } returns actualTable
        every { engineState.appendToTable(actualTable, any()) } just runs
        val data = ConstantData(
            DataRow("id" to 1, "value" to "foo"),
            DataRow("id" to 2, "value" to null),
        )

        shouldThrow<IllegalArgumentException> {
            engine.insertInto(targetTable, data)
        }
    }
}
