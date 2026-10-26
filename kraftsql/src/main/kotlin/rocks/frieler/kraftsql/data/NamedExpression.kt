package rocks.frieler.kraftsql.data

import rocks.frieler.kraftsql.expressions.Expression

/**
 * An [rocks.frieler.kraftsql.expressions.Expression] with a name, e.g. to define a column in a query.
 *
 * Names are not validated: a name that is not supported by the SQL engine, e.g. an empty one or one with special
 * characters, makes the [Engine][rocks.frieler.kraftsql.engine.Engine] fail with a [RuntimeException].
 *
 * @param T the Kotlin type of the [expression]'s value
 */
/* Design decision: A [NamedExpression] itself is no [Expression], so it can be used only where a name is needed, not
 * nested in other [Expression]s, and cannot be named again. */
// TODO: Validate names, e.g. reject empty names and characters that are not supported by every SQL engine.
interface NamedExpression<out T> {
    /**
     * The name of this [NamedExpression], e.g. the name of the column when selected.
     */
    val name: String

    /**
     * The named [Expression].
     */
    val expression: Expression<T>
}

private data class SimpleNamedExpression<T>(
    override val name: String,
    override val expression: Expression<T>,
) : NamedExpression<T> {
    override fun toString() = "$expression AS `$name`"
}

/**
 * Names this [Expression].
 *
 * @param T the Kotlin type of the [Expression]'s value
 * @param name the name to give this [Expression]
 * @return a [NamedExpression] of this [Expression] with the given name
 */
infix fun <T> Expression<T>.named(name: String): NamedExpression<T> = SimpleNamedExpression(name, this)
