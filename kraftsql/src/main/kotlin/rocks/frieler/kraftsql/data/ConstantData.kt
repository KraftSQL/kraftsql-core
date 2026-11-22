package rocks.frieler.kraftsql.data

import rocks.frieler.kraftsql.data.Type.Companion.makeNullable
import rocks.frieler.kraftsql.engine.DataClassSerDe

/**
 * Constant [Data].
 *
 * @param T the Kotlin type of the [ConstantData]'s items, either a data class or generically [DataRow]
 */
// TODO: Support empty Data.
open class ConstantData<T : Any> protected constructor(
    /**
     * The items - or rows - in this data.
     */
    val items: Iterable<T>,
    override val schema: List<Column>,
) : Data<T> {

    /**
     * Creates a new [ConstantData] with the given items.
     *
     * The schema will be inferred from the items. Hence, the items must not be empty and all be of the same type,
     * either a data-class or [DataRow]. In case of [DataRow]s they must all have the same fields with compatible
     * values each.
     *
     * @param items the rows
     */
    constructor(items: Iterable<T>) : this(
        items.apply {
            val iterator = items.iterator()
            require(iterator.hasNext()) { "ConstantData needs at least one item (or you must specify schema information)." }
            val referenceItem = iterator.next()
            iterator.forEachRemaining { item ->
                require(item::class == referenceItem::class) { "All items must be of the same type." }
                if (referenceItem is DataRow && item is DataRow) {
                    require(item.fieldNames == referenceItem.fieldNames) { "All DataRow items must have the same columns." }
                }
            }
        },
        inferSchema(items)
    )

    constructor(vararg items: T) : this(items.toList())

    companion object {
        private fun <T :Any>inferSchema(items: Iterable<T>) : List<Column> {
            check(items.iterator().hasNext()) { "Cannot infer schema from zero items." }
            return items.first().let { firstItem ->

                if (firstItem is DataRow) {
                    @Suppress("UNCHECKED_CAST") val rows = items as Iterable<DataRow> // because all items are checked to be of the same type
                    inferSchemaFromDataRows(firstItem, rows)
                }

                else if (firstItem::class.isData) DataClassSerDe.getSchemaFor(firstItem::class)

                else TODO()
            }
        }

        private fun inferSchemaFromDataRows(firstItem: DataRow, rows: Iterable<DataRow>): List<Column> {
            data class ColumnTypeInformation(var type: Type? = null, var nullable: Boolean = false)

            val schemaInformation = firstItem.fieldNames.associateWith { ColumnTypeInformation() }
            rows.forEach { row ->
                schemaInformation.forEach { (column, typeInfo) ->
                    val value = row[column]
                    if (value != null) {
                        // FIXME: Reject values of different types.
                        typeInfo.type = typeInfo.type ?: Type.get(value::class, typeInfo.nullable)
                    } else {
                        typeInfo.nullable = true
                        typeInfo.type = typeInfo.type?.makeNullable()
                    }
                }
            }
            return schemaInformation.map { (name, type) -> Column(name, type.type ?: TODO()) } // FIXME: Handle null values only.
        }
    }
}
