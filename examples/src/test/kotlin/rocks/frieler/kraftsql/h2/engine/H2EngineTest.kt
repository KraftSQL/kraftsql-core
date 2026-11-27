package rocks.frieler.kraftsql.h2.engine

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
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
import rocks.frieler.kraftsql.data.Table
import rocks.frieler.kraftsql.data.Type
import rocks.frieler.kraftsql.engine.DataClassSerDe
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.SQLNonTransientException
import java.sql.Statement

class H2EngineTest {
    private val connection = mockk<Connection>()
    private val sqlGenerator = mockk<H2SqlGenerator>()
    private val engine = H2Engine(
        connection = connection,
        sqlGenerator = sqlGenerator,
    )

    @Test
    fun `collect() can collect ConstantData of DataRows as DataRows`() {
        val items = listOf(DataRow("id" to 1), DataRow("id" to 2))
        val data = mockk<ConstantData<DataRow>> {
            every { this@mockk.items } returns items
            every { schema } returns listOf(Column("id", Type.INTEGER))
        }

        val query = "<SQL statement for $data>"
        every { sqlGenerator.makeSQL(data) } returns query
        mockQueryResult(query, data)

        val collectedData = engine.collect(data, DataRow::class)

        collectedData shouldContainExactly items
    }

    @Test
    fun `collect() can collect ConstantData of DataRows as data-class instances`() {
        data class Answer(val answer: Int)
        val item = DataRow("answer" to 42)
        @Suppress("UNCHECKED_CAST") val data = mockk<ConstantData<DataRow>> {
            every { this@mockk.items } returns listOf(item)
            every { schema } returns listOf(Column("answer", Type.INTEGER))
        } as Data<Answer>

        val query = "<SQL statement for $data>"
        every { sqlGenerator.makeSQL(data) } returns query
        mockQueryResult(query, data as ConstantData<Answer>)

        val collectedData = engine.collect(data, Answer::class)

        collectedData shouldContainExactly listOf(Answer(42))
    }

    @Test
    fun `collect() can collect ConstantData of data-class instances as DataRows`() {
        data class Answer(val answer: Int)
        val item = Answer(42)
        @Suppress("UNCHECKED_CAST") val data = mockk<ConstantData<Answer>> {
            every { this@mockk.items } returns listOf(item)
            every { schema } returns listOf(Column("answer", Type.INTEGER))
        } as Data<DataRow>

        val query = "<SQL statement for $data>"
        every { sqlGenerator.makeSQL(data) } returns query
        mockQueryResult(query, data as ConstantData<DataRow>)

        val collectedData = engine.collect(data, DataRow::class)

        collectedData shouldContainExactly listOf(DataRow("answer" to 42))
    }

    @Test
    fun `collect() can collect values of all supported types`() {
        val items = listOf(
            DataRow("int" to 1, "string" to "KraftSQL", "boolean" to true),
            DataRow("int" to 2, "string" to "SQL", "boolean" to false),
        )
        val data = mockk<ConstantData<DataRow>> {
            every { this@mockk.items } returns items
            every { schema } returns listOf(
                Column("int", Type.INTEGER),
                Column("string", Type.STRING),
                Column("boolean", Type.BOOLEAN),
            )
        }

        val query = "<SQL statement for $data>"
        every { sqlGenerator.makeSQL(data) } returns query
        mockQueryResult(query, data)

        val collectedData = engine.collect(data, DataRow::class)

        collectedData shouldContainExactly items
    }

    @Test
    fun `collect() can collect nulls and values from nullable columns`() {
        val items = listOf(
            DataRow("nullable_int" to 1, "nullable_string" to "KraftSQL"),
            DataRow("nullable_int" to null, "nullable_string" to null),
        )
        val data = mockk<ConstantData<DataRow>> {
            every { this@mockk.items } returns items
            every { schema } returns listOf(
                Column("nullable_int", Type.NULLABLE_INTEGER),
                Column("nullable_string", Type.NULLABLE_STRING),
            )
        }

        val query = "<SQL statement for $data>"
        every { sqlGenerator.makeSQL(data) } returns query
        mockQueryResult(query, data)

        val collectedData = engine.collect(data, DataRow::class)

        collectedData shouldContainExactly items
    }

    @Test
    fun `collect() can collect special Strings`() {
        val items = listOf(DataRow(
            "string_with_quotes" to "it's \"quoted\"",
            "empty_string" to "",
        ))
        val data = mockk<ConstantData<DataRow>> {
            every { this@mockk.items } returns items
            every { schema } returns listOf(
                Column("string_with_quotes", Type.STRING),
                Column("empty_string", Type.STRING),
            )
        }

        val query = "<SQL statement for $data>"
        every { sqlGenerator.makeSQL(data) } returns query
        mockQueryResult(query, data)

        val collectedData = engine.collect(data, DataRow::class)

        collectedData shouldContainExactly items
    }

    @Test
    fun `collect() rethrows SqlNonTransientException as IllegalStateException`() {
        val table = mockk<Table<DataRow>>()
        val query = "<SELECT $table>"
        every { sqlGenerator.makeSQL(table) } returns query

        val sqlNonTransientException = SQLNonTransientException("error")
        every { connection.createStatement() } returns mockk {
            every { executeQuery(query) } throws sqlNonTransientException
            every { close() } just runs
        }

        shouldThrow<IllegalStateException> {
            engine.collect(table, DataRow::class)
        }.also {
            it.cause shouldBe sqlNonTransientException
        }
    }

    @Test
    fun `createTable() executes the CREATE TABLE statement and closes the Statement`() {
        val table = mockk<Table<*>>()
        val createStatement = "<CREATE TABLE statement for $table>"
        every { sqlGenerator.makeCreateTableStatement(table) } returns createStatement
        val statement = mockStatement()

        engine.createTable(table)

        verify { statement.execute(createStatement) }
        verify { statement.close() }
    }

    @Test
    fun `createTable() propagates the SQLException if the table already exists`() {
        val table = mockk<Table<*>>()
        val createStatement = "<CREATE TABLE statement for $table>"
        every { sqlGenerator.makeCreateTableStatement(table) } returns createStatement
        val exception = SQLException("Table already exists")
        val statement = mockStatement { every { execute(createStatement) } throws exception }

        shouldThrow<SQLException> {
            engine.createTable(table)
        } shouldBe exception
        verify { statement.close() }
    }

    @Test
    fun `dropTable() executes the DROP TABLE statement and closes the Statement`() {
        val table = mockk<Table<*>>()
        val dropStatement = "<DROP TABLE statement for $table>"
        every { sqlGenerator.makeDropTableStatement(table, ifExists = false) } returns dropStatement
        val statement = mockStatement()

        engine.dropTable(table, ifExists = false)

        verify { statement.execute(dropStatement) }
        verify { statement.close() }
    }

    @Test
    fun `dropTable() executes the DROP TABLE statement for ifExists if requested`() {
        val table = mockk<Table<*>>()
        val dropStatement = "<DROP TABLE IF EXISTS statement for $table>"
        every { sqlGenerator.makeDropTableStatement(table, ifExists = true) } returns dropStatement
        val statement = mockStatement()

        engine.dropTable(table, ifExists = true)

        verify { statement.execute(dropStatement) }
    }

    @Test
    fun `dropTable() propagates the SQLException if the table does not exist`() {
        val table = mockk<Table<*>>()
        val dropStatement = "<DROP TABLE statement for $table>"
        every { sqlGenerator.makeDropTableStatement(table, ifExists = false) } returns dropStatement
        val exception = SQLException("Table not found")
        val statement = mockStatement { every { execute(dropStatement) } throws exception }

        shouldThrow<SQLException> {
            engine.dropTable(table, ifExists = false)
        } shouldBe exception
        verify { statement.close() }
    }

    @Test
    fun `insertInto() executes INSERT INTO statement and return number of affected rows`() {
        val table = mockk<Table<DataRow>>()
        val data = mockk<Data<DataRow>>()
        val insertIntoStatement = "<INSERT INTO $table <$data>>"
        every { sqlGenerator.makeInsertIntoStatement(table, data) } returns insertIntoStatement
        val statement = mockStatement { every { executeUpdate(insertIntoStatement) } returns 14 }

        val insertedRows = engine.insertInto(table, data)
        verify { statement.executeUpdate(insertIntoStatement) }
        insertedRows shouldBe 14
    }

    private fun mockStatement(block: Statement.() -> Unit = {}): Statement {
        val statement = mockk<Statement> {
            every { execute(any<String>()) } returns false
            every { close() } just runs
            block()
        }
        every { connection.createStatement() } returns statement
        return statement
    }

    private fun mockQueryResult(query: String, data: ConstantData<*>) {
        val rows = data.items.map { item -> item as? DataRow ?: DataClassSerDe.serialize(item) }
        var cursor = -1
        val resultSet = mockk<ResultSet> {
            every { next() } answers { ++cursor < rows.size }
            every { getObject(any<String>(), any<Class<Any>>()) } answers { rows[cursor][firstArg()] }
            every { getString(any<String>()) } answers { rows[cursor][firstArg()] as String? }
            every { close() } just runs
        }
        every { connection.createStatement() } returns mockk {
            every { executeQuery(query) } returns resultSet
            every { close() } just runs
        }
    }
}
