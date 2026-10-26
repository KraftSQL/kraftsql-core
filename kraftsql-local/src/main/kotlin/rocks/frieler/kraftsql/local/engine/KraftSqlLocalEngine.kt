package rocks.frieler.kraftsql.local.engine

import rocks.frieler.kraftsql.data.ConstantData
import rocks.frieler.kraftsql.data.Data
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.Selection
import rocks.frieler.kraftsql.engine.DataClassSerDe
import rocks.frieler.kraftsql.engine.Engine
import rocks.frieler.kraftsql.expressions.Column
import rocks.frieler.kraftsql.expressions.Constant
import rocks.frieler.kraftsql.expressions.Expression
import kotlin.reflect.KClass

/**
 * [Engine] implementation that works locally in the JVM.
 *
 * The [KraftSqlLocalEngine] is primarily meant as a dummy [Engine] for (unit) testing business logic, when the real
 * [Engine] is not available or too expensive to use. It is not optimized and not intended to handle "Big Data".
 */
class KraftSqlLocalEngine : Engine {
    override fun <T : Any> collect(data: Data<T>, type: KClass<T>): List<T> {
        return rows(data)
            .map { @Suppress("UNCHECKED_CAST") if (type != DataRow::class) DataClassSerDe.deserialize(it, type) else it as T }
    }

    private fun rows(data: Data<*>): List<DataRow> {
        return when (data) {
            is ConstantData<*> -> data.items
                .map { if (it !is DataRow) DataClassSerDe.serialize(it) else it }
            is Selection -> rows(data.source)
                .map { row -> DataRow(data.expressions.map { column -> column.name to evaluateExpression(column.expression, row) }) }
            else -> throw NotImplementedError("Collecting $data is not implemented.")
        }
    }

    private fun evaluateExpression(expression: Expression<*>, row: DataRow) : Any? = when (expression) {
        is Constant<*> -> expression.value
        is Column<*> -> row[expression.name]
        else -> throw NotImplementedError("Evaluating $expression is not supported.")
    }
}
