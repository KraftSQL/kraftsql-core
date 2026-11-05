package rocks.frieler.kraftsql.engine

import rocks.frieler.kraftsql.data.Data
import rocks.frieler.kraftsql.data.Table
import java.util.ServiceLoader
import kotlin.reflect.KClass

/**
 * An engine (usually a SQL engine) to work with data inside it.
 */
interface Engine {
    /**
     * Collects [Data] as Kotlin objects of the specified [KClass], either [rocks.frieler.kraftsql.data.DataRow] or a
     * data class.
     *
     * @param data the [Data] to collect
     * @param type the Kotlin type of the data
     * @return the data as a list of Kotlin objects
     */
    fun <T : Any> collect(data: Data<T>, type: KClass<T>): List<T>

    /**
     * Creates a [Table] in the SQL engine, i.e. `CREATE TABLE`.
     *
     * @param table the [Table] to create
     * @throws RuntimeException if a table with the same coordinates already exists, or if the
     * [database][Table.database] or [namespace][Table.namespace] does not exist or cannot be used
     */
    fun createTable(table: Table<*>)

    /**
     * Drops a [Table] from the SQL engine, i.e. `DROP TABLE`, deleting all its rows.
     *
     * The table is identified by the [Table]'s coordinates only; its [schema][Table.schema] is irrelevant.
     *
     * @param table the [Table] to drop
     * @param ifExists whether to silently do nothing if the table does not exist, i.e. `DROP TABLE IF EXISTS`
     * @throws IllegalStateException if the table does not exist and [ifExists] is not set
     */
    fun dropTable(table: Table<*>, ifExists: Boolean = false)

    /**
     * Inserts the rows of [Data] into a [Table], i.e. `INSERT INTO`.
     *
     * The columns of the [data] are matched to the columns of the [table] by name, not by position. Each column of the
     * [data] must be in the [schema][Table.schema] of the [table] and have the same
     * [Type][rocks.frieler.kraftsql.data.Type] its nullable variant. Columns of the [table] missing in the [data] are
     * filled with `NULL`, hence they must be nullable.
     *
     * In case of a failure, no row is inserted.
     *
     * @param T the Kotlin type of the rows
     * @param table the [Table] to insert into
     * @param data the [Data] to insert
     * @return the number of inserted rows, which may be zero
     */
    /* Design decision: The Kotlin row types of the Data and the Table must match, to prevent obvious mistakes at
     * compile-time. Generic DataRows do not ensure a schema, though. */
    fun <T : Any> insertInto(table: Table<T>, data: Data<T>): Long

    companion object {
        /**
         * A default [Engine] loaded by the [ServiceLoader].
         *
         * Works only, if there is exactly one service implementation registered for the [Engine] interface.
         */
        val default: Engine
            get() = _default ?: loadDefaultEngine()

        private var _default: Engine? = null

        private fun loadDefaultEngine(): Engine {
            val candidates = ServiceLoader.load(Engine::class.java).toList()
            check(candidates.isNotEmpty()) { "No Engine implementation registered for ServiceLoader." }
            check(candidates.size < 2) { "Multiple Engine implementations registered for ServiceLoader: $candidates." }
            return candidates.single().also { _default = it }
        }

        /**
         * Resets the cached [default] [Engine], so it is loaded anew via the [ServiceLoader] on next access.
         */
        fun resetDefault() {
            _default = null
        }

        /**
         * Sets the [default] [Engine] explicitly to bypass the [ServiceLoader].
         *
         * Use this when there is more than one [Engine] registered, but you still want to define one as default.
         *
         * @param engine the desired [default] [Engine]
         */
        fun setDefault(engine: Engine) {
            _default = engine
        }
    }
}

/**
 * Collects the [Data] as Kotlin objects of the reified type, either [rocks.frieler.kraftsql.data.DataRow] or a data
 * class, using the [default Engine][Engine.default].
 *
 * @return the data as a list of Kotlin objects
 */
inline fun <reified T : Any> Data<T>.collect() = Engine.default.collect(this, T::class)

/**
 * Creates this [Table] using the [default Engine][Engine.default].
 *
 * @see Engine.createTable
 * @throws RuntimeException if a table with the same coordinates already exists, or if the
 * [database][Table.database] or [namespace][Table.namespace] does not exist or cannot be used
 */
fun Table<*>.create(): Unit = Engine.default.createTable(this)

/**
 * Drops this [Table] using the [default Engine][Engine.default].
 *
 * @param ifExists whether to silently do nothing if the table does not exist
 * @see Engine.dropTable
 * @throws RuntimeException if the table does not exist and [ifExists] is not set
 */
fun Table<*>.drop(ifExists: Boolean = false): Unit = Engine.default.dropTable(this, ifExists)

/**
 * Inserts the rows of this [Data] into the given [Table] using the [default Engine][Engine.default].
 *
 * @param T the Kotlin type of the rows
 * @param table the [Table] to insert into
 * @return the number of inserted rows
 * @see Engine.insertInto
 */
fun <T : Any> Data<T>.insertInto(table: Table<T>): Long = Engine.default.insertInto(table, this)
