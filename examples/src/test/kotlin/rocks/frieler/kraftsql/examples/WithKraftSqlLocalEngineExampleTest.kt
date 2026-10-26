package rocks.frieler.kraftsql.examples

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.ConstantData
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.select
import rocks.frieler.kraftsql.engine.collect
import rocks.frieler.kraftsql.expressions.get
import rocks.frieler.kraftsql.testing.WithKraftSqlLocalEngine

@WithKraftSqlLocalEngine
class WithKraftSqlLocalEngineExampleTest {
    data class Thing(val id: Int, val value: String)

    @Test
    fun `select a column from ConstantData`() {
        val things = ConstantData(Thing(1, "foo"), Thing(2, "bar"))

        val values = things.select(things[Thing::value]).collect<DataRow>()

        values shouldContainExactlyInAnyOrder listOf(DataRow("value" to "foo"), DataRow("value" to "bar"))
    }
}
