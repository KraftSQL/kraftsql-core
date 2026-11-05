package rocks.frieler.kraftsql.data

import rocks.frieler.kraftsql.engine.DataClassSerDe

/**
 * A table to hold data in an SQL engine, usually persistent.
 *
 * A [Table] object is only a reference to the table in the SQL engine; constructing it neither checks nor changes
 * anything there.
 *
 * It can be used like any other [Data] to read its content or use it in a query. To do so, the [schema] does not have
 * to be complete. **Kraft**SQL will read the specified columns and ignore the rest. Also, the types in the schema don't
 * need to fully match, as long as the actual values are compatible at runtime.
 *
 * Moreover, a [Table] object is used for actions on that table, e.g. creating or dropping it, or writing data.
 *
 * Usually, a [Table] of data-class instances is created using the [Table] function, which derives the [schema] from
 * the data class.
 *
 * Connectors may offer subclasses with additional, engine-specific coordinates.
 *
 * @param T the Kotlin type of the [Table]'s rows, either a data class or generically [DataRow]
 * @param database the name of the database containing the [namespace], or `null` for the default database of the
 * [Engine][rocks.frieler.kraftsql.engine.Engine]
 * @param namespace the name of the namespace containing the table, i.e. what SQL calls a schema
 * @param name the name of the table
 * @param schema the (possibly incomplete) [Column] definitions of the table
 */
/* Design decision: There are no separate "table references" with partial schema and "table definitions" with full
 * schema. A single [Table] class serves both purposes, because KraftSQL cannot verify the completeness of a schema
 * without the Engine anyway, and the user usually knows which columns are of interest. */
/* Design decision: The actual schema of a table in the SQL engine is not checked eagerly, which would cost an extra
 * metadata lookup per Table. */
open class Table<T : Any>(
    val database: String? = null,
    val namespace: String,
    val name: String,
    override val schema: List<Column>,
) : Data<T> {
    constructor(namespace: String, name: String, schema: List<Column>) : this(null, namespace, name, schema)

    init {
        require(schema.size == schema.distinctBy { it.name }.size) { "Schema $schema must not contain duplicate column names." }
    }

    override fun toString() = "${database ?: "<default db>"}.$namespace.$name"
}

/**
 * Creates a [Table] of data-class instances.
 *
 * The [schema][Table.schema] of the [Table] is derived from [T] by [DataClassSerDe.getSchemaFor], so the same
 * requirements apply.
 *
 * @param T the data class of the [Table]'s rows
 * @param database the name of the database containing the [namespace], or `null` for the default database of the
 * [Engine][rocks.frieler.kraftsql.engine.Engine]
 * @param namespace the name of the namespace containing the table, i.e. what SQL calls a schema
 * @param name the name of the table
 * @return a [Table] with the [schema][Table.schema] of the data class [T]
 */
inline fun <reified T : Any> Table(database: String? = null, namespace: String, name: String): Table<T> =
    Table(database, namespace, name, DataClassSerDe.getSchemaFor(T::class))

/**
 * Creates a [Table] of data-class instances in the default database of the
 * [Engine][rocks.frieler.kraftsql.engine.Engine].
 *
 * The [schema][Table.schema] of the [Table] is derived from [T] by [DataClassSerDe.getSchemaFor], so the same
 * requirements apply.
 *
 * @param T the data class of the [Table]'s rows
 * @param namespace the name of the namespace containing the table, i.e. what SQL calls a schema
 * @param name the name of the table
 * @return a [Table] with the [schema][Table.schema] of the data class [T]
 */
inline fun <reified T : Any> Table(namespace: String, name: String): Table<T> =
    Table(null, namespace, name, DataClassSerDe.getSchemaFor(T::class))
