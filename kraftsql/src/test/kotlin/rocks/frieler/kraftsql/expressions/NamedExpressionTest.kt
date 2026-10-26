package rocks.frieler.kraftsql.expressions

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import rocks.frieler.kraftsql.data.named

class NamedExpressionTest {
    @Test
    fun `named() creates a NamedExpression of the Expression with the given name`() {
        val expression = mockk<Expression<*>>()
        val name = "name"

        val namedExpression = expression named name

        namedExpression.name shouldBe name
        namedExpression.expression shouldBe expression
    }

    @Test
    fun `NamedExpression's toString() returns its Expression and name joined by AS`() {
        val expression = mockk<Expression<*>> {
            every { this@mockk.toString() } returns "expression"
        }
        val name = "name"

        val namedExpression = expression named name

        namedExpression.toString() shouldBe "$expression AS `$name`"
    }
}
