package rocks.frieler.kraftsql.expressions

import rocks.frieler.kraftsql.data.Data
import rocks.frieler.kraftsql.data.NamedExpression
import rocks.frieler.kraftsql.data.Type
import java.util.Objects
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf

/**
 * An [Expression] that references a column by its name.
 *
 * Usually, a [Column] is retrieved from [Data] by the [get] operators, which check its [type] against the
 * [schema][Data.schema]. Otherwise, a [Column] is created by the [Column] function, which determines its [type] from
 * the Kotlin type [T].
 *
 * A [Column] is also a [rocks.frieler.kraftsql.data.NamedExpression] of itself, naturally named by its [name] and
 * thereby directly usable in queries. To select it under another name, it can be [rocks.frieler.kraftsql.data.named],
 * e.g. `data["col"] named "my_column"`.
 *
 * @param T the Kotlin type of the column's values
 * @param name the name of the column
 * @param type the [Type] of the column, expected to comply with `T`
 */
class Column<T> @PublishedApi internal constructor(
    override val name: String,
    override val type: Type,
) : Expression<T>, NamedExpression<T> {
    /**
     * This [Column] itself.
     */
    override val expression: Column<T> get() = this

    override fun toString() = name

    override fun equals(other: Any?): Boolean = other is Column<*>
            && name == other.name
            && type == other.type

    override fun hashCode(): Int = Objects.hash(name, type)
}

/**
 * Creates a [Column] expression for a column of the given name.
 *
 * The [Column]'s [Type] is determined from the Kotlin type [T], e.g. `Column<String?>("name")` for a column of nullable
 * Strings.
 *
 * @param T the Kotlin type of the column's values, one of the supported [Type]s
 * @param name the name of the column
 * @return a [Column] expression with the given name and the [Type] of [T]
 * @throws IllegalStateException if [T] is not a supported [Type]
 */
inline fun <reified T> Column(name: String): Column<T> = Column(name, Type.get(typeOf<T>()))

/**
 * Retrieves a [Column] expression for the named column of this [Data].
 *
 * The caller specifies the Kotlin type [V] of the column's values, e.g. `data.get<String?>("name")`, which must match
 * the column's [Type] in the [schema][Data.schema].
 *
 * The [Column] expression is not bound to this [Data], but can be used on any [Data] with a column of the same name and
 * [Type].
 *
 * @param V the Kotlin type of the column's values
 * @param name the name of the column
 * @return a [Column] expression with the name and [Type] of the named column
 * @throws IllegalArgumentException if there is no column with the given name in the [schema][Data.schema], or if its
 * [Type] does not match [V]
 */
inline operator fun <reified V> Data<*>.get(name: String): Column<V> {
    require(schema.any { it.name == name }) { "No column '$name' in Data; did you mean one of ${schema.map { it.name }}?" }
    return Column<V>(name).also { column ->
        val referencedColumnType = schema.first { it.name == name }.type
        require(column.type == referencedColumnType) { "Column '$name' of type '$referencedColumnType' does not match '${typeOf<V>()}'." }
    }
}

/**
 * Retrieves a [Column] expression for the column specified by a [property][KProperty1] of the rows' type.
 *
 * The column is identified by the [property][KProperty1]'s name. Its Kotlin type determines the type of the [Column]
 * expression. Like for the column by name, the [Column] expression is not bound to this [Data].
 *
 * @param T the Kotlin type of the [Data]'s rows
 * @param V the Kotlin type of the property and thereby of the column's values
 * @param property the [property][KProperty1] of the rows' type to get the [Column] for
 * @return a [Column] expression with the name and [Type] of the specified [property][KProperty1]
 * @throws IllegalArgumentException if there is no column with the given name in the [schema][Data.schema], or if its
 * [Type] does not match [V]
 */
inline operator fun <T : Any, reified V> Data<T>.get(property: KProperty1<T, V>): Column<V> = get<V>(property.name)
