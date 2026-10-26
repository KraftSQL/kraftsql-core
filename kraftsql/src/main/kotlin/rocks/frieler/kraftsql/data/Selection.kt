package rocks.frieler.kraftsql.data

import rocks.frieler.kraftsql.expressions.Expression
import kotlin.reflect.typeOf
import rocks.frieler.kraftsql.expressions.Column as ColumnExpression

/**
 * [Data] consisting of certain selected columns of other [Data].
 *
 * Each selected [Expression] becomes a column of the [Selection]. A [Selection] has the same rows as its [source], but
 * not necessarily in the same order.
 *
 * Usually, a [Selection] is created by [select].
 *
 * Column names are case-sensitive, so columns whose names differ only in case are distinct columns. A selection of
 * such columns may make the [Engine][rocks.frieler.kraftsql.engine.Engine] fail with a [RuntimeException], if its SQL
 * engine treats column names case-insensitively, e.g. BigQuery.
 *
 * @param source the [Data] to select the columns from
 * @param expressions the [Expression]s to select as columns, evaluated against the [source]
 * @throws IllegalArgumentException if [expressions] is empty, if it contains a column more than once, or if an
 * [Expression] references a [Column][rocks.frieler.kraftsql.expressions.Column] that is not in the [schema][Data.schema]
 * of the [source] with the same name and [Type]
 */
class Selection(
    val source: Data<*>,
    val expressions: List<Expression<*>>,
) : Data<DataRow> {
    init {
        require(expressions.isNotEmpty()) { "Selection needs at least one expression." }
        require(expressions.toSet().size == expressions.size) { "Selection must not contain an expression more than once: $expressions." }
        expressions.forEach {
            // TODO: Check the Columns referenced by other kinds of Expressions, once there are any.
            if (it !is ColumnExpression<*>) throw NotImplementedError("Selecting $it is not implemented.")
            require(source.schema.any { column -> column.name == it.name }) { "No column '$it.name' in source; did you mean one of ${source.schema.map { column -> column.name }}?" }
            val referencedColumnType = source.schema.first { column -> column.name == it.name }.type
            require(it.type == referencedColumnType) { "Column '${it.name}' in source is of type '$referencedColumnType', which does not match '${it.type}'." }
        }
    }

    /**
     * The schema of the resulting [Data], i.e. [Column] definitions for the selected [expressions], in the order of the
     * selection.
     */
    override val schema: List<Column> = expressions.map { Column((it as ColumnExpression<*>).name, it.type) }
}

/**
 * Selects certain columns of this [Data].
 *
 * The resulting [Data] has exactly the selected columns, in the order of the selection. A selected
 * [Column][rocks.frieler.kraftsql.expressions.Column] keeps its [Type] and values from this [Data]. The result has the
 * same rows as this [Data], but not necessarily in the same order; rows that become equal by the selection are kept,
 * not deduplicated.
 *
 * A [Column][rocks.frieler.kraftsql.expressions.Column] is selected by its name and [Type], so it may also be retrieved
 * from other [Data] with the same column, e.g. from the source of this [Data] if this [Data] keeps the column.
 *
 * @param columns the [Expression]s to select as columns, e.g. [Column][rocks.frieler.kraftsql.expressions.Column]s of
 * this [Data]
 * @return a [Selection] of the columns, with generic [DataRow]s as rows
 * @throws IllegalArgumentException if no columns are given, if a column is selected more than once, or if an
 * [Expression] references a [Column][rocks.frieler.kraftsql.expressions.Column] that is not in the
 * [schema][Data.schema] of this [Data] with the same name and [Type]
 * @see Selection
 */
fun Data<*>.select(columns: List<Expression<*>>): Data<DataRow> = Selection(this, columns)

/**
 * Selects certain columns of this [Data].
 *
 * Same as [select] with a [List] of [Expression]s.
 *
 * @param columns the [Expression]s to select as columns, e.g. [Column][rocks.frieler.kraftsql.expressions.Column]s of
 * this [Data]
 * @return a [Selection] of the columns, with generic [DataRow]s as rows
 * @throws IllegalArgumentException if no columns are given, if a column is selected more than once, or if an
 * [Expression] references a [Column][rocks.frieler.kraftsql.expressions.Column] that is not in the
 * [schema][Data.schema] of this [Data] with the same name and [Type]
 */
fun Data<*>.select(vararg columns: Expression<*>): Data<DataRow> = select(columns.toList())
