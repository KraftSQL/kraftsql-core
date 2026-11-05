package rocks.frieler.kraftsql.local.engine

import rocks.frieler.kraftsql.data.Table

/**
 * The fully qualified name of this [Table], i.e.
 * [[database][Table.database].][namespace][Table.namespace].[name][Table.name].
 */
val Table<*>.fqdn : String
    get() = listOfNotNull(database, namespace, name).joinToString(".")
