package rocks.frieler.kraftsql.expressions

import rocks.frieler.kraftsql.data.Type
import kotlin.reflect.typeOf

/**
 * An [Expression] that evaluates to the same constant value for each row, i.e. an SQL literal.
 *
 * Usually, a [Constant] is created by the [Constant] function, which determines its [type] from the Kotlin type [T].
 *
 * @param T the Kotlin type of the [Constant]'s value
 * @param value the constant value, `null` for SQL's `NULL`
 * @param type the [Type] of the [value]
 * @throws IllegalArgumentException if the [value] does not comply with the [type], e.g. `null` for a non-nullable
 * [Type]
 */
class Constant<T> @PublishedApi internal constructor(
    val value: T,
    override val type: Type,
) : Expression<T> {
    init {
        require(type.accepts(value)) { "$value is not of type $type." }
    }

    /**
     * Returns the String value of the [value], e.g. `42`, `foo` or `null`.
     */
    override fun toString(): String = value.toString()

    override fun equals(other: Any?): Boolean = other is Constant<*>
        && other.value == value
        && other.type == type

    override fun hashCode(): Int = value.hashCode()
}

/**
 * Creates a [Constant] of the given value.
 *
 * The [Constant]'s [Type] is determined from the Kotlin type [T]. Specify it explicitly for `null` and nullable types.
 *
 * @param T the Kotlin type of the constant value, one of the supported [Type]s
 * @param value the constant value, `null` for SQL's `NULL`
 * @return a [Constant] of the given value with the [Type] of [T]
 * @throws IllegalStateException if [T] is not a supported [Type]
 */
inline fun <reified T> Constant(value: T): Constant<T> {
    if (value == null) require(typeOf<T>() != typeOf<Nothing?>()) {
        "Got type 'Nothing?' for NULL Constant, which is not supported. Most likely because no type was specified."
    }
    return Constant(value, Type.get(typeOf<T>()))
}
