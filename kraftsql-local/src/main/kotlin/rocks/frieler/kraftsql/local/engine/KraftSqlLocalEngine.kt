package rocks.frieler.kraftsql.local.engine

import rocks.frieler.kraftsql.data.ConstantData
import rocks.frieler.kraftsql.data.Data
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.Selection
import rocks.frieler.kraftsql.data.Table
import rocks.frieler.kraftsql.data.Type.Companion.makeNullable
import rocks.frieler.kraftsql.engine.DataClassSerDe
import rocks.frieler.kraftsql.engine.Engine
import rocks.frieler.kraftsql.expressions.Column
import rocks.frieler.kraftsql.expressions.Constant
import rocks.frieler.kraftsql.expressions.Expression
import kotlin.reflect.KClass

/**
 * [Engine] implementation that works locally in the JVM.
 *
 * The [KraftSqlLocalEngine] is primarily meant as a dummy [Engine] for (unit) testing business logic, when the real
 * [Engine] is not available or too expensive to use. It is not optimized and not intended to handle "Big Data".
 *
 * [Table]s are kept in an in-memory state of the [KraftSqlLocalEngine] instance, starting empty: they exist only as
 * long as the instance, and are not shared with other instances. Any [database][Table.database] and
 * [namespace][Table.namespace] can be used without creating it before. The default database `null` is distinct from all
 * named databases. All names are case-sensitive.
 *
 * Limitation: The [KraftSqlLocalEngine] is not thread-safe. An instance must not be used by multiple threads
 * concurrently, e.g. by tests running in parallel.
 */
class KraftSqlLocalEngine(
    private val state: EngineState = EngineState(),
) : Engine {

    override fun <T : Any> collect(data: Data<T>, type: KClass<T>): List<T> {
        return rows(data)
            .map { @Suppress("UNCHECKED_CAST") if (type != DataRow::class) DataClassSerDe.deserialize(it, type) else it as T }
    }

    private fun rows(data: Data<*>): List<DataRow> {
        return when (data) {
            is ConstantData<*> -> data.items
                .map { if (it !is DataRow) DataClassSerDe.serialize(it) else it }
            is Table<*> -> {
                val actualTable = state.findTable(data.fqdn) ?: throw IllegalStateException("Table '${data.fqdn}' not found.")
                data.schema.forEach { columnToRead ->
                    val columnToReadFrom = actualTable.schema.find { it.name == columnToRead.name } ?: throw IllegalStateException("Column '$columnToRead.name' does not exist in table '${data.fqdn}'.")
                    check(columnToRead.type in setOf(columnToReadFrom.type, columnToReadFrom.type.makeNullable())) { "Column '${columnToReadFrom.name}' is of type '${columnToReadFrom.type}' but expected '${columnToRead.type}'." }
                }
                state.readTable(actualTable)
                    .map { row -> DataRow(data.schema.map { column -> column.name to row[column.name] }) }
            }
            is Selection -> rows(data.source)
                .map { row -> DataRow(data.expressions.map { column -> column.name to evaluateExpression(column.expression, row) }) }
            else -> throw NotImplementedError("Collecting $data is not implemented.")
        }
    }

    private fun evaluateExpression(expression: Expression<*>, row: DataRow) : Any? = when (expression) {
        is Constant<*> -> expression.value
        is Column<*> -> row[expression.name]
        else -> throw NotImplementedError("Evaluating $expression is not supported.")
    }

    /**
     * Creates the [Table] in the in-memory state.
     *
     * @throws IllegalStateException if a table with the same coordinates already exists
     */
    override fun createTable(table: Table<*>) {
        check(state.findTable(table.fqdn) == null) { "Table '${table.fqdn}' already exists." }
        state.putTable(table)
    }

    /**
     * Drops the [Table] from the in-memory state.
     *
     * @throws IllegalStateException if the table does not exist and [ifExists] is not set
     */
    override fun dropTable(table: Table<*>, ifExists: Boolean) {
        check(ifExists || state.findTable(table.fqdn) != null) { "Table '${table.fqdn}' does not exist." }
        state.removeTable(table)
    }

    /**
     * Inserts the rows into the [Table] in the in-memory state.
     *
     * @param table the [Table] to insert into
     * @param data the [Data] to insert
     * @return the number of inserted rows
     */
    override fun <T : Any> insertInto(table: Table<T>, data: Data<T>): Long {
        val actualTable = state.findTable(table.fqdn) ?: throw IllegalStateException("Table '${table.fqdn}' not found.")
        table.schema.forEach { columnToInsert ->
            val columnToInsertInto = actualTable.schema.find { it.name == columnToInsert.name } ?: throw IllegalArgumentException("Column '${columnToInsert.name}' does not exist in table '${table.fqdn}'.")
            require(columnToInsertInto.type in setOf(columnToInsert.type, columnToInsert.type.makeNullable())) { "Column '${columnToInsert.name}' is of type '${columnToInsert.type}' but expected '${columnToInsertInto.type}'." }
        }
        val rows = rows(data)
            .map { row ->
                DataRow(actualTable.schema.map { column ->
                    column.name to if (column.name in row.fieldNames) row[column.name] else null
                })
            }
        state.appendToTable(actualTable, rows)
        return rows.size.toLong()
    }
}
