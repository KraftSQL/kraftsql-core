package rocks.frieler.kraftsql.referencetest.engine

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.ConstantData
import rocks.frieler.kraftsql.data.DataRow
import rocks.frieler.kraftsql.data.named
import rocks.frieler.kraftsql.data.select
import rocks.frieler.kraftsql.engine.collect
import rocks.frieler.kraftsql.expressions.Constant

class ConstantExpressionTest {
    @Test
    fun `Engine can evaluate Constant Expression`() {
        val result = ConstantData(DataRow("id" to 1))
            .select(Constant(42) named "answer")
            .collect()

        result.single()["answer"] shouldBe 42
    }

    @Test
    fun `Engine can select Constants, once for each row of the source`() {
        val result = ConstantData(
            DataRow("id" to 1),
            DataRow("id" to 2),
        )
            .select(Constant(42) named "constant")
            .collect()

        result shouldHaveSize 2
        result.forEach { row -> row["constant"] shouldBe 42 }
    }
}
