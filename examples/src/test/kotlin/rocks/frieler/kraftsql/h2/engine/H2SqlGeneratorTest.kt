package rocks.frieler.kraftsql.h2.engine

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.ConstantData
import rocks.frieler.kraftsql.data.Data
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.Selection
import rocks.frieler.kraftsql.data.named
import rocks.frieler.kraftsql.expressions.get

class H2SqlGeneratorTest {
    @Test
    fun `makeSQL selects ConstantData of DataRows as union of casted constants`() {
        val data = ConstantData(
            DataRow("number" to 1, "name" to "one"),
            DataRow("number" to 2, "name" to "two"),
        )

        H2SqlGenerator.makeSQL(data).normalized() shouldBe
            "SELECT CAST(1 AS INTEGER) AS `number`,CAST('one' AS VARCHAR) AS `name` " +
            "UNION ALL " +
            "SELECT CAST(2 AS INTEGER) AS `number`,CAST('two' AS VARCHAR) AS `name`"
    }

    @Test
    fun `makeSQL casts null values of ConstantData to their column's type`() {
        val data = ConstantData(
            DataRow("number" to 1, "name" to null),
            DataRow("number" to null, "name" to "two"),
        )

        H2SqlGenerator.makeSQL(data).normalized() shouldBe
            "SELECT CAST(1 AS INTEGER) AS `number`,CAST(NULL AS VARCHAR) AS `name` " +
            "UNION ALL " +
            "SELECT CAST(NULL AS INTEGER) AS `number`,CAST('two' AS VARCHAR) AS `name`"
    }

    @Test
    fun `constant() renders null as NULL`() {
        H2SqlGenerator.constant(null) shouldBe "NULL"
    }

    @Test
    fun `constant() renders an Int as integer literal`() {
        H2SqlGenerator.constant(42) shouldBe "42"
    }

    @Test
    fun `constant() renders a String as single-quoted string literal`() {
        H2SqlGenerator.constant("text") shouldBe "'text'"
    }

    @Test
    fun `constant() escapes single quotes in a String by doubling them`() {
        val stringWithSpecialCharacters = """
            it's a \ test
            with a second line
        """.trimIndent()

        H2SqlGenerator.constant(stringWithSpecialCharacters) shouldBe "'it''s a \\ test\nwith a second line'"
    }

    @Test
    fun `makeSQL selects the columns of a Selection from its source data as subquery`() {
        val data = ConstantData(
            DataRow("number" to 1, "name" to "one"),
            DataRow("number" to 2, "name" to "two"),
        )
        val selection = Selection(data, listOf(data.get<String>("name")))

        H2SqlGenerator.makeSQL(selection).normalized() shouldBe
            "SELECT `name` AS `name` " +
            "FROM (" +
                "SELECT CAST(1 AS INTEGER) AS `number`,CAST('one' AS VARCHAR) AS `name` " +
                "UNION ALL " +
                "SELECT CAST(2 AS INTEGER) AS `number`,CAST('two' AS VARCHAR) AS `name`" +
            ") AS `source`"
    }

    @Test
    fun `makeSQL selects an expression of a Selection aliased by its given name`() {
        val source = ConstantData(DataRow("foo" to "bar"))
        val selection = Selection(source, listOf(source.get<String>("foo") named "fu"))

        H2SqlGenerator.makeSQL(selection).normalized() shouldBe
            "SELECT `foo` AS `fu` " +
            "FROM (${H2SqlGenerator.makeSQL(source).normalized()}) AS `source`"
    }

    // ATTENTION: Does not respect String literal, but changes them as well!
    private fun String.normalized() = trim()
        .replace(Regex("""([(,\[])\s*"""), "$1")
        .replace(Regex("""\s*([),\]])"""), "$1")
        .replace(Regex("""\s+"""), " ")
}
