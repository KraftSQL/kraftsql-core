package rocks.frieler.kraftsql.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.engine.DataClassSerDe

class TableTest {
    @Test
    fun `Table has the given coordinates and schema`() {
        val schema = listOf(
            Column("id", Type.INTEGER),
            Column("value", Type.NULLABLE_STRING),
        )

        val table = Table<DataRow>("db", "ns", "table", schema)

        table.database shouldBe "db"
        table.namespace shouldBe "ns"
        table.name shouldBe "table"
        table.schema shouldBe schema
    }

    @Test
    fun `Table rejects a duplicate column name`() {
        shouldThrow<IllegalArgumentException> {
            Table<DataRow>("db", "ns", "table", listOf(
                Column("value", Type.INTEGER),
                Column("value", Type.NULLABLE_STRING),
            ))
        }
    }

    @Test
    fun `toString() builds full qualified table name`() {
        val table = Table<DataRow>("db", "ns", "table", listOf())

        table.toString() shouldBe "db.ns.table"
    }

    @Test
    fun `toString() hints to default db if database is not set`() {
        val table = Table<DataRow>("ns", "table", listOf())

        table.toString() shouldBe "<default db>.ns.table"
    }

    @Test
    fun `Table() derives the schema from the data class`() {
        data class Thing(val id: Int, val value: String?)

        val table = Table<Thing>("db", "ns", "things")

        table.schema shouldBe DataClassSerDe.getSchemaFor(Thing::class)
    }

    @Test
    fun `Table() without database creates a Table in the Engine's default database`() {
        data class Thing(val id: Int, val value: String?)

        val table = Table<Thing>("ns", "things")

        table.database shouldBe null
    }
}
