package rocks.frieler.kraftsql.h2.engine

import rocks.frieler.kraftsql.data.ConstantData
import rocks.frieler.kraftsql.data.Data
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.Selection
import rocks.frieler.kraftsql.engine.DataClassSerDe
import rocks.frieler.kraftsql.expressions.Column

/**
 * Utility object that generates H2 SQL for KraftSQL [rocks.frieler.kraftsql.data.Data].
 */
object H2SqlGenerator {
    /**
     * Generates a `SELECT` statement to select the given [Data] in H2.
     *
     * @param data the [Data] to select
     * @return a `SELECT` statement to select the `data`
     */
    fun makeSQL(data: Data<*>): String = when (data) {
        is ConstantData<*> -> makeSQL(data)
        is Selection -> makeSQL(data)
        else -> throw NotImplementedError("Collecting $data is not implemented.")
    }

    private fun makeSQL(data: ConstantData<*>): String {
        val rows = data.items.map { it as? DataRow ?: DataClassSerDe.serialize(it) }

        return rows.joinToString(separator = " UNION ALL ") { row ->
            data.schema.joinToString(separator = ",", prefix = "SELECT ") { column ->
                "CAST(${constant(row[column.name])} AS ${column.type.native.name}) AS `${column.name}`"
            }
        }
    }

    private fun makeSQL(data: Selection): String = """
        SELECT ${data.expressions.joinToString(separator = ",") { "${column(it.expression as Column<*>)} AS `${it.name}`" }}
        FROM (${makeSQL(data.source)}) AS `source`
    """.trimIndent() // TODO: Merge multiple Selections.

    internal fun constant(value: Any?) = when (value) {
        null -> "NULL"
        is String -> "'${value.replace("'", "''")}'"
        else -> value.toString()
    }

    internal fun column(column: Column<*>) = "`${column.name}`"
}
