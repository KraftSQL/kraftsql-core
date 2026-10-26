package rocks.frieler.kraftsql.expressions

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.Data
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.NamedExpression
import rocks.frieler.kraftsql.data.Type
import rocks.frieler.kraftsql.data.Column as ColumnDefinition

class ColumnTest {
    @Test
    fun `Column can be created from name and inlined type`() {
        val intColumn = Column<Int>("integer")

        intColumn.name shouldBe "integer"
        intColumn.type shouldBe Type.INTEGER
    }

    @Test
    fun `Column can be created from name and inlined nullable type`() {
        val intColumn = Column<Int?>("integer")

        intColumn.name shouldBe "integer"
        intColumn.type shouldBe Type.NULLABLE_INTEGER
    }

    @Test
    fun `Column rejects an unsupported Kotlin type`() {
        shouldThrow<IllegalStateException> {
            Column<Void>("integer")
        }
    }

    @Test
    fun `Column is a NamedExpression of itself, named by its name`() {
        val column = Column<String>("text") as NamedExpression<String>

        column.name shouldBe "text"
        column.expression shouldBe column
    }

    @Test
    fun `Column's toString() returns its name`() {
        val column = Column<Int>("the_column")

        column.toString() shouldBe column.name
    }

    @Test
    fun `Column equals another Column with the same name and type`() {
        Column<Int>("integer") shouldBe Column<Int>("integer")
    }

    @Test
    fun `Column does not equal a Column with a different name`() {
        Column<Int>("integer") shouldNotBe Column<Int>("number")
    }

    @Test
    fun `Column does not equal a Column with the same name but a different type`() {
        Column<Int>("number") shouldNotBe Column<Int?>("number")
    }

    @Test
    fun `Column does not equal something that is not a Column`() {
        Column<Int>("number") shouldNotBe Any()
    }

    @Test
    fun `Equal Columns have the same hashCode`() {
        Column<Int>("integer").hashCode() shouldBe Column<Int>("integer").hashCode()
    }

    interface WithId { val id: Int }
    private val data = mockk<Data<DataRow>> {
        every { schema } returns listOf(ColumnDefinition("id", Type.INTEGER))
    }

    @Test
    fun `Data's get operator returns Column expression with name and type of a column in the schema`() {
        val column: Column<Int> = data["id"]

        column.name shouldBe "id"
        column.type shouldBe Type.INTEGER
    }

    @Test
    fun `get operator rejects to provide Column expression for name of a column not in the schema`() {
        shouldThrow<IllegalArgumentException> {
            data.get<String?>("unknown")
        }.apply {
            message shouldContain data.schema.map { it.name }.toString()
        }
    }

    @Test
    fun `get operator rejects to provide Column expression for name of a column of another type than specified`() {
        shouldThrow<IllegalArgumentException> {
            data.get<String?>("id")
        }
    }

    @Test
    fun `get operator returns Column expression with name and type of the column for property of the rows' type`() {
        @Suppress("UNCHECKED_CAST") val typedData = data as Data<WithId>

        val column = typedData[WithId::id]

        column shouldBe typedData[WithId::id.name]
    }

    @Test
    fun `get operator rejects to provide Column expression for property that is not a column in the schema`() {
        data class Foo(val id: String, val bar: String)
        @Suppress("UNCHECKED_CAST") val wronglyTypedData = data as Data<Foo>

        shouldThrow<IllegalArgumentException> {
            wronglyTypedData[Foo::id]
        }

        shouldThrow<IllegalArgumentException> {
            wronglyTypedData[Foo::bar]
        }
    }
}
