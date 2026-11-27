package rocks.frieler.kraftsql.h2.engine

import rocks.frieler.kraftsql.data.Column
import rocks.frieler.kraftsql.data.Data
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.Table
import rocks.frieler.kraftsql.data.Type
import rocks.frieler.kraftsql.engine.DataClassSerDe
import rocks.frieler.kraftsql.engine.Engine
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLNonTransientException
import kotlin.reflect.KClass

/**
 * [Engine] implementation for the [h2 database](https://h2database.com/html/main.html) for experimentation and demo
 * purposes.
 *
 * KraftSQL's [Type]s are mapped to H2's native types as defined by [Type.native][native].
 *
 * @constructor Creates an [H2Engine] that executes all queries on the given [Connection].
 * @param connection the JDBC [Connection] to the H2 database to execute queries on; defaults to an in-memory database
 * configured by the [H2EngineConnectionSettings] service
 * @param sqlGenerator the [H2SqlGenerator] to generate SQL; only extracted and configurable for testing
 */
class H2Engine(
    private val connection: Connection = makeConnectionFromSettings(),
    private val sqlGenerator: H2SqlGenerator = H2SqlGenerator,
) : Engine {

    /**
     * Collects [Data] by querying it from H2.
     *
     * @param data the [Data] to collect
     * @param type the Kotlin type of the data
     * @return the data as a list of Kotlin objects
     */
    override fun <T : Any> collect(data: Data<T>, type: KClass<T>): List<T> {
        try {
            val rows = connection.createStatement().use { statement ->
                @Suppress("SqlSourceToSinkFlow")
                statement.executeQuery(sqlGenerator.makeSQL(data)).use { resultSet ->
                    generateSequence { if (resultSet.next()) DataRow(data.schema.map { column -> column.name to resultSet.value(column) }) else null }
                        .toList()
                }
            }

            return rows.map { @Suppress("UNCHECKED_CAST") if (type == DataRow::class) it as T else DataClassSerDe.deserialize(it, type) }
        } catch (e: SQLNonTransientException) {
            throw IllegalStateException("Failed to collect data.", e)
        }
    }

    /**
     * Creates the [Table] in H2 by a `CREATE TABLE` statement.
     *
     * @param table the [Table] to create
     * @throws java.sql.SQLException if the table already exists or the database does not exist
     */
    override fun createTable(table: Table<*>) {
        connection.createStatement().use { statement ->
            statement.execute(sqlGenerator.makeCreateTableStatement(table))
        }
    }

    /**
     * Drops the [Table] from H2 by a `DROP TABLE` statement.
     *
     * @param table the [Table] to drop
     * @param ifExists whether to silently do nothing if the table does not exist
     * @throws java.sql.SQLException if the table does not exist and [ifExists] is not set
     */
    override fun dropTable(table: Table<*>, ifExists: Boolean) {
        connection.createStatement().use { statement ->
            statement.execute(sqlGenerator.makeDropTableStatement(table, ifExists))
        }
    }

    /**
     * Inserts the [Data] into the [Table] in H2 by an `INSERT INTO ...` statement.
     *
     * @param table the [Table] to insert into
     * @param data the [Data] to insert
     * @return the number of inserted rows
     * @throws IllegalStateException if the table does not exist or does not fit the values
     */
    override fun <T : Any> insertInto(table: Table<T>, data: Data<T>): Long {
        return connection.createStatement().use { statement ->
            statement.executeUpdate(sqlGenerator.makeInsertIntoStatement(table, data))
        }.toLong()
    }

    private fun ResultSet.value(column: Column) = when (column.type) {
        Type.INTEGER, Type.NULLABLE_INTEGER -> getObject(column.name, Int::class.javaObjectType)
        Type.STRING, Type.NULLABLE_STRING -> getString(column.name)
        Type.BOOLEAN, Type.NULLABLE_BOOLEAN -> getObject(column.name, Boolean::class.javaObjectType)
        else -> throw NotImplementedError("Collecting ${column.type} is not implemented.")
    }
}
