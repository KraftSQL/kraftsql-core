package rocks.frieler.kraftsql.data

import rocks.frieler.kraftsql.expressions.Expression
import rocks.frieler.kraftsql.expressions.Column as ColumnExpression

/**
 * [Data] consisting of certain selected columns of other [Data].
 *
 * Each selected [NamedExpression] becomes a column of the [Selection], with its name and the [Type] and values of its
 * [Expression][NamedExpression.expression]. A [Selection] has the same rows as its
 * [source], but not necessarily in the same order.
 *
 * Usually, a [Selection] is created using [Data.select()][select].
 *
 * Column names are case-sensitive, so columns whose names differ only in case are distinct columns. A selection of
 * such columns may make the [Engine][rocks.frieler.kraftsql.engine.Engine] fail with a [RuntimeException], if its SQL
 * engine treats column names case-insensitively, e.g. BigQuery.
 *
 * @param source the [Data] to select the columns from
 * @param expressions the [NamedExpression]s to select as columns, evaluated against the [source]
 * @throws IllegalArgumentException if [expressions] is empty, if two of them have the same name, or if an [Expression]
 * references a [Column][rocks.frieler.kraftsql.expressions.Column] that is not in the [schema][Data.schema] of the
 * [source] with the same name and [Type]
 */
class Selection(
    val source: Data<*>,
    val expressions: List<NamedExpression<*>>,
) : Data<DataRow> {
    init {
        require(expressions.isNotEmpty()) { "Selection needs at least one expression." }
        require(expressions.map { it.name }.toSet().size == expressions.size) { "Selection must not define a column more than once: $expressions." }
        expressions.forEach {
            // TODO: Check the nested Columns referenced by other kinds of Expressions, once there are any.
            (it.expression as? ColumnExpression<*>)?.apply {
                require(source.schema.any { column -> column.name == name }) { "No column '$it.name' in source; did you mean one of ${source.schema.map { column -> column.name }}?" }
                val referencedColumnType = source.schema.first { column -> column.name == name }.type
                require(type == referencedColumnType) { "Column '${name}' in source is of type '$referencedColumnType', which does not match '${type}'." }
            }
        }
    }

    /**
     * The schema of the resulting [Data], i.e. [Column] definitions for the selected [expressions], in the order of the
     * selection.
     */
    override val schema: List<Column> = expressions.map { Column(it.name, it.expression.type) }
}

/**
 * Makes a [Selection] of the given [columns] from this [Data].
 *
 * @param columns the [NamedExpression]s to select as columns
 * @return a [Selection] of the columns, with generic [DataRow]s as rows
 * @throws IllegalArgumentException if no columns are given, if two columns have the same name, or if an [Expression]
 * references a [Column][rocks.frieler.kraftsql.expressions.Column] that is not in the [schema][Data.schema] of this
 * [Data] with the same name and [Type]
 * @see Selection
 */
fun Data<*>.select(columns: List<NamedExpression<*>>): Data<DataRow> = Selection(this, columns)

/**
 * Makes a [Selection] of the given [columns] from this [Data].
 *
 * Same as [select] with a [List] of [NamedExpression]s.
 *
 * @param columns the [NamedExpression]s to select as columns
 * @return a [Selection] of the columns, with generic [DataRow]s as rows
 * @throws IllegalArgumentException if no columns are given, if two columns have the same name, or if an [Expression]
 * references a [Column][rocks.frieler.kraftsql.expressions.Column] that is not in the [schema][Data.schema] of this
 * [Data] with the same name and [Type]
 */
fun Data<*>.select(vararg columns: NamedExpression<*>): Data<DataRow> = select(columns.toList())
