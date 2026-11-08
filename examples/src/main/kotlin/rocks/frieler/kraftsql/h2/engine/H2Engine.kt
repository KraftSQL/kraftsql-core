package rocks.frieler.kraftsql.h2.engine

import rocks.frieler.kraftsql.data.Column
import rocks.frieler.kraftsql.data.Data
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.Type
import rocks.frieler.kraftsql.engine.DataClassSerDe
import rocks.frieler.kraftsql.engine.Engine
import java.sql.Connection
import java.sql.ResultSet
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
        val rows = connection.createStatement().use { statement ->
            @Suppress("SqlSourceToSinkFlow")
            statement.executeQuery(sqlGenerator.makeSQL(data)).use { resultSet ->
                generateSequence { if (resultSet.next()) DataRow(data.schema.map { column -> column.name to resultSet.value(column) }) else null }
                    .toList()
            }
        }

        return rows.map { @Suppress("UNCHECKED_CAST") if (type == DataRow::class) it as T else DataClassSerDe.deserialize(it, type) }
    }

    private fun ResultSet.value(column: Column) = when (column.type) {
        Type.INTEGER, Type.NULLABLE_INTEGER -> getObject(column.name, Int::class.javaObjectType)
        Type.STRING, Type.NULLABLE_STRING -> getString(column.name)
        else -> throw NotImplementedError("Collecting ${column.type} is not implemented.")
    }
}
