package rocks.frieler.kraftsql.local.engine

import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.Table

/**
 * Internal state of the data (in the form of tables) of an in-memory [rocks.frieler.kraftsql.engine.Engine].
 *
 * Tables are identified by their [fully qualified name][fqdn].
 */
class EngineState {
    private val tables = HashMap<String, Pair<Table<*>, MutableList<DataRow>>>()

    /**
     * Looks up a table by its fully qualified name.
     *
     * @param fqdn the [fully qualified name][Table.fqdn] of the table
     * @return the [Table] with that name, or `null` if there is none
     */
    fun findTable(fqdn: String): Table<*>? = tables[fqdn]?.first

    /**
     * Reads the content of the given table, looked up by its [fqdn] as [DataRow]s.
     *
     * @param table the table to read
     * @return the [DataRow]s stored in the table
     */
    fun readTable(table: Table<*>): List<DataRow> = tables[table.fqdn]!!.second

    /**
     * Adds a table, replacing any existing table with the same [fully qualified name][Table.fqdn].
     *
     * @param table the [Table] to add
     */
    fun putTable(table: Table<*>) {
        tables[table.fqdn] = table to mutableListOf()
    }

    /**
     * Appends the given [rows]s to the [table], looked up by its [fqdn].
     *
     * @param table to [Table] to write into
     * @param rows the [DataRow]s to write into the [table]
     */
    fun appendToTable(table: Table<*>, rows: List<DataRow>) {
        rows.forEach { row ->
            row.entries.forEach { entry ->
                val targetColumn = table.schema.first { column -> column.name == entry.first }
                require(targetColumn.type.accepts(entry.second)) { "Column '${targetColumn.name}' of type ${targetColumn.type} cannot hold '${entry.second}'." }
            }
        }
        tables[table.fqdn]!!.second.addAll(rows)
    }

    /**
     * Removes the table with the same [fully qualified name][Table.fqdn], if any.
     *
     * @param table the [Table] to remove
     */
    fun removeTable(table: Table<*>) {
        tables.remove(table.fqdn)
    }
}
