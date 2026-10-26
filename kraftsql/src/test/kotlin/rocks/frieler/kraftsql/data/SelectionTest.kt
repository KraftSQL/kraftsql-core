package rocks.frieler.kraftsql.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.expressions.get
import rocks.frieler.kraftsql.expressions.Column as ColumnExpression

class SelectionTest {
    private val source = mockk<Data<*>> {
        every { schema } returns listOf(Column("id", Type.INTEGER), Column("name", Type.NULLABLE_STRING))
    }
    private val id = ColumnExpression<Int>("id")
    private val name = ColumnExpression<String?>("name")

    @Test
    fun `Selection can select columns of the source`() {
        val selection = Selection(source, listOf(id, name))

        selection.source shouldBe source
        selection.expressions shouldBe listOf(id, name)
    }

    @Test
    fun `Selection rejects an empty list of columns`() {
        shouldThrow<IllegalArgumentException> {
            Selection(source, emptyList())
        }
    }

    @Test
    fun `Selection rejects a column selected more than once`() {
        shouldThrow<IllegalArgumentException> {
            Selection(source, listOf(id, name, ColumnExpression<Int>("id")))
        }
    }

    @Test
    fun `Selection rejects a column whose name is not in the schema of the source`() {
        shouldThrow<IllegalArgumentException> {
            Selection(source, listOf(ColumnExpression<String>("unknown")))
        }.apply {
            message shouldContain source.schema.map { column -> column.name }.toString()
        }
    }

    @Test
    fun `Selection rejects a column whose type differs from the column of the same name in the schema of the source`() {
        shouldThrow<IllegalArgumentException> {
            Selection(source, listOf(ColumnExpression<String>("id")))
        }
    }

    @Test
    fun `Selection can select a column retrieved from other Data, if the source has a column of the same name and type`() {
        val otherData = mockk<Data<DataRow>> {
            every { schema } returns listOf(Column("id", Type.INTEGER))
        }
        val foreignIdColumn: ColumnExpression<Int> = otherData["id"]

        val selection = Selection(source, listOf(foreignIdColumn))

        selection.source shouldBe source
        selection.expressions shouldBe listOf(foreignIdColumn)
    }

    @Test
    fun `Selection has the names and types of the selected columns as schema in the order of the selection`() {
        val selection = Selection(source, listOf(name, id))

        selection.schema shouldBe listOf(Column("name", Type.NULLABLE_STRING), Column("id", Type.INTEGER))
    }

    @Test
    fun `select() creates a Selection of the given columns from the Data`() {
        val selection = source.select(listOf(id, name))

        selection.shouldBeInstanceOf<Selection>()
        selection.source shouldBe source
        selection.expressions shouldBe listOf(id, name)
    }

    @Test
    fun `select() also accepts the columns as vararg`() {
        val selection = source.select(id, name)

        selection.shouldBeInstanceOf<Selection>()
        selection.source shouldBe source
        selection.expressions shouldBe listOf(id, name)
    }
}
